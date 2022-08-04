package dev.racci.terix.api.origins.origin

import dev.racci.minix.api.utils.collections.MultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.exceptions.OriginCreationException
import dev.racci.terix.api.origins.AbstractAbility
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffects
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.food.FoodProperties
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect

sealed class OriginValues {
    private val specials = arrayOfNulls<Boolean>(3)

    open val name: String = this::class.simpleName
        ?.withIndex()
        ?.takeWhile { it.value.isLetter() || it.index == 0 }
        ?.map(IndexedValue<Char>::value)?.toString() ?: throw OriginCreationException("Origin name is null")

    open val colour: TextColor = NamedTextColor.WHITE

    open val permission: String? = null

    open val becomeOriginTitle: TitleBuilder? = null

    open var nightVision: Boolean
        get() = specials[0] ?: false
        protected set(value) {
            specials[0] = value
        }
    open var fireImmunity: Boolean
        get() = specials[1] ?: false
        protected set(value) {
            specials[1] = value
        }
    open var waterBreathing: Boolean
        get() = specials[2] ?: false
        protected set(value) {
            specials[2] = value
        }

    val item: OriginItem = OriginItem()
    val sounds: SoundEffects = SoundEffects()
    val displayName: Component by lazy { Component.text(name).color(colour) }

    val potions: MultiMap<Trigger, PotionEffect> by lazy(::multiMapOf)
    val damageTicks: MutableMap<Trigger, Double> by lazy(::mutableMapOf)
    val titles: MutableMap<Trigger, TitleBuilder> by lazy(::mutableMapOf)
    val abilities: MutableMap<KeyBinding, AbstractAbility> by lazy(::mutableMapOf)
    val customFoodProperties: HashMap<Material, FoodProperties> by lazy(::hashMapOf)
    val foodAttributes: MultiMap<Material, TimedAttributeBuilder> by lazy(::multiMapOf)
    val foodBlocks: MutableMap<Material, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val triggerBlocks: MutableMap<Trigger, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val attributeModifiers: MultiMap<Trigger, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)
}
