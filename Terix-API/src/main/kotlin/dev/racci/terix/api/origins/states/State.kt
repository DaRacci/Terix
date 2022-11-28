package dev.racci.terix.api.origins.states

import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.data.enums.LiquidType.Companion.liquidType
import dev.racci.minix.api.events.player.PlayerLiquidEnterEvent
import dev.racci.minix.api.events.player.PlayerLiquidExitEvent
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.events.world.WorldDayEvent
import dev.racci.minix.api.events.world.WorldNightEvent
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.events
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.isNight
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.RecursionUtils
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.extensions.concurrentMultimap
import dev.racci.terix.api.extensions.handle
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State.BiomeState.Companion.getBiomeState
import dev.racci.terix.api.origins.states.State.LiquidState.Companion.convertLiquidToState
import dev.racci.terix.api.sentryScoped
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toPersistentSet
import net.minecraft.core.BlockPos
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.Heightmap
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.World.Environment
import org.bukkit.block.Block
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.reflect.KClass

public sealed class State : WithPlugin<Terix> {
    final override val plugin: Terix by inject()

    public val name: String = this::class.simpleName ?: throw IllegalStateException("Anonymous classes aren't supported.")

    public open val incompatibleStates: Array<out State> = emptyArray()
    public open val providesSideEffects: Array<out SideEffect> = emptyArray()

    public sealed class StatedSource<I : Any> : State(), StateSource<I>

    public sealed class DualState<I : Any> : StatedSource<I>() {
        public abstract val opposite: DualState<I>
    }

    public object CONSTANT : StatedSource<Nothing>() {
        override fun get(player: Player): Boolean = true
        override fun get(input: Nothing): Boolean = true
    }

    public sealed class TimeState : DualState<World>() {
        override fun get(player: Player): Boolean = this[player.world]

        public object DAY : TimeState() {
            override val opposite: DualState<World> = NIGHT
            override val incompatibleStates: Array<out State> = arrayOf(NIGHT)
            override fun get(input: World): Boolean = getTimeState(input) === this
        }

        public object NIGHT : TimeState() {
            override val opposite: DualState<World> = DAY
            override val incompatibleStates: Array<out State> = arrayOf(DAY)
            override fun get(input: World): Boolean = getTimeState(input) === this
        }
    }

    public sealed class WorldState : StatedSource<Environment>() {
        override fun get(player: Player): Boolean = this[player.world.environment]

        public object OVERWORLD : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(NETHER, END)
            override fun get(input: Environment): Boolean = input === Environment.NORMAL
        }

