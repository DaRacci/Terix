package dev.racci.terix.api.dsl

import dev.racci.minix.api.extensions.inWholeTicks
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import kotlin.reflect.KClass
import kotlin.time.Duration

class PotionEffectBuilder(
    amplifier: Int? = null,
    duration: Duration? = null,
    type: PotionEffectType? = null,
    ambient: Boolean? = false,
    particles: Boolean? = null,
    icon: Boolean? = null,
    key: NamespacedKey? = null
) : CachingBuilder<PotionEffect>() {

    var amplifier by createWatcher(amplifier ?: 1)
    var duration by createWatcher(duration ?: 20.ticks)
    var type by createWatcher(type)
    var ambient by createWatcher(ambient ?: false)
    var particles by createWatcher(particles ?: ambient)
    var icon by createWatcher(icon)
    var key by createWatcher(key)

    inline fun <reified O : Origin> originKey(state: State) = originKey(OriginService.getOrigin(O::class), state)

    fun originKey(
        origin: OriginValues,
        state: State
    ) = originKey(origin.name, state.name)

    fun originKey(
        origin: String,
        state: String
    ): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_potion_${origin.lowercase()}/${state.lowercase()}")
        return this
    }

    inline fun <reified A : Ability> abilityKey() = abilityKey(A::class)

    fun abilityKey(ability: KClass<out Ability>) = abilityKey(ability.simpleName ?: error("Cannot use anonymous classes as abilities."))

    fun abilityKey(ability: String): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_ability_${ability.lowercase()}")
        return this
    }

    fun foodKey(food: Material) = foodKey(food.name)

    fun foodKey(food: String): PotionEffectBuilder {
        this.key = NamespacedKey("terix", "origin_food_${food.lowercase()}")
        return this
    }

    override fun create() = PotionEffect(
        ::type.watcherOrNull() ?: error("Type must be set for potion builder."),
        duration.inWholeTicks.coerceAtMost(Integer.MAX_VALUE.toLong()).toInt(),
        amplifier,
        ambient,
        ::particles.watcherOrNull() ?: !ambient,
        ::icon.watcherOrNull() ?: ::particles.watcherOrNull() ?: !ambient,
        ::key.watcherOrNull().takeUnless { it == null || !it.toString().matches(regex) } ?: error("Invalid key. Was null or didn't match ${regex.pattern}: $key")
    )

    companion object {
        val regex by lazy { Regex("^terix:origin_(?<type>potion|ability|food)_(?<from>[a-z_-]+)?(/(?<state>[a-z_-]+))?$") }
    }
}
