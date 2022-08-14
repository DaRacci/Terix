package dev.racci.terix.api.origins.states

import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.isNight
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.sentryScoped
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.atomic
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
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
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.functions.math.PiFunction.functionName
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration

sealed class State : KoinComponent, WithPlugin<Terix> {
    final override val plugin: Terix by inject()

    val ordinal: Int = ordinalInc.getAndIncrement()
    val name: String = this::class.simpleName ?: throw IllegalStateException("Anonymous classes aren't supported.")

    open val incompatibleStates: Array<out State> = emptyArray()
    open val providesSideEffects: Array<out SideEffect> = emptyArray()

    object CONSTANT : State(), StateSource<Nothing> {
        override fun getState(input: Nothing): Boolean = true
    }

    sealed class TimeState : State(), StateSource<World> {
        override fun fromPlayer(player: Player) = this.getState(player.world)

        object DAY : TimeState() {
            override val incompatibleStates: Array<out State> = arrayOf(NIGHT)
            override fun getState(input: World): Boolean = getTimeState(input) === this
        }

        object NIGHT : TimeState() {
            override val incompatibleStates: Array<out State> = arrayOf(DAY)
            override fun getState(input: World): Boolean = getTimeState(input) === this
        }
    }

    sealed class WorldState : State(), StateSource<Environment> {
        override fun fromPlayer(player: Player) = this.getState(player.world.environment)

        object OVERWORLD : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(NETHER, END)
            override fun getState(input: Environment): Boolean = input === Environment.NORMAL
        }