        public object NETHER : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, END, TimeState.DAY, TimeState.NIGHT)
            override fun get(input: Environment): Boolean = input === Environment.NETHER
        }

        public object END : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, NETHER, TimeState.DAY, TimeState.NIGHT)
            override fun get(input: Environment): Boolean = input === Environment.THE_END
        }
    }

    public sealed class LiquidState : StatedSource<Block>() {
        override fun get(player: Player): Boolean = this[player.location.block]

        public object WATER : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(LAVA, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun get(input: Block): Boolean = getLiquidState(input) === this
        }

        public object LAVA : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.BURNING)
            override fun get(input: Block): Boolean = getLiquidState(input) === this
        }

        public object LAND : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAVA)
            override fun get(input: Block): Boolean = getLiquidState(input) === this
        }

        public companion object {
            public fun convertLiquidToState(liquid: LiquidType): LiquidState = when (liquid) {
                LiquidType.WATER -> WATER
                LiquidType.LAVA -> LAVA
                else -> LAND
            }
        }
    }

    public sealed class LightState : DualState<Nothing>() {
        override fun get(input: Nothing): Boolean = throw UnsupportedOperationException("LightState is only takes players supported")

        public object SUNLIGHT : LightState() {
            override val opposite: LightState = DARKNESS
            override val incompatibleStates: Array<out State> = arrayOf(DARKNESS)
            override fun get(player: Player): Boolean = getLightState(player) === this
        }

        public object DARKNESS : LightState() {
            override val opposite: LightState = SUNLIGHT
            override val incompatibleStates: Array<out State> = arrayOf(SUNLIGHT)
            override fun get(player: Player): Boolean = getLightState(player) === this
        }

        public companion object {
            public fun getLightState(player: Player): LightState? {
                val inventory = player.inventory
                if (inventory.itemInMainHand.type != Material.TORCH && inventory.itemInOffHand.type != Material.TORCH && player.location.block.lightLevel < 5) {
                    return DARKNESS
                }

                val nms = player.handle
                val pos = BlockPos(nms.x, nms.eyeY, nms.z)
                if (player.world.toNMS().canSeeSky(pos)) {
                    return SUNLIGHT
                }

                return null
            }
        }
    }

    public sealed class WeatherState : StatedSource<Location>() {
        override fun get(player: Player): Boolean = this[player.location]

        public object RAIN : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(SNOW)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun get(input: Location): Boolean = getWeatherState(input) === this
        }

        public object SNOW : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(RAIN)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun get(input: Location): Boolean = getWeatherState(input) === this
        }

        public companion object {
            public fun getWeatherState(location: Location): WeatherState? {
                if (location.world == null || location.world.environment != Environment.NORMAL) {
                    return null
                }

                val blockPos = location.block.castOrThrow<CraftBlock>().position
                val level = location.world.toNMS()

                if (!location.world.hasStorm() || !level.canSeeSky(blockPos) || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).y > blockPos.y) return null

                val biome = level.getBiome(blockPos).value()
                return when {
                    biome.precipitation == Biome.Precipitation.RAIN && biome.warmEnoughToRain(blockPos) -> RAIN
                    biome.precipitation == Biome.Precipitation.SNOW && biome.coldEnoughToSnow(blockPos) -> SNOW
                    else -> null
                }
            }
        }
    }

    public sealed class BiomeState : StatedSource<Location>() {
        override fun get(player: Player): Boolean = this[player.location]

        public object STD : BiomeState() {
            override fun get(input: Location): Boolean = getBiomeState(input) === this
        }

        public object WARM : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(COLD)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.HOT)
            override fun get(input: Location): Boolean = getBiomeState(input) === this
        }

        public object COLD : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(WARM)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun get(input: Location): Boolean = getBiomeState(input) === this
        }

        public companion object {
            public fun getBiomeState(location: Location): BiomeState {
                requireNotNull(location.world) { "Location must have a world" }

                when (location.world.environment) {
                    Environment.NETHER -> return WARM
                    Environment.THE_END -> return COLD
                    else -> { /* do nothing */ }
                }

                val blockPos = location.block.castOrThrow<CraftBlock>().position
                val level = location.world.toNMS()
                val biome = level.getBiome(blockPos).value()
                return when (biome.precipitation) {
                    Biome.Precipitation.NONE -> WARM
                    Biome.Precipitation.SNOW -> COLD
                    else -> STD
                }
            }
        }
    }

    public open suspend fun activate(
        player: Player,
        origin: Origin
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped
        require(!activeStates.containsEntry(player, this)) { "Cannot activate state that is already present" }

        deactivateConflictingStates(player, origin)
        activeStates.put(player, this)

        this.addAsync(player, origin)
        this.addSync(player, origin)
    }

    public open suspend fun deactivate(
        player: Player,
        origin: Origin
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped
        require(activeStates.containsEntry(player, this)) { "Cannot deactivate state that is not present" }

        activeStates.remove(player, this)
        this.removeAsync(player, origin)
        this.removeSync(player, origin)
    }

    public open suspend fun exchange(
        player: Player,
        origin: Origin,
        to: State
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin, to)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped
        require(activeStates.containsEntry(player, this)) { "Cannot exchange state that is not present" }
        require(!activeStates.containsEntry(player, to)) { "Cannot exchange state to state that is already present" }

        activeStates.remove(player, this)
        this.removeAsync(player, origin)
        this.removeSync(player, origin)

        to.deactivateConflictingStates(player, origin)
        to.addAsync(player, origin)
        to.addSync(player, origin)
        activeStates.put(player, to)
    }

    private suspend fun deactivateConflictingStates(
        player: Player,
        origin: Origin
    ) = activeStates[player]
        .filter { this in it.incompatibleStates || it in this.incompatibleStates }
        .forEach { state -> state.deactivate(player, origin) }

    private suspend fun addAsync(
        player: Player,
        origin: Origin
    ) = with(origin.stateData[this@State]) {
        title.tap { title -> title(player) }
        action.tap { action -> action(player) }
        modifiers.forEach { modifier -> modifier(player) }
    }

    private fun removeAsync(
        player: Player,
        origin: Origin
    ) = origin.stateData[this@State].modifiers
        .groupBy(AttributeModifierBuilder::attribute)
        .mapKeys { (attribute, _) -> player.getAttribute(attribute) }
        .filterKeys { attribute -> attribute != null }
        .forEach { (_, modifiers) -> modifiers.forEach { it.remove(player) } }

    private fun addSync(
        player: Player,
        origin: Origin
    ) = sync { origin.stateData[this@State].potions.forEach { it(player) } }

    private fun removeSync(
        player: Player,
        origin: Origin
    ) = sync { origin.stateData[this@State].potions.map(PotionEffectBuilder::type).forEach(player::removePotionEffect) }

    private fun sentryMessage(
        origin: Origin,
        otherState: State? = null
    ): String {
        return buildString {
            val functionName = StackWalker.getInstance().walk { stream ->
                stream.skip(2).map { it.methodName.substringBefore('$') }.findFirst().get()
            }
            append(origin.name)

            append('.')
            append(this@State.name)
            append(".")
            append(functionName)

            if (otherState != null) {
                append('.')
                append(otherState.name)
            }
        }
    }

    final override fun toString(): String = name
    final override fun hashCode(): Int = name.hashCode()

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is State) return false

        if (name != other.name) return false
        if (!incompatibleStates.contentEquals(other.incompatibleStates)) return false
        if (!providesSideEffects.contentEquals(other.providesSideEffects)) return false

        return true
    }

    public companion object : KoinComponent {
        private const val CATEGORY = "terix.origin.state"
        internal val activeStates = concurrentMultimap<Player, State>()
        private val plugin by getKoin().inject<Terix>()
        public val values: Array<State> = RecursionUtils.recursiveFinder<KClass<out State>>(
            State::class,
            finder = { this.sealedSubclasses },
            filter = { false }
        ).filter { it.isFinal }.map { it.objectInstance!! }.toTypedArray()

        public fun getPlayerStates(player: Player): PersistentSet<State> = activeStates[player].toPersistentSet()

        public fun valueOf(name: String): State {
            plugin.log.debug { values.joinToString(", ") { it.name } }
            return values.find { it.name == name } ?: throw IllegalArgumentException("No State with name $name")
        }

        public fun getTimeState(world: World): TimeState? = when {
            world.environment != Environment.NORMAL -> null
            world.isDayTime -> TimeState.DAY
            world.isNight -> TimeState.NIGHT
            else -> null
        }

        public fun getEnvironmentState(environment: Environment): WorldState = when (environment) {
            Environment.NORMAL -> WorldState.OVERWORLD
            Environment.NETHER -> WorldState.NETHER
            Environment.THE_END -> WorldState.END
            else -> throw IllegalArgumentException("Environment $environment is not supported")
        }

        public fun getLiquidState(block: Block): LiquidState = when (block.liquidType) {
            LiquidType.WATER -> LiquidState.WATER
            LiquidType.LAVA -> LiquidState.LAVA
            else -> LiquidState.LAND
        }

        init {
            get<Terix>().events {
                suspend fun TerixPlayer.maybeExchange(
                    last: State,
                    current: State
                ) { if (last != current) last.exchange(this.backingPlayer, this.origin, current) }

                fun <E : Event, I> generateLambda(
                    playerGetter: E.() -> Player,
                    lastInputGetter: E.() -> I,
                    currentInputGetter: E.() -> I,
                    inputToState: I.() -> State
                ): suspend (E) -> Unit = { event ->
                    TerixPlayer[playerGetter(event)].maybeExchange(
                        event.lastInputGetter().inputToState(),
                        event.currentInputGetter().inputToState()
                    )
                }

                fun <E : PlayerEvent, I> generateLambda(
                    lastInputGetter: E.() -> I,
                    currentInputGetter: E.() -> I,
                    inputToState: I.() -> State
                ) = generateLambda(
                    { player },
                    lastInputGetter,
                    currentInputGetter,
                    inputToState
                )

                events(
                    PlayerLiquidEnterEvent::class,
                    PlayerLiquidExitEvent::class,
                    priority = EventPriority.MONITOR,
                    ignoreCancelled = true,
                    block = generateLambda({ previousType }, { newType }, ::convertLiquidToState)
                )

                event<PlayerMoveFullXYZEvent>(
                    EventPriority.MONITOR,
                    true,
                    block = generateLambda({ from }, { to }, ::getBiomeState)
                )

                event<PlayerChangedWorldEvent>(
                    EventPriority.MONITOR,
                    true,
                    forceAsync = true,
                    generateLambda({ from.environment }, { player.world.environment }, ::getEnvironmentState)
                )

                events(
                    WorldDayEvent::class,
                    WorldNightEvent::class,
                    priority = EventPriority.MONITOR,
                    ignoreCancelled = true,
                    forceAsync = true
                ) {
                    onlinePlayers.filter(Player::inOverworld)
                        .map(TerixPlayer::get)
                        .onEach { player ->
                            val currentState = getTimeState(player.world)!!
                            player.maybeExchange(
                                currentState.opposite,
                                currentState
                            )
                        }
                }
            }
        }
    }
}
