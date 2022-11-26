package dev.racci.terix.api.origins.origin

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.toOption
import arrow.optics.lens
import com.destroystokyo.paper.MaterialSetTag
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.extensions.collections.findKProperty
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.applyTag
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.DSLMutator
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.maybeAppend
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.abilities.passive.PassiveAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.toPersistentSet
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.food.Foods
import org.apiguardian.api.API
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/** Handles the origins primary variables. */
// TODO -> I don't like the current DSL style.
@API(status = API.Status.STABLE, since = "1.0.0")
public sealed class OriginBuilder : OriginValues() {

    @API(status = API.Status.INTERNAL)
    public val builderCache: LoadingCache<KClass<out BuilderPart<*>>, BuilderPart<*>> = Caffeine.newBuilder().build { kClass -> kClass.constructors.first().call() }

    private inline fun <reified T : BuilderPart<*>> builder(): T = builderCache[T::class].castOrThrow()

    public fun fireImmunity(fireImmunity: Boolean = true): OriginBuilder {
        this.fireImmunity = fireImmunity
        return this
    }

    public fun waterBreathing(waterBreathing: Boolean = true): OriginBuilder {
        this.waterBreathing = waterBreathing
        return this
    }

    @MinixDsl
    protected suspend fun potions(builder: suspend PotionBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun attributes(builder: suspend AttributeBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun title(builder: suspend TimeTitleBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun damage(builder: suspend DamageBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun food(builder: suspend FoodBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun item(builder: suspend OriginItem.() -> Unit) {
        builder(item)
    }

    @MinixDsl
    protected suspend fun abilities(builder: suspend AbilityBuilderPart.() -> Unit) {
        builder(builder())
    }

    public sealed class BuilderPart<T : Any> {
        private val heldElements: MutableList<T> = mutableListOf()

        @PublishedApi internal fun addElement(element: T) {
            heldElements.add(element)
        }

        protected fun getElements(): List<T> = heldElements.toList()

        @API(status = API.Status.INTERNAL)
        public abstract suspend fun insertInto(originValues: OriginValues): Option<Exception>
    }

    public class PotionBuilderPart internal constructor() : BuilderPart<PotionBuilderPart.PotionElement>() {

        /**
         * Adds a [PotionEffect] which will be granted to the player while this state is active.
         *
         * @receiver The [State] which will grant the potion.
         * @param mutator A [DSLMutator] which will be used to configure the [PotionEffect].
         */
        public operator fun State.plusAssign(mutator: DSLMutator<PotionEffectBuilder>) {
            PotionElement(
                this,
                mutator.asNew()
            ).also(::addElement)
        }

        /**
         * Adds a [PotionEffect] which will be granted to the player for these states.
         *
         * @receiver The Collection of [State]'s which will grant the potion.
         * @param mutator A [DSLMutator] which will be used to configure the [PotionEffect].
         */
        public operator fun Collection<State>.plusAssign(mutator: DSLMutator<PotionEffectBuilder>) {
            val builder = mutator.asNew()
            this.forEach { state ->
                PotionElement(
                    state,
                    builder
                ).also(::addElement)
            }
        }

        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            super.getElements().associate { it.state to it.builder }
                .onEach { (state, builder) -> builder.applyTag(OriginNamespacedTag.baseStateOf(originValues, state)) }
                .forEach { (state, builder) -> originValues.stateData.modify(state, OriginValues.StateData::potions) { potions -> potions.add(builder) } }

            return None
        }

        public data class PotionElement internal constructor(
            val state: State,
            val builder: PotionEffectBuilder
        )
    }

    public class AttributeBuilderPart internal constructor() : BuilderPart<AttributeBuilderPart.AttributeElement>() {

        /**
         * Removes this number from the players' base attributes.
         *
         * @param value The amount to remove.
         * @receiver The attribute to remove from.
         */
        public operator fun Attribute.minusAssign(value: Double): Unit = Pair(State.CONSTANT, this).minusAssign(value)

        /**
         * Adds this number to the players' base attributes.
         *
         * @param value The amount to add.
         * @receiver The attribute to add to.
         */
        public operator fun Attribute.plusAssign(value: Double): Unit = Pair(State.CONSTANT, this).plusAssign(value)

        /**
         * Multiplies the players' base attribute by this number.
         *
         * @param value The amount to multiply by.
         * @receiver The attribute to multiply.
         */
        public operator fun Attribute.timesAssign(value: Double): Unit = Pair(State.CONSTANT, this).timesAssign(value)

        /**
         * Divides the players base attribute by this number.
         *
         * @param value The amount to divide by.
         * @receiver The attribute to divide.
         */
        public operator fun Attribute.divAssign(value: Double): Unit = Pair(State.CONSTANT, this).divAssign(value)

        /**
         * Removes this number from the players' attribute when this trigger is active.
         *
         * @param value The amount to remove.
         * @receiver The Trigger and Attribute to remove from.
         */
        public operator fun Pair<State, Attribute>.minusAssign(value: Number): Unit = AttributeElement.of(this, value, AttributeModifier.Operation.ADD_NUMBER).let(::addElement)

        /**
         * Adds this number to the players' attribute when this trigger is active.
         *
         * @param value The amount to add.
         * @receiver The Trigger and Attribute to add to.
         */
        public operator fun Pair<State, Attribute>.plusAssign(value: Number): Unit = AttributeElement.of(this, value, AttributeModifier.Operation.ADD_NUMBER).let(::addElement)

        /**
         * Multiplies the players attribute by this number when this trigger is active.
         *
         * @param value The amount to multiply by.
         * @receiver The Trigger and Attribute to multiply.
         */
        public operator fun Pair<State, Attribute>.timesAssign(value: Double): Unit = AttributeElement.of(this, value - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1).let(::addElement)

        /**
         * Divides the players attribute by this number when this trigger is active.
         *
         * @param value The amount to divide by.
         * @receiver The Trigger and Attribute to divide.
         */
        public operator fun Pair<State, Attribute>.divAssign(value: Double): Unit = AttributeElement.of(this, 1.0 / value, AttributeModifier.Operation.MULTIPLY_SCALAR_1).let(::addElement)

        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            super.getElements().groupBy(AttributeElement::state).forEach { (state, elements) ->
                val modifiers = elements.map { element -> element.toBuilder(originValues) }.toPersistentSet()
                if (modifiers.isEmpty()) {
                    return@forEach
                } else originValues.stateData.modify(state, OriginValues.StateData::modifiers) { modifiers }
            }

            return None
        }

        public data class AttributeElement internal constructor(
            val state: State,
            val amount: Double,
            val attribute: Attribute,
            val operation: AttributeModifier.Operation
        ) {
            public fun toBuilder(
                originValues: OriginValues
            ): AttributeModifierBuilder = dslMutator<AttributeModifierBuilder> {
                this.attribute = attribute
                this.operation = operation
                this.amount = amount
                this.name = OriginNamespacedTag.baseStateOf(originValues, state).asString
            }.asNew()

            internal companion object {
                fun of(
                    pair: Pair<State, Attribute>,
                    amount: Number,
                    operation: AttributeModifier.Operation
                ) = AttributeElement(pair.first, amount.toDouble(), pair.second, operation)
            }
        }
    }

    /** A Utility class for building time-based stateTitles. */
    public class TimeTitleBuilderPart internal constructor() : BuilderPart<TimeTitleBuilderPart.TimeTitleElement>() {

        /**
         * Displays this title to the player when then given trigger is activated.
         *
         * @param builder The title builder to use.
         * @receiver The trigger to activate the title.
         */
        public operator fun State.plusAssign(builder: TitleBuilder.() -> Unit) {
            TimeTitleElement(
                this,
                dslMutator(builder)
            ).also(::addElement)
        }

        // TODO: Title on deactivation of trigger.

        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            super.getElements()
                .associate { it.state to it.builder }
                .forEach { (state, builder) -> originValues.stateData.modify(state, OriginValues.StateData::title) { title -> builder.mutateOrNew(title.orNull()).toOption() } }

            return None
        }

        public data class TimeTitleElement internal constructor(
            val state: State,
            val builder: DSLMutator<TitleBuilder>
        )
    }

    /** A Utility class for building damage triggers. */
    public class DamageBuilderPart internal constructor() : BuilderPart<Either<DamageBuilderPart.DamageActionElement, DamageBuilderPart.DamageTickElement>>() {

        /**
         * Deals the amount of damage to the player when the given trigger is
         * activated.
         *
         * @param value The amount of damage to deal.
         * @receiver The trigger to activate the damage.
         */
        public operator fun State.plusAssign(value: Double) {
            DamageTickElement(
                this,
                value
            ).right().let(::addElement)
        }

        /**
         * Calls this lambda when a damage event with the cause is called.
         *
         * @param value
         */
        public operator fun EntityDamageEvent.DamageCause.plusAssign(lambda: suspend (EntityDamageEvent) -> Unit) {
            DamageActionElement(
                this,
                lambda
            ).left().let(::addElement)
        }

        /**
         * Adds this amount of damage to the player when the player's damage cause
         * is this.
         *
         * @param value The amount of damage to add.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.plusAssign(value: Double) {
            DamageActionElement(
                this
            ) { this.damage += value }.left().let(::addElement)
        }

        /**
         * Minuses this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param value The amount of damage to minus.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.minusAssign(value: Double) {
            DamageActionElement(
                this
            ) { this.damage -= value }.left().let(::addElement)
        }

        /**
         * Multiplies this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param value The amount of damage to multiply.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.timesAssign(value: Double) {
            DamageActionElement(
                this
            ) { this.damage *= value }.left().let(::addElement)
        }

        /**
         * Divides this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param value The amount of damage to divide.
         * @receiver The damage cause that is affected.
         */
        public operator fun EntityDamageEvent.DamageCause.divAssign(value: Double) {
            DamageActionElement(
                this
            ) { this.damage /= value }.left().let(::addElement)
        }

        /**
         * Adds all elements to [State.plusAssign]
         *
         * @param value The amount of damage to deal.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<State>.plusAssign(value: Double): Unit = forEach { it += value }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
         *
         * @param value The amount of damage to deal.
         * @receiver The causes that are affected.
         */
        @JvmName("plusAssignEntityDamageEventDamageCause")
        public operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(value: Double): Unit = forEach { it += value }

        /** Adds all elements to [EntityDamageEvent.DamageCause.minusAssign]. */
        public operator fun Collection<EntityDamageEvent.DamageCause>.minusAssign(value: Double): Unit = forEach { it -= value }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.timesAssign]
         *
         * @param value The amount of damage to multiply.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<EntityDamageEvent.DamageCause>.timesAssign(value: Double): Unit = forEach { it *= value }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.divAssign]
         *
         * @param value The amount of damage to divide.
         * @receiver The triggers that activate the damage.
         */
        public operator fun Collection<EntityDamageEvent.DamageCause>.divAssign(value: Double): Unit = forEach { it /= value }

        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            val damageActions = originValues.damageActions.builder()

            super.getElements().forEach { either ->
                either.fold(
                    ifLeft = { (cause, action) ->
                        val existingLambda = damageActions[cause]
                        if (existingLambda != null) {
                            damageActions[cause] = {
                                existingLambda()
                                action()
                            }
                        } else damageActions[cause] = action
                    },
                    ifRight = { (state, amount) ->
                        originValues.stateData.modify(state, OriginValues.StateData::damage) { damage ->
                            damage.fold(
                                ifEmpty = { amount },
                                ifSome = { it + amount }
                            ).toOption()
                        }
                    }
                )
            }

            originValues.damageActions = damageActions.build()

            return None
        }

