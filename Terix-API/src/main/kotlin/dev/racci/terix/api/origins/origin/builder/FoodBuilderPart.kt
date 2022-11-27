package dev.racci.terix.api.origins.origin.builder

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import com.destroystokyo.paper.MaterialSetTag
import dev.racci.minix.api.extensions.collections.findKProperty
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.dsl.DSLMutator
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.maybeAppend
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.origin.PlayerLambda
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.food.Foods
import org.bukkit.Material
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

// TODO -> Add a way to clear default potions.
@OptIn(ExperimentalTypeInference::class)
public class FoodBuilderPart internal constructor() : BuilderPart<Either<FoodBuilderPart.FoodPropertyElement, FoodBuilderPart.FoodActionElement>>() {

    @JvmName("exchangeFoodProperties")
    public fun exchangeFoodProperties(
        first: Material,
        second: Material
    ) {
        val firstProps = Foods.DEFAULT_PROPERTIES[first.key.key]
        val secondProps = Foods.DEFAULT_PROPERTIES[second.key.key]

        if (firstProps == null || secondProps == null) {
            throw IllegalArgumentException("One of the materials is not a food.")
        }

        fun toMutator(props: FoodProperties): DSLMutator<FoodPropertyBuilder> {
            return dslMutator {
                this.saturationModifier = props.saturationModifier
                this.nutrition = props.nutrition
                this.fastFood = props.isFastFood
                this.effects = ArrayList(props.effects.map { pair -> pair.first to pair.second })
                this.canAlwaysEat = props.canAlwaysEat()
                this.isMeat = props.isMeat
            }
        }

        FoodPropertyElement(
            first.left(),
            toMutator(secondProps)
        ).left().let(::addElement)

        FoodPropertyElement(
            second.left(),
            toMutator(firstProps)
        ).left().let(::addElement)
    }

    /** Modifies or Creates a Food Property. */
    @JvmName("modifyFoodSingle")
    public fun modifyFood(
        material: Material,
        builder: DSLMutator<FoodPropertyBuilder>
    ) { FoodPropertyElement(material.left(), builder).left().let(::addElement) }

    /** Creates a Food Property with a relation to an [ItemMatcher]. */
    @JvmName("modifyFoodSingle")
    public fun modifyFood(
        builder: DSLMutator<FoodPropertyBuilder>,
        matcher: ItemMatcher
    ) { FoodPropertyElement(matcher.right(), builder).left().let(::addElement) }

    /** Modifies or Creates a Food Property on each item of the collection. */
    @JvmName("modifyFoodIterable")
    public fun modifyFood(
        materials: Iterable<Material>,
        builder: DSLMutator<FoodPropertyBuilder>
    ): Unit = materials.forEach { modifyFood(it, builder) }

    @JvmName("plusFoodSingle")
    public fun plusFood(
        material: Material,
        value: Number
    ): Unit = modifyFood(
        material,
        dslMutator {
            nutrition += value.toInt()
            saturationModifier += value.toInt() / 10 / 2
        }
    )

    @JvmName("plusFoodIterable")
    public fun plusFood(
        materials: Iterable<Material>,
        value: Number
    ): Unit = materials.forEach { plusFood(it, value) }

    @JvmName("timesModifierSingle")
    public fun timesModifier(
        material: Material,
        value: Number
    ): Unit = modifyFood(
        material,
        dslMutator {
            nutrition *= value.toInt()
            saturationModifier *= value.toFloat()
        }
    )

    @JvmName("timeModifierIterable")
    public fun timesModifier(
        materials: Iterable<Material>,
        value: Number
    ): Unit = materials.forEach { timesModifier(it, value) }

    @Suppress("UnsatCallPre")
    @JvmName("divModifierSingle")
    public fun divModifier(
        material: Material,
        value: Number
    ): Unit = modifyFood(
        material,
        dslMutator {
            nutrition /= value.toInt()
            saturationModifier /= value.toFloat()
        }
    )

    @JvmName("divModifierIterable")
    public fun divModifier(
        materials: Iterable<Material>,
        value: Number
    ): Unit = materials.forEach { divModifier(it, value) }

    @JvmName("potionEffectSingle")
    public fun potionEffect(
        material: Material,
        builder: DSLMutator<PotionEffectBuilder>
    ): Unit = modifyFood(
        material,
        dslMutator { addEffect(builder.asNew().get()) }
    )

    @JvmName("potionEffectIterable")
    public fun potionEffect(
        materials: Iterable<Material>,
        builder: DSLMutator<PotionEffectBuilder>
    ): Unit = materials.forEach { potionEffect(it, builder) }

