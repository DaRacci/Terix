package dev.racci.terix.api.origins

import com.destroystokyo.paper.MaterialSetTag
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.utils.collections.MultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect

abstract class AbstractOrigin : IAbstractOrigin {

    private val potionBuilder by lazy(::PotionsBuilderImpl)
    private val attributeBuilder by lazy(::AttributeBuilderImpl)
    private val timeTitleBuilder by lazy(::TimeTitleBuilderImpl)
    private val damageBuilder by lazy(::DamageBuilderImpl)
    private val foodBuilder by lazy(::FoodBuilderImpl)
    private val itemBuilder by lazy(::ItemBuilderImpl)

    val attributeModifiers: MultiMap<Trigger, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    val titles: MutableMap<Trigger, TitleBuilder> by lazy(::mutableMapOf)
    val potions: MultiMap<Trigger, PotionEffect> by lazy(::multiMapOf)
    val damageTicks: MutableMap<Trigger, Double> by lazy(::mutableMapOf)
    val triggerBlocks: MutableMap<Trigger, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val damageMultipliers: MutableMap<EntityDamageEvent.DamageCause, Double> by lazy(::mutableMapOf)
    val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)
    val foodPotions: MultiMap<Material, PotionEffect> by lazy(::multiMapOf)
    val foodAttributes: MultiMap<Material, TimedAttributeBuilder> by lazy(::multiMapOf)
    val foodMultipliers: MutableMap<Material, Int> by lazy(::mutableMapOf)

    lateinit var itemMaterial: Material
    lateinit var itemName: Component
    lateinit var itemLore: List<Component>

    override val displayName by lazy { Component.text(name).color(colour) }

    override val nightVision: Boolean = false
    override val waterBreathing: Boolean = false
    override val fireImmune: Boolean = false
    override val becomeOriginTitle: TitleBuilder? = null

    @MinixDsl
    protected suspend fun potions(builder: suspend IAbstractOrigin.PotionsBuilder.() -> Unit) {
        builder(potionBuilder)
    }

    @MinixDsl
    protected suspend fun attributes(builder: suspend IAbstractOrigin.AttributeBuilder.() -> Unit) {
        builder(attributeBuilder)
    }

    @MinixDsl
    protected suspend fun title(builder: suspend IAbstractOrigin.TimeTitleBuilder.() -> Unit) {
        builder(timeTitleBuilder)
    }

    @MinixDsl
    protected suspend fun damage(builder: suspend IAbstractOrigin.DamageBuilder.() -> Unit) {
        builder(damageBuilder)
    }

    @MinixDsl
    protected suspend fun food(builder: suspend IAbstractOrigin.FoodBuilder.() -> Unit) {
        builder(foodBuilder)
    }

    @MinixDsl
    protected suspend fun item(builder: suspend IAbstractOrigin.ItemBuilder.() -> Unit) {
        builder(itemBuilder)
    }

    inner class PotionsBuilderImpl : IAbstractOrigin.PotionsBuilder {

        @MinixDsl
        override infix fun Trigger.causes(builder: PotionEffectBuilder.() -> Unit) {
            val potionEffectBuilder = PotionEffectBuilder()
            potionEffectBuilder.builder()
            potionEffectBuilder.originKey(this@AbstractOrigin, this)
            potions.put(this, potionEffectBuilder.build())
        }
    }

    inner class AttributeBuilderImpl : IAbstractOrigin.AttributeBuilder {

        @MinixDsl
        override fun Attribute.setBase(builder: AttributeModifierBuilder.() -> Unit) {
            val modifierBuilder = AttributeModifierBuilder()
            modifierBuilder.builder()
            modifierBuilder.attribute = this
            modifierBuilder.name = "origin_modifier_${this@AbstractOrigin.name.lowercase()}_${Trigger.ON.name.lowercase()}" // Use Enum incase of changes of name or something.
            attributeModifiers.put(Trigger.ON, this to modifierBuilder.build())
        }

        @MinixDsl
        override infix fun Trigger.causes(builder: AttributeModifierBuilder.() -> Unit) {
            val attributeModifierBuilder = AttributeModifierBuilder()
            attributeModifierBuilder.builder()
            attributeModifierBuilder.name = "origin_modifier_${this@AbstractOrigin.name.lowercase()}_${this@causes.name.lowercase()}"
            attributeModifiers.put(this, attributeModifierBuilder.attribute to attributeModifierBuilder.build())
        }
    }

    inner class TimeTitleBuilderImpl : IAbstractOrigin.TimeTitleBuilder {

        @MinixDsl
        override infix fun Trigger.causes(builder: TitleBuilder.() -> Unit) {
            val titleBuilder = TitleBuilder()
            titleBuilder.builder()
            titles[this] = titleBuilder
        }
    }

    inner class DamageBuilderImpl : IAbstractOrigin.DamageBuilder {
        @MinixDsl
        override infix fun Trigger.invokes(builder: suspend (Player) -> Unit) { triggerBlocks[this] = builder }

        @MinixDsl
        override infix fun Trigger.ticks(damage: Double) { damageTicks[this] = damage }

        @MinixDsl
        override fun EntityDamageEvent.DamageCause.multiplied(multiplier: Double) { damageMultipliers[this] = multiplier }

        @MinixDsl
        override fun Collection<EntityDamageEvent.DamageCause>.multiplied(multiplier: Double) { this.forEach { it.multiplied(multiplier) } }

        @MinixDsl
        override fun EntityDamageEvent.DamageCause.triggers(action: suspend EntityDamageEvent.() -> Unit) { damageActions[this] = action }
    }

    inner class FoodBuilderImpl : IAbstractOrigin.FoodBuilder {

        @MinixDsl
        override fun MaterialSetTag.effects(builder: PotionEffectBuilder.() -> Unit) { values.forEach { it.effects(builder) } }

        @MinixDsl
        override fun Material.effects(builder: PotionEffectBuilder.() -> Unit) { foodPotions.put(this, PotionEffectBuilder().apply { builder(); originKey(this@AbstractOrigin.name, "name") }.build()) }

        @MinixDsl
        override fun MaterialSetTag.applies(builder: TimedAttributeBuilder.() -> Unit) { values.forEach { it.applies(builder) } }

        @MinixDsl
        override fun Material.applies(builder: TimedAttributeBuilder.() -> Unit) { foodAttributes.put(this, TimedAttributeBuilder().apply(builder)) }

        @MinixDsl
        override fun MaterialSetTag.multiplied(multiplier: Int) { values.forEach { it.multiplied(multiplier) } }

        @MinixDsl
        override fun Material.multiplied(value: Int) { foodMultipliers[this] = value }
    }

    inner class ItemBuilderImpl : IAbstractOrigin.ItemBuilder {

        override fun AbstractOrigin.named(component: Component) { itemName = component }

        override fun AbstractOrigin.material(material: Material) { itemMaterial = material }

        override fun AbstractOrigin.lore(builder: MutableMap<Int, Component>.() -> Unit) {
            itemLore = mutableMapOf<Int, Component>().apply(builder).values.toList()
        }
    }
}
