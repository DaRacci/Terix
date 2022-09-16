package dev.racci.terix.api.dsl

import dev.racci.minix.api.utils.safeCast
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import java.util.Optional
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.time.DurationUnit

class FoodPropertyBuilder() {

    constructor(foodPropertyBuilder: FoodPropertyBuilder.() -> Unit) : this() {
        foodPropertyBuilder()
    }

    constructor(foodProperties: FoodProperties?) : this() {
        if (foodProperties == null) return

        foodProperties.effects.mapTo(effects) { it.first to it.second }
        this.nutrition = foodProperties.nutrition
        this.saturationModifier = foodProperties.saturationModifier
        this.isMeat = foodProperties.isMeat
        this.canAlwaysEat = foodProperties.canAlwaysEat()
        this.fastFood = foodProperties.isFastFood
    }

    private var cached: FoodProperties? = null
    private var changed: Boolean = true
    private val effects: ArrayList<Pair<MobEffectInstance, Float>> = arrayListOf()

    var nutrition: Int = 0
        set(value) { if (changeValue("nutrition", value)) field = value }
    var saturationModifier: Float = 0f
        set(value) { if (changeValue("saturationModifier", value)) field = value }
    var isMeat: Boolean = false
        set(value) { if (changeValue("isMeat", value)) field = value }
    var canAlwaysEat: Boolean = false
        set(value) { if (changeValue("canAlwaysEat", value)) field = value }
    var fastFood: Boolean = false
        set(value) { if (changeValue("fastFood", value)) field = value }

    fun clearEffects() {
        effects.clear()
        changed = true
    }

    fun addEffect(potionEffectBuilder: PotionEffectBuilder.() -> Unit) {
        val effect = PotionEffectBuilder(potionEffectBuilder)
        effects.add(
            MobEffectInstance(
                MobEffect.byId(effect.type!!.id)!!,
                effect.duration.toInt(DurationUnit.MILLISECONDS) / 50,
                effect.amplifier,
                effect.ambient,
                effect.particles ?: effect.ambient,
                effect.icon ?: effect.ambient,
                effect.key,
                Optional.empty()
            ) to 1f
        )
    }

    private fun changeValue(field: String, value: Any): Boolean {
        val prop = this::class.memberProperties.first { it.name == field }
        val setter = prop.safeCast<KMutableProperty1<FoodPropertyBuilder, Any>>()

        if (setter != null && setter.get(this) != value) {
            changed = true
            return true
        }

        return false
    }

    fun build(): FoodProperties {
        if (!changed) return cached!!

        val builder = FoodProperties.Builder()
            .nutrition(nutrition)
            .saturationMod(saturationModifier)

        if (isMeat) builder.meat()
        if (canAlwaysEat) builder.alwaysEat()
        if (fastFood) builder.fast()

        effects.forEach { builder.effect(it.first, it.second) }

        cached = builder.build()
        changed = false
        return cached!!
    }

    override fun toString(): String {
        return "FoodPropertyBuilder(nutrition=$nutrition, saturationModifier=$saturationModifier, isMeat=$isMeat, canAlwaysEat=$canAlwaysEat, fastFood=$fastFood, effects=$effects)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FoodPropertyBuilder) return false

        if (cached != other.cached) return false
        if (changed != other.changed) return false
        if (effects != other.effects) return false
        if (nutrition != other.nutrition) return false
        if (saturationModifier != other.saturationModifier) return false
        if (isMeat != other.isMeat) return false
        if (canAlwaysEat != other.canAlwaysEat) return false
        if (fastFood != other.fastFood) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cached?.hashCode() ?: 0
        result = 31 * result + changed.hashCode()
        result = 31 * result + effects.hashCode()
        result = 31 * result + nutrition
        result = 31 * result + saturationModifier.hashCode()
        result = 31 * result + isMeat.hashCode()
        result = 31 * result + canAlwaysEat.hashCode()
        result = 31 * result + fastFood.hashCode()
        return result
    }
}
