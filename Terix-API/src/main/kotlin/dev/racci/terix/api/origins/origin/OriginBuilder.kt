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

        @JvmName("plusAssignFoodPropertyBuilder")
        operator fun Material.plusAssign(foodProperties: FoodPropertyBuilder.() -> Unit) {
            customFoodProperties[this] = FoodPropertyBuilder(foodProperties).build()
        }

        fun Material.modifyFood(foodProperties: FoodPropertyBuilder.() -> Unit) {
            val foodProps = Foods.DEFAULT_PROPERTIES[this.key.key]
            val builder = FoodPropertyBuilder(foodProps)

            foodProperties(builder)
            customFoodProperties[this] = builder.build()
        }

        fun MaterialSetTag.modifyFood(foodProperties: (FoodPropertyBuilder) -> Unit) {
            values.forEach { it.modifyFood(foodProperties) }
        }

        @JvmName("plusAssignFoodPropertyBuilder")
        operator fun MaterialSetTag.plusAssign(foodProperties: (FoodPropertyBuilder) -> Unit) {
            values.forEach { it += foodProperties }
        }

        @JvmName("plusAssignMaterialFoodPropertyBuilder")
        operator fun Collection<Material>.plusAssign(foodProperties: (FoodPropertyBuilder) -> Unit) = forEach { it += foodProperties }

        @JvmName("plusAssignFoodPropertyBuilder")
        operator fun Collection<MaterialSetTag>.plusAssign(foodProperties: (FoodPropertyBuilder) -> Unit) = forEach { it += foodProperties }

        operator fun MaterialSetTag.timesAssign(value: Number) = values.forEach { it *= value }

        operator fun MaterialSetTag.divAssign(value: Number) = values.forEach { it /= value }

        operator fun MaterialSetTag.plusAssign(builder: suspend (Player) -> Unit) = values.forEach { it += builder }

        operator fun MaterialSetTag.plusAssign(builder: (PotionEffectBuilder) -> Unit) = values.forEach { it += builder }

        @JvmName("plusAssignTimedAttributeBuilder")
        operator fun MaterialSetTag.plusAssign(builder: (TimedAttributeBuilder) -> Unit) = values.forEach { it += builder }

        operator fun Material.timesAssign(value: Number) {
            modifyFood {
                this.nutrition = value.toInt()
                this.saturationModifier = value.toFloat()
            }
        }

        operator fun Material.divAssign(value: Number) {
            modifyFood {
                this.nutrition /= value.toInt()
                this.saturationModifier /= value.toFloat()
            }
        }

        operator fun Material.plusAssign(builder: suspend (Player) -> Unit) {
            foodBlocks[this] = builder
        }

        operator fun Material.plusAssign(builder: (PotionEffectBuilder) -> Unit) {
            modifyFood {
                addEffect {
                    builder(this)
                    foodKey(this@plusAssign)
                }
            }
        }

        @JvmName("plusAssignTimedAttributeBuilder")
        operator fun Material.plusAssign(builder: (TimedAttributeBuilder) -> Unit) {
            foodAttributes.put(this, TimedAttributeBuilder(builder).materialName(this, this@OriginBuilder))
        }

        operator fun Collection<Material>.timesAssign(value: Number) {
            for (food in this) food *= value
        }

        operator fun Collection<Material>.divAssign(value: Number) {
            for (food in this) food /= value
        }

        operator fun Collection<Material>.plusAssign(builder: suspend (Player) -> Unit) {
            for (food in this) food += builder
        }

        operator fun Collection<Material>.plusAssign(builder: (PotionEffectBuilder) -> Unit) {
            for (food in this) food += builder
        }

        @JvmName("plusAssignTimedAttributeBuilder")
        operator fun Collection<Material>.plusAssign(builder: (TimedAttributeBuilder) -> Unit) {
            for (food in this) food += builder
        }
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