        public data class DamageActionElement internal constructor(
            val cause: EntityDamageEvent.DamageCause,
            val block: suspend EntityDamageEvent.() -> Unit
        )

        public data class DamageTickElement internal constructor(
            val state: State,
            val damage: Double
        )
    }

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
                        suspend fun FoodProperties?.mutate(either: Either<Material, ItemMatcher>): FoodProperties {
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

    /** A Utility class for building abilities. */
    public class AbilityBuilderPart internal constructor() : BuilderPart<AbilityGenerator<*>>() {

        /**
         * Adds a keybinding bound ability that is granted with this origin.
         *
         * @receiver The keybinding to bind the ability to.
         * @param A The type of ability to add.
         * @param configure A builder function to configure the ability on creation.
         */
        public inline fun <reified A : KeybindAbility> KeyBinding.add(
            vararg constructorParams: Pair<KProperty1<A, *>, *>,
            noinline configure: A.() -> Unit = {}
        ): Unit = newBuilder<A>()
            .keybinding(this)
            .configure(configure)
            .apply { constructorParams.forEach { (property, value) -> parameter(property, value) } }
            .build()

        /**
         * Adds a passive ability that is granted with this origin.
         *
         * @param A The type of ability to add.
         * @param configure A builder function to configure the ability on creation.
         */
        public inline fun <reified A : PassiveAbility> withPassive(
            vararg constructorParams: Pair<KProperty1<A, *>, *>,
            noinline configure: A.() -> Unit = {}
        ): Unit = newBuilder<A>()
            .configure(configure)
            .apply { constructorParams.forEach { (property, value) -> parameter(property, value) } }
            .build()

        /**
         * Creates a new ability builder.
         *
         * @param A The reified type of ability to add.
         */
        public inline fun <reified A : Ability> newBuilder(): AbilityBuilder<A> = AbilityBuilder(None, A::class)

        /**
         * Completes the ability builder and adds the ability to the origin.
         *
         * @receiver The ability builder to complete.
         */
        public fun AbilityBuilder<*>.build() {
            this.generator.copy().also(::addElement)
        }

        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            AbilityData::generators.lens.modify(originValues.abilityData) { generators ->
                generators.builder().also { set -> set.addAll(super.getElements()) }.build()
            }
            return None
        }
    }

    /** A Utility class for building biome triggers. */
    // TODO
    public class BiomeBuilder internal constructor() : BuilderPart<Nothing>() {
        override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
            TODO()
        }
    }
}

public typealias PlayerLambda = suspend (player: Player) -> Unit
