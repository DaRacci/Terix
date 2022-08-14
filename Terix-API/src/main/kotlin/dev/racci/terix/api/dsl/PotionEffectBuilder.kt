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

class PotionEffectBuilder() {

    constructor(builder: PotionEffectBuilder.() -> Unit) : this() {
        builder(this)
    }

    var amplifier: Int = 1
    var duration: Duration = 20.ticks
    var durationInt: Int? = null
    var type: PotionEffectType? = null
    var ambient: Boolean = false
    var particles: Boolean? = null
    var icon: Boolean? = null
    var key: NamespacedKey? = null

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

    fun build(): PotionEffect = PotionEffect(
        type ?: error("Type must be set for potion builder."),
        durationInt ?: duration.inWholeTicks.coerceAtMost(Integer.MAX_VALUE.toLong()).toInt(),
        amplifier,
        ambient,
        particles ?: !ambient,
        icon ?: particles ?: !ambient,
        key.takeUnless { it == null || !it.toString().matches(regex) } ?: error("Invalid key. Was null or didn't match ${regex.pattern}: $key")
    )

    companion object {
        val regex by lazy { Regex("^terix:origin_(?<type>potion|ability|food)_(?<from>[a-z_-]+)?(/(?<state>[a-z_-]+))?$") }

        fun build(builder: PotionEffectBuilder.() -> Unit): PotionEffect = PotionEffectBuilder(builder).build()
    }
}