    @JvmName("attributeModifierSingle")
    public fun attributeModifier(
        material: Material,
        builder: DSLMutator<TimedAttributeBuilder>
    ): Unit = FoodActionElement(material, builder.right()).right().let(::addElement)

    @JvmName("attributeModifierIterable")
    public fun attributeModifier(
        materials: Iterable<Material>,
        builder: DSLMutator<TimedAttributeBuilder>
    ): Unit = materials.forEach { attributeModifier(it, builder) }

    @JvmName("actionModifierSingle")
    public fun actionModifier(
        material: Material,
        action: PlayerLambda
    ): Unit = FoodActionElement(
        material,
        action.left()
    ).right().let(::addElement)

    @JvmName("actionModifierIterable")
    public fun actionModifier(
        materials: Iterable<Material>,
        action: PlayerLambda
    ): Unit = materials.forEach { actionModifier(it, action) }

    @JvmName("plusAssignMaterial")
    @OverloadResolutionByLambdaReturnType
    public operator fun Material.plusAssign(builder: DSLMutator<FoodPropertyBuilder>): Unit = modifyFood(this, builder)

    @JvmName("timesAssignMaterial")
    public operator fun Material.timesAssign(number: Number): Unit = timesModifier(this, number)

    @JvmName("divAssignMaterial")
    public operator fun Material.divAssign(number: Number): Unit = divModifier(this, number)

    @JvmName("potionEffectMaterial")
    @OverloadResolutionByLambdaReturnType
    public operator fun Material.plusAssign(builder: DSLMutator<PotionEffectBuilder>): Unit = potionEffect(this, builder)

    @JvmName("attributeModifierMaterial")
    @OverloadResolutionByLambdaReturnType
    public operator fun Material.plusAssign(builder: DSLMutator<TimedAttributeBuilder>): Unit = attributeModifier(this, builder)

    @JvmName("actionModifierMaterial")
    @OverloadResolutionByLambdaReturnType
    public operator fun Material.plusAssign(builder: PlayerLambda): Unit = actionModifier(this, builder)

    @JvmName("plusAssignMaterialIterable")
    @OverloadResolutionByLambdaReturnType
    public operator fun Iterable<Material>.plusAssign(builder: DSLMutator<FoodPropertyBuilder>): Unit = modifyFood(this, builder)

    @JvmName("timesAssignMaterialIterable")
    public operator fun Iterable<Material>.timesAssign(number: Number): Unit = timesModifier(this, number)

    @JvmName("divAssignMaterialIterable")
    public operator fun Iterable<Material>.divAssign(number: Number): Unit = divModifier(this, number)

    @JvmName("potionEffectMaterialIterable")
    @OverloadResolutionByLambdaReturnType
    public operator fun Iterable<Material>.plusAssign(builder: DSLMutator<PotionEffectBuilder>): Unit = potionEffect(this, builder)

    @JvmName("attributeModifierMaterialIterable")
    @OverloadResolutionByLambdaReturnType
    public operator fun Iterable<Material>.plusAssign(builder: DSLMutator<TimedAttributeBuilder>): Unit = attributeModifier(this, builder)

    @JvmName("actionModifierMaterialIterable")
    @OverloadResolutionByLambdaReturnType
    public operator fun Iterable<Material>.plusAssign(builder: PlayerLambda): Unit = actionModifier(this, builder)

    @JvmName("plusAssignMaterialSetTag")
    @OverloadResolutionByLambdaReturnType
    public operator fun MaterialSetTag.plusAssign(builder: DSLMutator<FoodPropertyBuilder>): Unit = modifyFood(this.values, builder)

    @JvmName("timesAssignMaterialSetTag")
    public operator fun MaterialSetTag.timesAssign(number: Number): Unit = timesModifier(this.values, number)

    @JvmName("divAssignMaterialSetTag")
    public operator fun MaterialSetTag.divAssign(number: Number): Unit = divModifier(this.values, number)

    @JvmName("potionEffectMaterialSetTag")
    @OverloadResolutionByLambdaReturnType
    public operator fun MaterialSetTag.plusAssign(builder: DSLMutator<PotionEffectBuilder>): Unit = potionEffect(this.values, builder)

    @JvmName("attributeModifierMaterialSetTag")
    @OverloadResolutionByLambdaReturnType
    public operator fun MaterialSetTag.plusAssign(builder: DSLMutator<TimedAttributeBuilder>): Unit = attributeModifier(this.values, builder)

