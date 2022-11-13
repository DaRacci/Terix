package dev.racci.terix.core.services

import arrow.core.Either
import arrow.core.Predicate
import arrow.core.left
import arrow.core.right
import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.coroutine.scope
import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.data.enums.LiquidType.Companion.liquidType
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extension.ExtensionState
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.extensions.collections.computeAndRemove
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.NMSWorld
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.api.services.TickService
import dev.racci.terix.api.services.TickService.Companion.TICK_RATE
import dev.racci.terix.api.services.TickService.Companion.playerFlow
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.inWater
import dev.racci.terix.core.extensions.wasInDarkness
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.extensions.wasInWater
import dev.racci.terix.core.services.runnables.AmbientTick
import dev.racci.terix.core.services.runnables.ChildTicker
import dev.racci.terix.core.services.runnables.DarknessTick
import dev.racci.terix.core.services.runnables.MotherTicker
import dev.racci.terix.core.services.runnables.RainTick
import dev.racci.terix.core.services.runnables.SunlightTick
import dev.racci.terix.core.services.runnables.WaterTick
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import net.minecraft.core.BlockPos
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.Heightmap
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.PlayerInventory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt

@MappedExtension(Terix::class, "Tick Service", [OriginService::class], TickService::class, 4)
public class TickServiceImpl(override val plugin: Terix) : Extension<Terix>(), TickService {
    // TODO -> Count subscriptions that want the player so we can remove the tick if no one wants it.
    private val tickingPlayers = HashSet(ConcurrentHashMap.newKeySet<UUID>())
    private val delayChannel = Channel<Unit>(0)
    private val arrayQueueMutex = Mutex()
    private val playerQueue = ArrayDeque<Player>() // TODO -> Possibly thread local groups of players to avoid mutex usage.
    private val internalFlow = MutableSharedFlow<Player>()
    private val motherTickers = mutableMapOf<Player, MotherTicker>()
    private var tickables = persistentHashMapOf<Either<State, Predicate<Origin>>, (Player, Origin) -> ChildTicker>()

    /** The queue of players that have disconnected or have no subscribers. */
    private val removeQueue = ConcurrentHashMap.newKeySet<UUID>()

    override val playerFlow: SharedFlow<Player> = internalFlow.asSharedFlow()

    override val threadContext: CoroutineDispatcher get() = this.dispatcher.get()

    override suspend fun handleEnable() {
        // TODO -> Only load these once
        startTicker()
        startInternalTickListeners()

        tickables = persistentHashMapOf(
            State.LightState.SUNLIGHT.left() to ::SunlightTick,
            State.LightState.DARKNESS.left() to ::DarknessTick,
            State.WeatherState.RAIN.left() to ::RainTick,
            State.LiquidState.WATER.left() to ::WaterTick,
            { origin: Origin -> origin.sounds.ambientSound != null }.right() to ::AmbientTick
        )

        onlinePlayers.forEach { player -> rehabMother(player, TerixPlayer.cachedOrigin(player)) }

        event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
            val origin = TerixPlayer.cachedOrigin(player)
            rehabMother(player, origin)
        }

        event<PlayerQuitEvent> {
            tickingPlayers.remove(player.uniqueId)
            removeQueue.add(player.uniqueId)
        }

        event<PlayerPostRespawnEvent> {
            delayChannel.trySend(Unit) // Gets locked if there was only one player online. (Fuck you weird behaviour)
        }

