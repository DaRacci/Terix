package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.inWholeTicks
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

public class PotionEffectBuilder(
    amplifier: Int? = null,
    duration: Duration? = null,
    type: PotionEffectType? = null,
    ambient: Boolean? = false,
    particles: Boolean? = null,
    icon: Boolean? = null,
    key: NamespacedKey? = null
) : CachingBuilder<PotionEffect>() {

    public var amplifier: Int by createWatcher(amplifier ?: 1)
    public var duration: Duration by createWatcher(duration ?: 20.ticks)
    public var type: PotionEffectType by createWatcher(type)
    public var ambient: Boolean by createWatcher(ambient ?: false)
    public var particles: Boolean by createWatcher(particles ?: ambient)
    public var icon: Boolean by createWatcher(icon)
    public var key: NamespacedKey by createWatcher(key)

    public inline fun <reified O : Origin> originKey(state: State): PotionEffectBuilder = originKey(OriginService.getOrigin(O::class), state)

    public fun originKey(
        origin: OriginValues,
        state: State
    ): PotionEffectBuilder = originKey(origin.name, state.name)

    public fun originKey(
        origin: String,
        state: String
    ): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_potion_${origin.lowercase()}/${state.lowercase()}")
        return this
    }

    public fun abilityKey(
        origin: Origin,
        ability: KeybindAbility
    ): PotionEffectBuilder = abilityKey(origin.name, ability.name)

    public fun abilityKey(
        origin: String,
        ability: String
    ): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_ability_${origin.lowercase()}/${ability.lowercase()}")
        return this
    }

    public fun foodKey(
        origin: Origin,
        food: Material
    ): PotionEffectBuilder = foodKey(origin.name, food.name)

    public fun foodKey(
        origin: String,
        food: String
    ): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_food_${origin.lowercase()}/${food.lowercase()}")
        return this
    }

    override fun create(): PotionEffect = PotionEffect(
        ::type.watcherOrNull() ?: error("Type must be set for potion builder."),
        duration.inWholeTicks.coerceAtMost(Integer.MAX_VALUE.toLong()).toInt(),
        amplifier,
        ambient,
        ::particles.watcherOrNull() ?: !ambient,
        ::icon.watcherOrNull() ?: ::particles.watcherOrNull() ?: !ambient,
        ::key.watcherOrNull().takeUnless { it == null || !it.toString().matches(regex) } ?: error("Invalid key. Was null or didn't match ${regex.pattern}: $key")
    )

    public companion object {
        public val regex: Regex by lazy { Regex("^terix:origin_(?<type>potion|ability|food)_(?<from>[a-z_-]+)?(/(?<state>[a-z_-]+))?$") }
    }
}