    @JvmName("actionModifierMaterialSetTag")
    @OverloadResolutionByLambdaReturnType
    public operator fun MaterialSetTag.plusAssign(builder: PlayerLambda): Unit = actionModifier(this.values, builder)

    @OverloadResolutionByLambdaReturnType
    @JvmName("plusAssignMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.plusAssign(builder: DSLMutator<FoodPropertyBuilder>): Unit = modifyFood(this.flatMap { it.values }, builder)

    @JvmName("timesAssignMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.timesAssign(number: Number): Unit = timesModifier(this.flatMap { it.values }, number)

    @JvmName("divAssignMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.divAssign(number: Number): Unit = divModifier(this.flatMap { it.values }, number)

    @OverloadResolutionByLambdaReturnType
    @JvmName("potionEffectMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.plusAssign(builder: DSLMutator<PotionEffectBuilder>): Unit = potionEffect(this.flatMap { it.values }, builder)

    @OverloadResolutionByLambdaReturnType
    @JvmName("attributeModifierMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.plusAssign(builder: DSLMutator<TimedAttributeBuilder>): Unit = attributeModifier(this.flatMap { it.values }, builder)

    @OverloadResolutionByLambdaReturnType
    @JvmName("actionModifierMaterialSetTagIterable")
    public operator fun Iterable<MaterialSetTag>.plusAssign(builder: PlayerLambda): Unit = actionModifier(this.flatMap { it.values }, builder)

    @JvmName("plusMaterialIterable")
    public operator fun Iterable<Material>.plus(number: Number): Unit = plusFood(this, number)

    @JvmName("plusMaterialSingle")
    public operator fun Material.plus(number: Number): Unit = plusFood(this, number)

    public fun ItemMatcher.foodProperty(builder: FoodPropertyBuilder.() -> Unit): Unit = modifyFood(dslMutator(builder), this)

    // TODO -> Cleanup and prevent large nesting
    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        val mutableActions = originValues.foodData.materialActions.builder()
        val mutableProperties = originValues.foodData.materialProperties.builder()
        val mutableMatcherProperties = originValues.foodData.matcherProperties.builder()

        for (value in super.getElements()) {
            value.fold(
                ifLeft = { element ->
                    fun FoodProperties?.mutate(either: Either<Material, ItemMatcher>): FoodProperties {
                        val builder = FoodPropertyBuilder(this)
                        element.mutator.on(builder)
                        // TODO -> Filter default potions
                        builder.effects.replaceAll { (effect, float) ->
                            MobEffectInstance(
                                effect.effect,
                                effect.duration,
                                effect.amplifier,
                                effect.isAmbient,
                                effect.isVisible,
                                effect.showIcon(),
                                effect::class.memberProperties.findKProperty<MobEffectInstance?>("hiddenEffect")
                                    .tap { it.isAccessible = true }
                                    .map { it.get(effect) }.orNull(),
                                effect.factorData,
                                OriginNamespacedTag.baseFoodOf(originValues, either).bukkitKey
                            ) to float
                        }
                        return builder.get()
                    }

                    element.key.fold(
                        ifLeft = { material ->
                            val existingProperty = mutableProperties[material] ?: Foods.ALL_PROPERTIES[material.key.key]
                            mutableProperties[material] = existingProperty.mutate(material.left())
                        },
                        ifRight = { matcher ->
                            mutableMatcherProperties[matcher] = null.mutate(matcher.right())
                        }
                    )
                },
                ifRight = { element ->
                    element.action.fold(
                        ifLeft = { action ->
                            mutableActions.compute(element.material) { _, existing -> existing.maybeAppend(action) }
                        },
                        ifRight = { builder ->
                            val modifier = builder.asNew().materialName(element.material, originValues)
                            mutableActions.compute(element.material) { _, existing -> existing.maybeAppend { player -> modifier(player) } }
                        }
                    )
                }
            )
        }

        originValues.foodData = originValues.foodData.copy(
            matcherProperties = mutableMatcherProperties.build(),
            materialProperties = mutableProperties.build(),
            materialActions = mutableActions.build()
        )

        return None
    }

    public data class FoodPropertyElement internal constructor(
        public val key: Either<Material, ItemMatcher>,
        public val mutator: DSLMutator<FoodPropertyBuilder>
    )

    public data class FoodActionElement internal constructor(
        public val material: Material,
        public val action: Either<PlayerLambda, DSLMutator<TimedAttributeBuilder>>
    )
}
