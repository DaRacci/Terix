package dev.racci.terix.api.origins.origin

import arrow.analysis.unsafeCall
import arrow.core.Either
import com.destroystokyo.paper.MaterialSetTag
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.DSLMutator
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.abilities.passive.PassiveAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.states.State
import net.minecraft.world.food.Foods
import org.apiguardian.api.API
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import java.time.Duration
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass

/** Handles the origins primary variables. */
@API(status = API.Status.STABLE, since = "1.0.0")
public sealed class OriginBuilder : OriginValues() {

    private val builderCache: LoadingCache<KClass<*>, Any> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(15)) // Unneeded after the origin's registered & built.
        .build { kClass -> unsafeCall(kClass.constructors.first()).call(this) }

    private inline fun <reified T : Any> builder(): T = builderCache[T::class].castOrThrow()

    public fun fireImmunity(fireImmunity: Boolean = true): OriginBuilder {
        this.fireImmunity = fireImmunity
        return this
    }

    public fun waterBreathing(waterBreathing: Boolean = true): OriginBuilder {
        this.waterBreathing = waterBreathing
        return this
    }

    @MinixDsl
    public suspend fun potions(builder: suspend PotionBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    public suspend fun attributes(builder: suspend AttributeBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    public suspend fun title(builder: suspend TimeTitleBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    public suspend fun damage(builder: suspend DamageBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    public suspend fun food(builder: suspend FoodBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    public suspend fun item(builder: suspend OriginItem.() -> Unit) {
        builder(item)
    }

    @MinixDsl
    public suspend fun abilities(builder: suspend AbilityBuilder.() -> Unit) {
        builder(builder())
    }

    /** A Utility class for building potion modifiers. */
    public inner class PotionBuilder {

        /**
         * Adds a potion to the player while this trigger is active.
         *
         * ### Note: The potion key will always be overwritten.
         */
        public operator fun State.plusAssign(mutator: DSLMutator<PotionEffectBuilder>) {
            statePotions.put(this, mutator.asNew().originKey(this@OriginBuilder, this).get())
        }

        public operator fun Collection<State>.plusAssign(builder: DSLMutator<PotionEffectBuilder>): Unit = this.forEach { it += builder }
    }

    /** A Utility class for building attribute modifiers. */
    public inner class AttributeBuilder {

        /**
         * Removes this number from the players' base attributes.
         *
         * @param value The amount to remove.
         * @receiver The attribute to remove from.
         */
        public operator fun Attribute.minusAssign(value: Number): Unit = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, State.CONSTANT)

        /**
         * Adds this number to the players' base attributes.
         *
         * @param value The amount to add.
         * @receiver The attribute to add to.
         */
        public operator fun Attribute.plusAssign(value: Number): Unit = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, State.CONSTANT)

        /**
         * Multiplies the players' base attribute by this number.
         *
         * @param value The amount to multiply by.
         * @receiver The attribute to multiply.
         */
        public operator fun Attribute.timesAssign(value: Number): Unit = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value.toDouble() - 1, State.CONSTANT)

        /**
         * Divides the players base attribute by this number.
         *
         * @param value The amount to divide by.
         * @receiver The attribute to divide.
         */
        public operator fun Attribute.divAssign(value: Number): Unit = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, (1.0 / value.toDouble()) - 1, State.CONSTANT)

        /**
         * Removes this number from the players' attribute when this trigger is active.
         *
         * @param value The amount to remove.
         * @receiver The Trigger and Attribute to remove from.
         */
        public operator fun Pair<State, Attribute>.minusAssign(value: Number): Unit = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Adds this number to the players' attribute when this trigger is active.
         *
         * @param value The amount to add.
         * @receiver The Trigger and Attribute to add to.
         */
        public operator fun Pair<State, Attribute>.plusAssign(value: Number): Unit = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Multiplies the players attribute by this number when this trigger is active.
         *
         * @param value The amount to multiply by.
         * @receiver The Trigger and Attribute to multiply.
         */
        public operator fun Pair<State, Attribute>.timesAssign(value: Number): Unit = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value.toDouble() - 1, this.first)

        /**
         * Divides the players attribute by this number when this trigger is active.
         *
         * @param value The amount to divide by.
         * @receiver The Trigger and Attribute to divide.
         */
        public operator fun Pair<State, Attribute>.divAssign(value: Number): Unit = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0 / value.toDouble(), this.first)

        private fun addAttribute(
            attribute: Attribute,
            operation: AttributeModifier.Operation,
            amount: Number,
            state: State
        ) {
            this@OriginBuilder.attributeModifiers.put(
                state,
                attribute to dslMutator<AttributeModifierBuilder> {
                    originName(this@OriginBuilder, state)
                    this.attribute = attribute
                    this.operation = operation
                    this.amount = amount.toDouble()
                }.asNew().get()
            )
        }
    }

    /** A Utility class for building time-based stateTitles. */
    public inner class TimeTitleBuilder {

        /**
         * Displays this title to the player when then given trigger is activated.
         *
         * @param builder The title builder to use.
         * @receiver The trigger to activate the title.
         */
        public operator fun State.plusAssign(builder: TitleBuilder.() -> Unit) {
            val title = TitleBuilder()
            builder(title)
            this@OriginBuilder.stateTitles[this] = title
        }

        // TODO: Title on deactivation of trigger.
    }

    /** A Utility class for building damage triggers. */
    public inner class DamageBuilder {

        /**
         * Triggers this lambda when the player takes damage and this Trigger is
         * active.
         *
         * @param builder The damage builder to use.
         * @receiver The trigger to activate the damage.
         */
        public operator fun State.plusAssign(builder: suspend (Player) -> Unit) {
            stateBlocks[this] = builder
        }

        /**
         * Deals the amount of damage to the player when the given trigger is
         * activated.
         *
         * @param number The amount of damage to deal.
         * @receiver The trigger to activate the damage.
         */
        public operator fun State.plusAssign(number: Number) {
            stateDamageTicks[this] = number.toDouble()
        }

        /**
         * Adds this amount of damage to the player when the player's damage cause
         * is this.
         *
         * @param number The amount of damage to add.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.plusAssign(number: Number) {
            damageActions[this] = { this.damage += number.toDouble() }
        }

        /**
         * Minuses this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to minus.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.minusAssign(number: Number) {
            damageActions[this] = { this.damage -= number.toDouble() }
        }

        /**
         * Multiplies this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to multiply.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.timesAssign(number: Number) {
            damageActions[this] = { this.damage *= number.toDouble() }
        }

        /**
         * Divides this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to divide.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.divAssign(number: Number) {
            damageActions[this] = { this.damage /= number.toDouble() }
        }

        /**
         * Runs this lambda async when the player takes damage from these causes.
         *
         * @param block The lambda to run.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.plusAssign(block: suspend (EntityDamageEvent) -> Unit) {
            damageActions[this] = block
        }

        /**
         * Adds all elements for [State.plusAssign]
         *
         * @param builder The damage builder to use.
         * @receiver The triggers that activate the damage.
         */
        @JvmName("plusAssignTrigger")
        public operator fun Collection<State>.plusAssign(builder: suspend (Player) -> Unit): Unit = forEach { it += builder }

        /**
         * Adds all elements to [State.plusAssign]
         *
         * @param number The amount of damage to deal.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<State>.plusAssign(number: Number): Unit = forEach { it += number }

        /** Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]. */
        public operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(block: suspend (EntityDamageEvent) -> Unit): Unit = forEach { it += block }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
         * * @param number The amount of damage to deal.
         *
         * @receiver The causes that are affected.
         */
        @JvmName("plusAssignCause")
        public operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(number: Number): Unit = forEach { it += number }

        /** Adds all elements to [EntityDamageEvent.DamageCause.minusAssign]. */
        public operator fun Collection<EntityDamageEvent.DamageCause>.minusAssign(number: Number): Unit = forEach { it -= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.timesAssign]
         *
         * @param number The amount of damage to multiply.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<EntityDamageEvent.DamageCause>.timesAssign(number: Number): Unit = forEach { it *= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.divAssign]
         *
         * @param number The amount of damage to divide.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<EntityDamageEvent.DamageCause>.divAssign(number: Number): Unit = forEach { it /= number }
    }

    // TODO -> Add a way to clear default potions.
    /** A Utility class for building food triggers. */
    @OptIn(ExperimentalTypeInference::class)
    public inner class FoodBuilder {

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

            customFoodProperties[first] = secondProps
            customFoodProperties[second] = firstProps
        }

        /** Modifies or Creates a Food Property. */
        @JvmName("modifyFoodSingle")
        public fun modifyFood(
            material: Material,
            builder: DSLMutator<FoodPropertyBuilder>
        ) {
            val propBuilder = FoodPropertyBuilder(customFoodProperties[material] ?: Foods.DEFAULT_PROPERTIES[material.key.key])
            customFoodProperties[material] = builder.on(propBuilder).get()
        }

        /** Creates a Food Property with a relation to an [ItemMatcher]. */
        @JvmName("modifyFoodSingle")
        public fun modifyFood(
            builder: DSLMutator<FoodPropertyBuilder>,
            matcher: ItemMatcher
        ) {
            val propBuilder = FoodPropertyBuilder(customMatcherFoodProperties[matcher])
            customMatcherFoodProperties[matcher] = builder.on(propBuilder).get()
        }

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
            dslMutator {
                addEffect(builder.asNew().foodKey(this@OriginBuilder.name, material.name).get())
            }
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
        ): Unit = customFoodActions.put(material, Either.Right(builder.asNew().materialName(material, this@OriginBuilder)))

        @JvmName("attributeModifierIterable")
        public fun attributeModifier(
            materials: Iterable<Material>,
            builder: DSLMutator<TimedAttributeBuilder>
        ): Unit = materials.forEach { attributeModifier(it, builder) }

        @JvmName("actionModifierSingle")
        public fun actionModifier(
            material: Material,
            action: ActionPropBuilder
        ): Unit = customFoodActions.put(material, Either.Left(action))

        @JvmName("actionModifierIterable")
        public fun actionModifier(
            materials: Iterable<Material>,
            action: ActionPropBuilder
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
        public operator fun Material.plusAssign(builder: ActionPropBuilder): Unit = actionModifier(this, builder)

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
        public operator fun Iterable<Material>.plusAssign(builder: ActionPropBuilder): Unit = actionModifier(this, builder)

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
        public operator fun MaterialSetTag.plusAssign(builder: ActionPropBuilder): Unit = actionModifier(this.values, builder)

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
        public operator fun Iterable<MaterialSetTag>.plusAssign(builder: ActionPropBuilder): Unit = actionModifier(this.flatMap { it.values }, builder)

        @JvmName("plusMaterialIterable")
        public operator fun Iterable<Material>.plus(number: Number): Unit = plusFood(this, number)

        @JvmName("plusMaterialSingle")
        public operator fun Material.plus(number: Number): Unit = plusFood(this, number)

        public fun ItemMatcher.foodProperty(builder: FoodPropertyBuilder.() -> Unit): Unit = modifyFood(dslMutator(builder), this)
    }

    /** A Utility class for building abilities. */
    public inner class AbilityBuilder {

        public fun <T : KeybindAbility> KeyBinding.add(
            clazz: KClass<out T>,
            builder: T.() -> Unit = {}
        ): KeybindAbility? = abilities.put(this, OriginService.generateAbility(clazz, this@OriginBuilder).also(builder.castOrThrow()))

        public inline fun <reified T : KeybindAbility> KeyBinding.add(
            noinline builder: T.() -> Unit = {}
        ): KeybindAbility? = add(T::class, builder)

        /**
         * Adds a passive ability that is granted with this origin.
         *
         * @param T The type of ability to add.
         * @param configure A builder function to configure the ability on creation.
         */
        public inline fun <reified T : PassiveAbility> withPassive(
            noinline configure: suspend T.() -> Unit = {}
        ) { passiveAbilities.add(PassiveAbilityGenerator(T::class, configure.castOrThrow())) }
    }

    /** A Utility class for building biome triggers. */
    public inner class BiomeBuilder {

        public operator fun <T : KeybindAbility> Biome.plusAssign(ability: KClass<out T>) {
            TODO("Not implemented yet")
        }
    }
}

public typealias ActionPropBuilder = suspend (Player) -> Unit
