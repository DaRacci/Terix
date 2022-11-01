package dev.racci.terix.api.dsl

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import org.bukkit.potion.PotionEffect
import java.util.Optional

public class FoodPropertyBuilder(
    nutrition: Int?,
    saturationModifier: Float?,
    isMeat: Boolean?,
    canAlwaysEat: Boolean?,
    fastFood: Boolean?,
    effects: ArrayList<Pair<MobEffectInstance, Float>>?
) : CachingBuilder<FoodProperties>() {

    public constructor(foodProperties: FoodProperties?) : this(
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

    public var nutrition: Int by createWatcher(nutrition ?: 0)
    public var saturationModifier: Float by createWatcher(saturationModifier ?: 0.0f)
    public var isMeat: Boolean by createWatcher(isMeat ?: false)
    public var canAlwaysEat: Boolean by createWatcher(canAlwaysEat ?: false)
    public var fastFood: Boolean by createWatcher(fastFood ?: false)
    public var effects: java.util.ArrayList<Pair<MobEffectInstance, Float>> by createWatcher(effects ?: arrayListOf())

    public fun addEffect(effect: PotionEffect) {
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
