package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.launch
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.utils.Closeable
import dev.racci.minix.api.utils.kotlin.doesOverride
import dev.racci.minix.api.utils.ticks
import dev.racci.minix.nms.aliases.NMSWorld
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origin
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.core.extensions.inDarkness
import dev.racci.terix.core.extensions.inRain
import dev.racci.terix.core.extensions.inSunlight
import dev.racci.terix.core.extensions.inWater
import dev.racci.terix.core.extensions.wasInDarkness
import dev.racci.terix.core.extensions.wasInRain
import dev.racci.terix.core.extensions.wasInSunlight
import dev.racci.terix.core.extensions.wasInWater
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import net.minecraft.core.BlockPos
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.Heightmap
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.PlayerInventory
import java.util.UUID
import kotlin.math.roundToInt

@MappedExtension(Terix::class, "Tick Service", [OriginService::class])
class TickService(override val plugin: Terix) : Extension<Terix>() {
    private val delayChannel = Channel<Unit>(0)
    private val originReference = HashMap<Player, AbstractOrigin>()
    private val mutex = Mutex()
    private val playerQueue = ArrayDeque<Player>()
    private val internalFlow = MutableSharedFlow<Player>()
    private val tickerThreads = object : Closeable<ExecutorCoroutineDispatcher>() {
        @OptIn(DelicateCoroutinesApi::class)
        override fun create(): ExecutorCoroutineDispatcher {
            return newFixedThreadPoolContext(4, "Origin Ticker")
        }
    }

    /** The queue of players which have disconnected. (Slightly faster performance than checking [Player.isOnline]) */
    private val removeQueue = HashSet<UUID>()

    val playerFlow = internalFlow.asSharedFlow()

    override suspend fun handleEnable() {
        event<PlayerJoinEvent> {
            delayChannel.trySend(Unit)
            removeQueue.remove(player.uniqueId) // Maybe they reconnected?
            mutex.withLock { playerQueue.addLast(player) }

            val origin = origin(player)
            if (hasTick(origin)) { originReference[player] = origin }
        }
        event<PlayerQuitEvent> {
            originReference.remove(player)
            removeQueue.add(player.uniqueId)
        }
        event<PlayerOriginChangeEvent> {
            val origin = origin(player)
            if (hasTick(origin)) { originReference[player] = origin } else { originReference.remove(player) }
        }

        startTicker()
        startTickListener()
    }

    override suspend fun handleUnload() {
        tickerThreads.close()
    }

    private fun hasTick(origin: AbstractOrigin): Boolean {
        return origin::class.doesOverride(AbstractOrigin::onTick.name)
    }

    private suspend fun startTicker() = launch(tickerThreads.get()) {
        val flow = flow {
            while (loaded) {
                if (onlinePlayers.isEmpty()) delayChannel.receive() // Wait for players to join.

                if (playerQueue.isEmpty()) {
                    delay(10); continue
                } // If empty delay for 10ms and then try again.

                val player = mutex.withLock { playerQueue.removeFirst() }
                emit(player)
            }
        }

        flow.onEach(::runTicker)
            .buffer()
            .collect(internalFlow::emit)
    }

    private fun startTickListener() = launch(tickerThreads.get()) {
        playerFlow
            .onEach { player -> originReference[player]?.onTick(player) }
            .collect { player ->
                async {
                    yield()
                    delay(1.ticks)
                    if (player.uniqueId in removeQueue) return@async
                    mutex.withLock { playerQueue.addLast(player) }
                }
            }
    }

    private fun runTicker(
        player: Player
    ) {
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

    companion object : ExtensionCompanion<TickService>()
}
