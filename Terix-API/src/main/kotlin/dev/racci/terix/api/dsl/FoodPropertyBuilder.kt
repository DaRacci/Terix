package dev.racci.terix.api.dsl

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import org.bukkit.potion.PotionEffect
import java.util.Optional

class FoodPropertyBuilder(
    nutrition: Int?,
    saturationModifier: Float?,
    isMeat: Boolean?,
    canAlwaysEat: Boolean?,
    fastFood: Boolean?,
    effects: ArrayList<Pair<MobEffectInstance, Float>>?
) : CachingBuilder<FoodProperties>() {

    constructor(foodProperties: FoodProperties?) : this(
        foodProperties?.nutrition ?: 0,
        foodProperties?.saturationModifier ?: 0.0f,
        foodProperties?.isMeat ?: false,
        foodProperties?.canAlwaysEat() ?: false,
        foodProperties?.isFastFood ?: false,
        null
    ) {
        if (foodProperties == null) return
        foodProperties.effects.mapTo(effects) { it.first to it.second }
    }

    constructor(existing: FoodPropertyBuilder) : this(null, null, null, null, null, null) {
        this.effects.addAll(existing.effects)
        this.nutrition = existing.nutrition
        this.saturationModifier = existing.saturationModifier
        this.isMeat = existing.isMeat
        this.canAlwaysEat = existing.canAlwaysEat
        this.fastFood = existing.fastFood
    }

    var nutrition by createWatcher(nutrition ?: 0)
    var saturationModifier by createWatcher(saturationModifier ?: 0.0f)
    var isMeat by createWatcher(isMeat ?: false)
    var canAlwaysEat by createWatcher(canAlwaysEat ?: false)
    var fastFood by createWatcher(fastFood ?: false)
    var effects by createWatcher(effects ?: arrayListOf())

    fun clearEffects() {
        effects.clear()
        dirty = true
    }

    fun addEffect(effect: PotionEffect) {
        effects.add(
            MobEffectInstance(
                MobEffect.byId(effect.type.id)!!,
                effect.duration,
                effect.amplifier,
                effect.isAmbient,
                effect.hasParticles(),
                effect.hasIcon(),
                effect.key,
                Optional.empty()
            ) to 1f
        )
    }

    override fun create(): FoodProperties = FoodProperties.Builder()
        .nutrition(nutrition)
        .saturationMod(saturationModifier)
        .apply {
            if (isMeat) meat()
            if (canAlwaysEat) alwaysEat()
            if (fastFood) fast()
            effects.forEach { (effect, chance) -> effect(effect, chance) }
        }.build()
}