        object NETHER : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, END, TimeState.DAY, TimeState.NIGHT)
            override fun getState(input: Environment): Boolean = input === Environment.NETHER
        }

        object END : WorldState() {
            override val incompatibleStates: Array<out State> = arrayOf(OVERWORLD, NETHER, TimeState.DAY, TimeState.NIGHT)
            override fun getState(input: Environment): Boolean = input === Environment.THE_END
        }
    }

    sealed class LiquidState : State(), StateSource<Block> {
        override fun fromPlayer(player: Player) = this.getState(player.location.block)

        object WATER : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(LAVA, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }

        object LAVA : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAND)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.BURNING)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }

        object LAND : LiquidState() {
            override val incompatibleStates: Array<out State> = arrayOf(WATER, LAVA)
            override fun getState(input: Block): Boolean = getLiquidState(input) === this
        }
    }

    sealed class LightState : State(), StateSource<Player> {
        override fun fromPlayer(player: Player) = this.getState(player)

        object SUNLIGHT : LightState() {
            override val incompatibleStates: Array<out State> = arrayOf(DARKNESS)
            override fun getState(input: Player): Boolean = getLightState(input) === this
        }

        object DARKNESS : LightState() {
            override val incompatibleStates: Array<out State> = arrayOf(SUNLIGHT)
            override fun getState(input: Player): Boolean = getLightState(input) === this
        }
    }

    sealed class WeatherState : State(), StateSource<Location> {
        override fun fromPlayer(player: Player) = this.getState(player.location)

        object RAIN : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(SNOW)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.WET)
            override fun getState(input: Location): Boolean = getWeatherState(input) === this
        }

        object SNOW : WeatherState() {
            override val incompatibleStates: Array<out State> = arrayOf(RAIN)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun getState(input: Location): Boolean = getWeatherState(input) === this
        }
    }

    sealed class BiomeState : State(), StateSource<Location> {
        override fun fromPlayer(player: Player) = this.getState(player.location)

        object WARM : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(COLD)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.HOT)
            override fun getState(input: Location): Boolean = getBiomeState(input) === this
        }

        object COLD : BiomeState() {
            override val incompatibleStates: Array<out State> = arrayOf(WARM)
            override val providesSideEffects: Array<out SideEffect> = arrayOf(SideEffect.Temperature.COLD)
            override fun getState(input: Location): Boolean = getBiomeState(input) === this
        }
    }

    private suspend fun ifContains(
        player: Player,
        required: Boolean,
        state: State,
        block: suspend () -> Unit
    ) {
        val isPresent = activeTriggers[player]?.contains(state)
        if (isPresent != required) return

        block()
        activeTriggers.put(player, state)
    }

    open suspend fun activate(
        player: Player,
        origin: Origin
    ) = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        ifContains(player, false, this) {
            addAsync(player, origin)
            addSync(player, origin)
        }
    }

    open suspend fun deactivate(
        player: Player,
        origin: Origin
    ) = sentryScoped(player, CATEGORY, this.sentryMessage(origin)) {
        ifContains(player, true, this) {
            removeAsync(player, origin)
            removeSync(player, origin)
        }
    }

    open suspend fun exchange(
        player: Player,
        origin: Origin,
        to: State
    ) = sentryScoped(player, CATEGORY, this.sentryMessage(origin, null, to)) {
        ifContains(player, true, this) {
            this.removeAsync(player, origin)
            this.removeSync(player, origin)
        }

        ifContains(player, true, to) {
            to.addAsync(player, origin)
            to.addSync(player, origin)
        }
    }

    init {
        @Suppress("LeakingThis")
        values.add(this)
    }

    private suspend fun addAsync(
        player: Player,
        origin: Origin
    ) = async {
        origin.titles[this@State]?.invoke(player)
        origin.triggerBlocks[this@State]?.invoke(player)
        origin.attributeModifiers[this@State]?.takeUnless(Collection<*>::isEmpty)?.forEach { (attribute, modifier) -> player.getAttribute(attribute)?.addModifier(modifier) }
    }

    private fun removeAsync(
        player: Player,
        origin: Origin
    ) = async {
        origin.attributeModifiers[this@State]?.takeUnless(Collection<*>::isEmpty)?.forEach { (attribute, modifier) -> player.getAttribute(attribute)?.removeModifier(modifier) }
    }

    // TODO: Cache if the player has their nightvision potion
    private fun addSync(
        player: Player,
        origin: Origin
    ) = sync {
        origin.potions[this@State]?.takeUnless(Collection<*>::isEmpty)?.let(player::addPotionEffects)

//        if (origin.nightVision && transaction { PlayerData[player].nightVision == this@State } && !player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }.fromOrigin()) {
//            player.addPotionEffect(this@State.nightVisionPotion(origin))
//        }
    }

    private fun removeSync(
        player: Player,
        origin: Origin
    ) = sync {
        origin.potions[this@State]?.takeUnless(Collection<*>::isEmpty)?.map(PotionEffect::getType)?.forEach(player::removePotionEffect)

//        if (origin.nightVision && transaction { PlayerData[player].nightVision == this@State } && player.activePotionEffects.find { it.type == PotionEffectType.NIGHT_VISION }.fromOrigin()) {
//            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
//        }
    }

    private fun PotionEffect?.fromOrigin(): Boolean {
        if (this == null) return false
        return PotionEffectBuilder.regex.matches(key.toString())
    }

    private fun nightVisionPotion(
        origin: Origin
    ) = PotionEffectBuilder {
        type = PotionEffectType.NIGHT_VISION
        duration = Duration.INFINITE
        amplifier = 0
        ambient = true
        originKey(origin, this@State)
    }.build()

    private fun sentryMessage(
        origin: Origin,
        otherOrigin: Origin? = null,
        otherState: State? = null
    ): String {
        return StringBuilder(origin.name).apply {
            if (otherOrigin != null) {
                append('.')
                append(functionName)
                append('.')
                append(otherOrigin.name)
            }

            if (otherOrigin != null && this.endsWith(otherOrigin.name)) {
                this.append(" | ")
            }

            append(this@State.name)
            append(".")
            append(functionName)

            if (otherState != null) {
                append('.')
                append(otherState.name)
            }
        }.toString()
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

    companion object {
        private const val CATEGORY = "terix.origin.trigger"
        private val activeTriggers = multiMapOf<Player, State>()
        private val ordinalInc: AtomicInt = atomic(0)
        val values: PersistentList<State> = persistentListOf()

        fun getPlayerStates(player: Player) = activeTriggers[player]?.toPersistentSet() ?: emptySet<State>().toPersistentSet()

        fun fromOrdinal(ordinal: Int): State = values[ordinal]

        fun valueOf(name: String): State = values.find { it.name == name } ?: throw IllegalArgumentException("No trigger with name $name")

        fun getTimeState(world: World): TimeState? = when {
            world.environment != Environment.NORMAL -> null
            world.isDayTime -> TimeState.DAY
            world.isNight -> TimeState.NIGHT
            else -> null
        }

        fun getEnvironmentState(environment: Environment): WorldState = when (environment) {
            Environment.NORMAL -> WorldState.OVERWORLD
            Environment.NETHER -> WorldState.NETHER
            Environment.THE_END -> WorldState.END
            else -> throw IllegalArgumentException("Environment $environment is not supported")
        }

        fun getLiquidState(block: Block): LiquidState = when (block.liquidType) {
            LiquidType.WATER -> LiquidState.WATER
            LiquidType.LAVA -> LiquidState.LAVA
            else -> LiquidState.LAND
        }

        fun getLightState(player: Player): LightState? {
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

        fun getWeatherState(location: Location): WeatherState? {
            if (location.world == null || location.world.environment != Environment.NORMAL) {
                return null
            }

            val blockPos = location.block.unsafeCast<CraftBlock>().position
            val level = location.world.toNMS()

            if (!location.world.hasStorm() || !level.canSeeSky(blockPos) || level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockPos).y > blockPos.y) return null

            val biome = level.getBiome(blockPos).value()
            return when {
                biome.precipitation == Biome.Precipitation.RAIN && biome.warmEnoughToRain(blockPos) -> WeatherState.RAIN
                biome.precipitation == Biome.Precipitation.SNOW && biome.coldEnoughToSnow(blockPos) -> WeatherState.SNOW
                else -> null
            }
        }

        fun getBiomeState(location: Location): BiomeState? {
            if (location.world == null) {
                return null
            }

            when (location.world.environment) {
                Environment.NETHER -> return BiomeState.WARM
                Environment.THE_END -> return BiomeState.COLD
                else -> { /* do nothing */
                }
            }

            val blockPos = location.block.unsafeCast<CraftBlock>().position
            val level = location.world.toNMS()
            val biome = level.getBiome(blockPos).value()
            return when (biome.precipitation) {
                Biome.Precipitation.NONE -> BiomeState.WARM
                Biome.Precipitation.SNOW -> BiomeState.COLD
                else -> null
            }
        }

        fun convertLiquidToState(liquid: LiquidType) = when (liquid) {
            LiquidType.WATER -> LiquidState.WATER
            LiquidType.LAVA -> LiquidState.LAVA
            else -> LiquidState.LAND
        }
    }
}
