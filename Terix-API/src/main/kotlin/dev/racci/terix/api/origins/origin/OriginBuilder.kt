package dev.racci.terix.api.origins.origin

import com.destroystokyo.paper.MaterialSetTag
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import net.minecraft.world.food.Foods
import org.apiguardian.api.API
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.block.Biome
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import java.time.Duration
import kotlin.reflect.KClass

/** Handles the origins primary variables. */
@API(status = API.Status.STABLE, since = "1.0.0")
sealed class OriginBuilder : OriginValues() {

    private val builderCache: LoadingCache<KClass<*>, Any> = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(15)) // Unneeded after the origin's registered & built.
        .build { kClass -> kClass.constructors.first().call(this) }

    private inline fun <reified T> builder(): T = builderCache[T::class].unsafeCast()

    fun nightvision(nightVision: Boolean = true): OriginBuilder {
        this.nightVision = nightVision
        return this
    }

    fun fireImmunity(fireImmunity: Boolean = true): OriginBuilder {
        this.fireImmunity = fireImmunity
        return this
    }

    fun waterBreathing(waterBreathing: Boolean = true): OriginBuilder {
        this.waterBreathing = waterBreathing
        return this
    }

    @MinixDsl
    suspend fun potions(builder: suspend PotionBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    suspend fun attributes(builder: suspend AttributeBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    suspend fun title(builder: suspend TimeTitleBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    suspend fun damage(builder: suspend DamageBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    suspend fun food(builder: suspend FoodBuilder.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    suspend fun item(builder: suspend OriginItem.() -> Unit) {
        builder(item)
    }

    @MinixDsl
    suspend fun abilities(builder: suspend AbilityBuilder.() -> Unit) {
        builder(builder())
    }

    /** A Utility class for building potion modifiers. */
    inner class PotionBuilder {

        /**
         * Adds a potion to the player while this trigger is active.
         *
         * ### Note: The potion key will always be overwritten.
         */
        operator fun Trigger.plusAssign(builder: PotionEffectBuilder.() -> Unit) {
            val pot = PotionEffectBuilder(builder).originKey(this@OriginBuilder, this)
            potions.put(this, pot.build())
        }
    }

    /** A Utility class for building attribute modifiers. */
    inner class AttributeBuilder {

        /**
         * Removes this number from the players base attributes.
         *
         * @param value The amount to remove.
         * @receiver The attribute to remove from.
         */
        operator fun Attribute.minusAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, Trigger.ON)

        /**
         * Adds this number to the players base attributes.
         *
         * @param value The amount to add.
         * @receiver The attribute to add to.
         */
        operator fun Attribute.plusAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.ADD_NUMBER, value, Trigger.ON)

        /**
         * Multiplies the players base attribute by this number.
         *
         * @param value The amount to multiply by.
         * @receiver The attribute to multiply.
         */
        operator fun Attribute.timesAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value.toDouble() - 1, Trigger.ON)

        /**
         * Divides the players base attribute by this number.
         *
         * @param value The amount to divide by.
         * @receiver The attribute to divide.
         */
        operator fun Attribute.divAssign(value: Number) = addAttribute(this, AttributeModifier.Operation.MULTIPLY_SCALAR_1, (1.0 / value.toDouble()) - 1, Trigger.ON)

        /**
         * Removes this number from the players attribute when this trigger is
         * active.
         *
         * @param value The amount to remove.
         * @receiver The Trigger and Attribute to remove from.
         */
        operator fun Pair<Trigger, Attribute>.minusAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Adds this number to the players attribute when this trigger is active.
         *
         * @param value The amount to add.
         * @receiver The Trigger and Attribute to add to.
         */
        operator fun Pair<Trigger, Attribute>.plusAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.ADD_NUMBER, value, this.first)

        /**
         * Multiplies the players attribute by this number when this trigger is
         * active.
         *
         * @param value The amount to multiply by.
         * @receiver The Trigger and Attribute to multiply.
         */
        operator fun Pair<Trigger, Attribute>.timesAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, value, this.first)

        /**
         * Divides the players attribute by this number when this trigger is
         * active.
         *
         * @param value The amount to divide by.
         * @receiver The Trigger and Attribute to divide.
         */
        operator fun Pair<Trigger, Attribute>.divAssign(value: Number) = addAttribute(this.second, AttributeModifier.Operation.MULTIPLY_SCALAR_1, 1.0 / value.toDouble(), this.first)

        private fun addAttribute(
            attribute: Attribute,
            operation: AttributeModifier.Operation,
            amount: Number,
            trigger: Trigger
        ) {
            this@OriginBuilder.attributeModifiers.put(
                trigger,
                attribute to AttributeModifierBuilder {
                    originName(this@OriginBuilder, trigger)
                    this.attribute = attribute
                    this.operation = operation
                    this.amount = amount
                }.build()
            )
        }
    }

    /** A Utility class for building time based titles. */
    inner class TimeTitleBuilder {

        /**
         * Displays this title to the player when then given trigger is activated.
         *
         * @param builder The title builder to use.
         * @receiver The trigger to activate the title.
         */
        operator fun Trigger.plusAssign(builder: TitleBuilder.() -> Unit) {
            val title = TitleBuilder()
            builder(title)
            this@OriginBuilder.titles[this] = title
        }

        // TODO: Title on deactivation of trigger.
    }

    /** A Utility class for building damage triggers. */
    inner class DamageBuilder {

        /**
         * Triggers this lambda when the player takes damage and this Trigger is
         * active.
         *
         * @param builder The damage builder to use.
         * @receiver The trigger to activate the damage.
         */
        operator fun Trigger.plusAssign(builder: suspend (Player) -> Unit) {
            triggerBlocks[this] = builder
        }

        /**
         * Deals the number of damage to the player when the given trigger is
         * activated.
         *
         * @param number The amount of damage to deal.
         * @receiver The trigger to activate the damage.
         */
        operator fun Trigger.plusAssign(number: Number) {
            damageTicks[this] = number.toDouble()
        }

        /**
         * Adds this amount of damage to the player when the player's damage cause
         * is this.
         *
         * @param number The amount of damage to add.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.plusAssign(number: Number) {
            damageActions[this] = { this.damage += number.toDouble() }
        }

        /**
         * Minuses this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to minus.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.minusAssign(number: Number) {
            damageActions[this] = { this.damage -= number.toDouble() }
        }

        /**
         * Multiplies this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to multiply.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.timesAssign(number: Number) {
            damageActions[this] = { this.damage *= number.toDouble() }
        }

        /**
         * Divides this amount of damage to the player when the player's damage
         * cause is this.
         *
         * @param number The amount of damage to divide.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.divAssign(number: Number) {
            damageActions[this] = { this.damage /= number.toDouble() }
        }

        /**
         * Runs this lambda async when the player takes damage from this causes.
         *
         * @param block The lambda to run.
         * @receiver The damage cause that is affected.
         */
        operator fun EntityDamageEvent.DamageCause.plusAssign(block: suspend (EntityDamageEvent) -> Unit) {
            damageActions[this] = block
        }

        /**
         * Adds all elements for [Trigger.plusAssign]
         *
         * @param builder The damage builder to use.
         * @receiver The triggers that activate the damage.
         */
        @JvmName("plusAssignTrigger")
        operator fun Collection<Trigger>.plusAssign(builder: suspend (Player) -> Unit) = forEach { it += builder }

        /**
         * Adds all elements to [Trigger.plusAssign]
         *
         * @param number The amount of damage to deal.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<Trigger>.plusAssign(number: Number) = forEach { it += number }

        /** Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]. */
        operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(block: suspend (EntityDamageEvent) -> Unit) = forEach { it += block }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.plusAssign]
         * * @param number The amount of damage to deal.
         *
         * @receiver The causes that are affected.
         */
        @JvmName("plusAssignCause")
        operator fun Collection<EntityDamageEvent.DamageCause>.plusAssign(number: Number) = forEach { it += number }

        /** Adds all elements to [EntityDamageEvent.DamageCause.minusAssign]. */
        operator fun Collection<EntityDamageEvent.DamageCause>.minusAssign(number: Number) = forEach { it -= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.timesAssign]
         *
         * @param number The amount of damage to multiply.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.timesAssign(number: Number) = forEach { it *= number }

        /**
         * Adds all elements to [EntityDamageEvent.DamageCause.divAssign]
         *
         * @param number The amount of damage to divide.
         * @receiver The triggers that activate the damage.
         */
        operator fun Collection<EntityDamageEvent.DamageCause>.divAssign(number: Number) = forEach { it /= number }
    }

    /** A Utility class for building food triggers. */
    inner class FoodBuilder {

        /** Modifies or Creates a Food Property. */
        @JvmName("modifyFoodSingle")
        fun modifyFood(
            material: Material,
            builder: FoodPropBuilder
        ) {
            val foodProps = Foods.DEFAULT_PROPERTIES[material.key.key]
            val foodProp = FoodPropertyBuilder(foodProps)

            builder(foodProp)
            customFoodProperties[material] = foodProp.build()
        }

        /** Modifies or Creates a Food Property on each item of the collection. */
        @JvmName("modifyFoodIterable")
        fun modifyFood(
            materials: Iterable<Material>,
            builder: FoodPropBuilder
        ): Unit = materials.forEach { modifyFood(it, builder) }

        @JvmName("timesModifierSingle")
        fun timesModifier(
            material: Material,
            value: Number
        ): Unit = modifyFood(material) {
            it.nutrition *= value.toInt()
            it.saturationModifier *= value.toFloat()
        }

        @JvmName("timeModifierIterable")
        fun timesModifier(
            materials: Iterable<Material>,
            value: Number
        ): Unit = materials.forEach { timesModifier(it, value) }

        @JvmName("divModifierSingle")
        fun divModifier(
            material: Material,
            value: Number
        ): Unit = modifyFood(material) {
            it.nutrition /= value.toInt()
            it.saturationModifier /= value.toFloat()
        }

        @JvmName("divModifierIterable")
        fun divModifier(
            materials: Iterable<Material>,
            value: Number
        ): Unit = materials.forEach { divModifier(it, value) }

        @JvmName("potionEffectSingle")
        fun potionEffect(
            material: Material,
            builder: PotionPropBuilder
        ): Unit = modifyFood(material) {
            it.addEffect {
                builder(this)
                foodKey(material)
            }
        }

        @JvmName("potionEffectIterable")
        fun potionEffect(
            materials: Iterable<Material>,
            builder: PotionPropBuilder
        ): Unit = materials.forEach { potionEffect(it, builder) }

        @JvmName("attributeModifierSingle")
        fun attributeModifier(
            material: Material,
            builder: AttributePropBuilder
        ): Unit = foodAttributes.put(material, TimedAttributeBuilder(builder).materialName(material, this@OriginBuilder))

        @JvmName("actionModifierSingle")
        fun actionModifier(
            material: Material,
            action: ActionPropBuilder
        ) {
            foodBlocks[material] = action
        }

        @JvmName("actionModifierIterable")
        fun actionModifier(
            materials: Iterable<Material>,
            action: ActionPropBuilder
        ) {
            materials.forEach { actionModifier(it, action) }
        }

        @JvmName("attributeModifierIterable")
        fun attributeModifier(
            materials: Iterable<Material>,
            builder: AttributePropBuilder
        ): Unit = materials.forEach { attributeModifier(it, builder) }

        @JvmName("plusAssignMaterial")
        operator fun Material.plusAssign(builder: FoodPropBuilder) = modifyFood(this, builder)

        @JvmName("timesAssignMaterial")
        operator fun Material.timesAssign(number: Number) = timesModifier(this, number)

        @JvmName("divAssignMaterial")
        operator fun Material.divAssign(number: Number) = divModifier(this, number)

        @JvmName("potionEffectMaterial")
        operator fun Material.plusAssign(builder: PotionPropBuilder) = potionEffect(this, builder)

        @JvmName("attributeModifierMaterial")
        operator fun Material.plusAssign(builder: AttributePropBuilder) = attributeModifier(this, builder)

        @JvmName("actionModifierMaterial")
        operator fun Material.plusAssign(builder: ActionPropBuilder) = actionModifier(this, builder)

        @JvmName("plusAssignMaterialIterable")
        operator fun Iterable<Material>.plusAssign(builder: FoodPropBuilder) = modifyFood(this, builder)

        @JvmName("timesAssignMaterialIterable")
        operator fun Iterable<Material>.timesAssign(number: Number) = timesModifier(this, number)

        @JvmName("divAssignMaterialIterable")
        operator fun Iterable<Material>.divAssign(number: Number) = divModifier(this, number)

        @JvmName("potionEffectMaterialIterable")
        operator fun Iterable<Material>.plusAssign(builder: PotionPropBuilder) = potionEffect(this, builder)

        @JvmName("attributeModifierMaterialIterable")
        operator fun Iterable<Material>.plusAssign(builder: AttributePropBuilder) = attributeModifier(this, builder)

        @JvmName("actionModifierMaterialIterable")
        operator fun Iterable<Material>.plusAssign(builder: ActionPropBuilder) = actionModifier(this, builder)

        @JvmName("plusAssignMaterialSetTag")
        operator fun MaterialSetTag.plusAssign(builder: FoodPropBuilder) = modifyFood(this.values, builder)

        @JvmName("timesAssignMaterialSetTag")
        operator fun MaterialSetTag.timesAssign(number: Number) = timesModifier(this.values, number)

        @JvmName("divAssignMaterialSetTag")
        operator fun MaterialSetTag.divAssign(number: Number) = divModifier(this.values, number)

        @JvmName("potionEffectMaterialSetTag")
        operator fun MaterialSetTag.plusAssign(builder: PotionPropBuilder) = potionEffect(this.values, builder)

        @JvmName("attributeModifierMaterialSetTag")
        operator fun MaterialSetTag.plusAssign(builder: AttributePropBuilder) = attributeModifier(this.values, builder)

        @JvmName("actionModifierMaterialSetTag")
        operator fun MaterialSetTag.plusAssign(builder: ActionPropBuilder) = actionModifier(this.values, builder)

        @JvmName("plusAssignMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.plusAssign(builder: FoodPropBuilder) = modifyFood(this.flatMap { it.values }, builder)

        @JvmName("timesAssignMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.timesAssign(number: Number) = timesModifier(this.flatMap { it.values }, number)

        @JvmName("divAssignMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.divAssign(number: Number) = divModifier(this.flatMap { it.values }, number)

        @JvmName("potionEffectMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.plusAssign(builder: PotionPropBuilder) = potionEffect(this.flatMap { it.values }, builder)

        @JvmName("attributeModifierMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.plusAssign(builder: AttributePropBuilder) = attributeModifier(this.flatMap { it.values }, builder)

        @JvmName("actionModifierMaterialSetTagIterable")
        operator fun Iterable<MaterialSetTag>.plusAssign(builder: ActionPropBuilder) = actionModifier(this.flatMap { it.values }, builder)
    }

    /** A Utility class for building abilities. */
    inner class AbilityBuilder {

        fun <T : AbstractAbility> KeyBinding.add(clazz: KClass<out T>) = abilities.put(this, OriginService.getAbility(clazz))

        inline fun <reified T : AbstractAbility> KeyBinding.add() = add(T::class)
    }

    /** A Utility class for building biome triggers. */
    inner class BiomeBuilder {

        operator fun <T : AbstractAbility> Biome.plusAssign(ability: KClass<out T>) {
            TODO("Not implemented yet")
        }
    }
}

typealias FoodPropBuilder = (FoodPropertyBuilder) -> Unit
typealias PotionPropBuilder = (PotionEffectBuilder) -> Unit
typealias AttributePropBuilder = (TimedAttributeBuilder) -> Unit
typealias ActionPropBuilder = suspend (Player) -> Unit
