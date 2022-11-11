package dev.racci.terix.api.origins.states

import arrow.analysis.pre
import arrow.analysis.unsafeCall
import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.data.enums.LiquidType.Companion.liquidType
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.isNight
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.sentryScoped
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
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
import org.bukkit.potion.PotionEffect
import org.koin.core.component.inject

public sealed class State : WithPlugin<Terix> {
    final override val plugin: Terix by inject()

    public val ordinal: Int = ordinalInc.getAndIncrement()
    public val name: String = this::class.simpleName ?: throw IllegalStateException("Anonymous classes aren't supported.")

    public open val incompatibleStates: Array<out State> = emptyArray()
    public open val providesSideEffects: Array<out SideEffect> = emptyArray()

    public object CONSTANT : State(), StateSource<Nothing> {
        override fun fromPlayer(player: Player): Boolean = true
        override fun getState(input: Nothing): Boolean = true
    }

    public sealed class TimeState : State(), StateSource<World> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player.world)

        public object DAY : TimeState() {
            override val incompatibleStates: Array<out State> = arrayOf(NIGHT)
            override fun getState(input: World): Boolean = getTimeState(input) === this
        }

        public object NIGHT : TimeState() {
            override val incompatibleStates: Array<out State> = arrayOf(DAY)
            override fun getState(input: World): Boolean = getTimeState(input) === this
        }
    }

    public sealed class WorldState : State(), StateSource<Environment> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player.world.environment)

        public object OVERWORLD : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(NETHER, END)
            override fun getState(input: Environment): Boolean = input === Environment.NORMAL
        }

        public object NETHER : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, END, TimeState.DAY, TimeState.NIGHT)
            override fun getState(input: Environment): Boolean = input === Environment.NETHER
        }

        public object END : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, NETHER, TimeState.DAY, TimeState.NIGHT)
            override fun getState(input: Environment): Boolean = input === Environment.THE_END
        }
    }

    public sealed class LiquidState : State(), StateSource<Block> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player.location.block)

        public object WATER : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(LAVA, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }

        public object LAVA : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.BURNING)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }

        public object LAND : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAVA)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }
    }

    public sealed class LightState : State(), StateSource<Player> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player)

        public object SUNLIGHT : LightState() {
            override val incompatibleStates: Array<out State> = arrayOf(DARKNESS)
            override fun getState(input: Player): Boolean = getLightState(input) === this
        }

        public object DARKNESS : LightState() {
            override val incompatibleStates: Array<out State> = arrayOf(SUNLIGHT)
            override fun getState(input: Player): Boolean = getLightState(input) === this
        }
    }

    public sealed class WeatherState : State(), StateSource<Location> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player.location)

        public object RAIN : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(SNOW)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun getState(input: Location): Boolean = getWeatherState(input) === this
        }

        public object SNOW : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(RAIN)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun getState(input: Location): Boolean = getWeatherState(input) === this
        }
    }

    public sealed class BiomeState : State(), StateSource<Location> {
        override fun fromPlayer(player: Player): Boolean = this.getState(player.location)

        public object WARM : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(COLD)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.HOT)
            override fun getState(input: Location): Boolean = getBiomeState(input) === this
        }

        public object COLD : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(WARM)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun getState(input: Location): Boolean = getBiomeState(input) === this
        }
    }

    private suspend fun ifContains(
        player: Player,
        origin: Origin,
        required: Boolean,
        state: State,
        block: suspend () -> Unit
    ) {
        val states = activeStates[player]
        val isPresent = states?.contains(state) ?: false
        if (isPresent != required) return

        if (!required) {
            states?.filter { state in it.incompatibleStates || it in state.incompatibleStates }?.forEach { it.deactivate(player, origin) }
            activeStates.put(player, state)
        } else {
            activeStates.remove(player, state)
        }

        block()
    }

    public open suspend fun activate(
        player: Player,
        origin: Origin
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped

        ifContains(player, origin, false, this) {
            this.addAsync(player, origin)
            this.addSync(player, origin)
        }
    }

    public open suspend fun deactivate(
        player: Player,
        origin: Origin
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped

        ifContains(player, origin, true, this) {
            this.removeAsync(player, origin)
            this.removeSync(player, origin)
        }
    }

    public open suspend fun exchange(
        player: Player,
        origin: Origin,
        to: State
    ): Unit = sentryScoped(player, CATEGORY, this.sentryMessage(origin, to)) {
        if (OriginHelper.shouldIgnorePlayer(player)) return@sentryScoped

        ifContains(player, origin, true, this) {
            this.removeAsync(player, origin)
            this.removeSync(player, origin)
        }

        ifContains(player, origin, false, to) {
            to.addAsync(player, origin)
            to.addSync(player, origin)
        }
    }

    init {
        @Suppress("LeakingThis")
        states.add(this)
    }

    private suspend fun addAsync(
        player: Player,
        origin: Origin
    ) = async {
        origin.stateTitles[this@State]?.invoke(player)
        origin.stateBlocks[this@State]?.invoke(player)

        origin.attributeModifiers[this@State]?.takeUnless(Collection<*>::isEmpty)?.forEach { (attribute, modifier) ->
            with(player.getAttribute(attribute)) {
                if (this == null) return@forEach logger.debug(scope = CATEGORY) { "Attribute instance $attribute not found" }
                addModifier(modifier)
            }
        }
    }

    private fun removeAsync(
        player: Player,
        origin: Origin
    ) = async {
        origin.attributeModifiers[this@State]?.takeUnless(Collection<*>::isEmpty)?.forEach { (attribute, modifier) ->
            with(player.getAttribute(attribute)) {
                if (this == null) return@forEach plugin.log.debug(scope = CATEGORY) { "Attribute instance $attribute not found" }
                removeModifier(modifier)
            }
        }
    }

    // TODO: Cache if the player has their nightvision potion
    private fun addSync(
        player: Player,
        origin: Origin
    ) = sync {
        origin.statePotions[this@State]?.takeUnless(Collection<*>::isEmpty)?.forEach { potion ->
            player.addPotionEffect(potion)
        }
    }

    private fun removeSync(
        player: Player,
        origin: Origin
    ) = sync {
        origin.statePotions[this@State]?.takeUnless(Collection<*>::isEmpty)?.map(PotionEffect::getType)?.forEach {
            player.removePotionEffect(it)
        }
    }

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

    final override fun toString(): String {
        return name
    }

    final override fun hashCode(): Int {
        return name.hashCode()
    }

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is State) return false

        if (ordinal != other.ordinal) return false
        if (name != other.name) return false
        if (!incompatibleStates.contentEquals(other.incompatibleStates)) return false
        if (!providesSideEffects.contentEquals(other.providesSideEffects)) return false

        return true
    }

    public companion object {
        private const val CATEGORY = "terix.origin.state"
        internal val activeStates = multiMapOf<Player, State>()
        private val ordinalInc: AtomicInt = atomic(0)
        private val plugin by getKoin().inject<Terix>()
        private val states = mutableSetOf<State>()
        public var values: Array<State> = emptyArray()
            private set
            get() {
                if (field.size != states.size) {
                    field = states.toTypedArray()
                }
                return field
            }

        public fun recalculateAllStates(player: Player) {
            activeStates.remove(player)

            for (state in values) {
                if (state !is StateSource<*>) {
                    plugin.log.debug { "Skipping state $state" }
                    continue
                }

                if (state.incompatibleStates.any { activeStates[player]?.contains(it) == true }) continue
                if (!state.fromPlayer(player)) continue

                plugin.log.debug { "Adding state $state" }
                activeStates.put(player, state)
            }
        }

        public fun getPlayerStates(player: Player): PersistentSet<State> = activeStates[player]?.toPersistentSet() ?: emptySet<State>().toPersistentSet()

        public fun fromOrdinal(ordinal: Int): State {
            pre(ordinal < ordinalInc.value) { "Ordinal $ordinal is out of bounds" }
            return unsafeCall(values[ordinal])
        }

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

        public fun getLightState(player: Player): LightState? {
            val inventory = player.inventory
            if (inventory.itemInMainHand.type != Material.TORCH && inventory.itemInOffHand.type != Material.TORCH && player.location.block.lightLevel < 5) {
                return LightState.DARKNESS
            }

            val nms = player.toNMS()
            val pos = BlockPos(nms.x, nms.eyeY, nms.z)
            if (player.world.toNMS().canSeeSky(pos)) {
                return LightState.SUNLIGHT
            }

            return null
        }

        public fun getWeatherState(location: Location): WeatherState? {
            if (location.world == null || location.world.environment != Environment.NORMAL) {
                return null
            }

            val blockPos = location.block.castOrThrow<CraftBlock>().position
            val level = location.world.toNMS()

            if (!location.world.hasStorm() || !level.canSeeSky(blockPos) || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).y > blockPos.y) return null

            val biome = level.getBiome(blockPos).value()
            return when {
                biome.precipitation == Biome.Precipitation.RAIN && biome.warmEnoughToRain(blockPos) -> WeatherState.RAIN
                biome.precipitation == Biome.Precipitation.SNOW && biome.coldEnoughToSnow(blockPos) -> WeatherState.SNOW
                else -> null
            }
        }

        public fun getBiomeState(location: Location): BiomeState? {
            if (location.world == null) {
                return null
            }

            when (location.world.environment) {
                Environment.NETHER -> return BiomeState.WARM
                Environment.THE_END -> return BiomeState.COLD
                else -> { /* do nothing */
                }
            }

            val blockPos = location.block.castOrThrow<CraftBlock>().position
            val level = location.world.toNMS()
            val biome = level.getBiome(blockPos).value()
            return when (biome.precipitation) {
                Biome.Precipitation.NONE -> BiomeState.WARM
                Biome.Precipitation.SNOW -> BiomeState.COLD
                else -> null
            }
        }

        public fun convertLiquidToState(liquid: LiquidType): LiquidState = when (liquid) {
            LiquidType.WATER -> LiquidState.WATER
            LiquidType.LAVA -> LiquidState.LAVA
            else -> LiquidState.LAND
        }
    }
}