        event<PlayerOriginChangeEvent> {
            val origin = TerixPlayer.cachedOrigin(player)
            rehabMother(player, origin)
        }
    }

    override suspend fun handleDisable() {
        motherTickers.clear { _, mother -> mother.endSuffering() }
    }

    // TODO -> Construct a cache of filtered flows, which can be used to reduce the amount of filtering done.
    override fun filteredPlayer(player: Player): Flow<Player> = runBlocking {
        appendPlayer(player)
        playerFlow.filter { it.uniqueId == player.uniqueId }
    }

    private suspend fun appendPlayer(player: Player) = tickingPlayers.add(player.uniqueId).ifTrue {
        if (!removeQueue.remove(player.uniqueId)) { // Ensures we don't add a duplicate player.
            arrayQueueMutex.withLock { playerQueue.addLast(player) }
        }
        delayChannel.trySend(Unit)
    }

    private suspend fun startTicker() = flow {
        while (state != ExtensionState.DISABLED) {
            if (!loaded || onlinePlayers.isEmpty()) {
                logger.debug { "Awaiting players..." }
                delayChannel.receive()
            } // Wait for players to join or for this to load.

            if (playerQueue.isEmpty()) {
                delay(12); continue
            } // If empty delay for 12ms (Half a tick) and then try again.

            val player = arrayQueueMutex.withLock { playerQueue.removeFirst() }
            emit(player)
        }
    }.onEach(::runTicker)
        .buffer()
        .onEach(internalFlow::emit)
        .onEach { player ->
            yield()
            delay(TICK_RATE.ticks)
            if (removeQueue.remove(player.uniqueId)) return@onEach
            arrayQueueMutex.withLock { playerQueue.addLast(player) }
        }.launchIn(plugin.scope + dispatcher.get())

    private fun startInternalTickListeners() = playerFlow
        .conflate()
        .onEach { player -> TerixPlayer.cachedOrigin(player).onTick(player) }
        .mapNotNull(motherTickers::get)
        .onEach(MotherTicker::run)
        .catch { logger.error(it) { "Error in internal tick listener." } }
        .launchIn(plugin.scope + dispatcher.get())

    private fun runTicker(
        player: Player
    ) {
        if (player.isDead) return // Don't run ticks for dead players.

        val nmsPlayer = player.toNMS()
        val level = nmsPlayer.level
        val pos = BlockPos(nmsPlayer.x.roundToInt(), nmsPlayer.eyeY.roundToInt(), nmsPlayer.z.roundToInt()) // Player.eyePosition
        val brightness = level.getMaxLocalRawBrightness(pos)
        val canSeeSky = level.canSeeSky(pos)

        player.wasInDarkness = player.inDarkness
        player.inDarkness = inDarkness(brightness, player.inventory)

        player.wasInRain = player.inRain
        player.inRain = inRain(canSeeSky, level, pos)

        player.wasInWater = player.inWater
        player.inWater = player.location.block.liquidType == LiquidType.WATER

        player.wasInSunlight = player.inSunlight
        player.inSunlight = inSunlight(player, canSeeSky, level, brightness)
    }

    private fun inDarkness(
        brightness: Int,
        inventory: PlayerInventory
    ) = brightness < 0.5 &&
        inventory.itemInMainHand.type != Material.TORCH &&
        inventory.itemInOffHand.type != Material.TORCH

    private fun inRain(
        canSeeSky: Boolean,
        level: NMSWorld,
        pos: BlockPos
    ) = canSeeSky &&
        level.isRaining &&
        level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, pos).y < pos.y &&
        canRain(level.getBiome(pos).value(), pos)

    private fun canRain(
        biome: Biome,
        pos: BlockPos
    ) = biome.precipitation == Biome.Precipitation.RAIN && biome.warmEnoughToRain(pos)

    private fun inSunlight(
        player: Player,
        canSeeSky: Boolean,
        level: NMSWorld,
        brightness: Int
    ) = canSeeSky &&
        !player.inWater &&
        !player.inRain &&
        brightness > 0.5f &&
        level.isDay

    private suspend fun rehabMother(
        player: Player,
        origin: Origin
    ) {
        motherTickers.computeAndRemove(player, MotherTicker::endSuffering)

        val states = presentStates(origin)
        val children = mutableSetOf<ChildTicker>()
        for ((key, constructor) in tickables) {
            if (!key.fold(
                    { state -> states.contains(state) },
                    { predicate -> predicate(origin) }
                )
            ) continue

            children += constructor(player, origin)
        }

        if (children.isEmpty()) {
            logger.debug { "No tickables for ${player.name}." }
        } else {
            motherTickers[player] = MotherTicker(this, children.toPersistentHashSet())
            appendPlayer(player)
        }
    }

    private fun presentStates(origin: Origin): Sequence<State> = sequence {
        yieldAll(origin.statePotions.keys)
        yieldAll(origin.stateDamageTicks.keys)
        yieldAll(origin.stateTitles.keys)
        yieldAll(origin.stateBlocks.keys)
        yieldAll(origin.attributeModifiers.keys)
    }.distinct()
}
