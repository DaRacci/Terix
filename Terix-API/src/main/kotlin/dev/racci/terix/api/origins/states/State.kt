package dev.racci.terix.api.origins.states

import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.data.enums.LiquidType.Companion.liquidType
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.isNight
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.extensions.concurrentMultimap
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

    public sealed class StatedSource<I : Any> : State(), StateSource<I>

    public object CONSTANT : StatedSource<Nothing>() {
        override fun fromPlayer(player: Player): Boolean = true
        override fun getState(input: Nothing): Boolean = true
    }

    public sealed class TimeState : StatedSource<World>() {
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

    public sealed class WorldState : StatedSource<Environment>() {
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

    public sealed class LiquidState : StatedSource<Block>() {
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

    public sealed class LightState : StatedSource<Player>() {
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

    public sealed class WeatherState : StatedSource<Location>() {
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

    public sealed class BiomeState : StatedSource<Location>() {
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

    init {
        @Suppress("LeakingThis")
        values += this
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

    private fun addSync(
        player: Player,
        origin: Origin
    ) = sync {
        origin.statePotions[this@State]?.forEach(player::addPotionEffect)
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

    final override fun toString(): String = name
    final override fun hashCode(): Int = name.hashCode()

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
        internal val activeStates = concurrentMultimap<Player, State>()
        private val ordinalInc: AtomicInt = atomic(0)
        private val plugin by getKoin().inject<Terix>()
        public var values: Array<State> = emptyArray()
            private set

        public fun getPlayerStates(player: Player): PersistentSet<State> = activeStates[player].toPersistentSet()

        public fun fromOrdinal(ordinal: Int): State {
            return values[ordinal]
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
