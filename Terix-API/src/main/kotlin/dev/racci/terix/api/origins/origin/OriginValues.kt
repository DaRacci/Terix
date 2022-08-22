package dev.racci.terix.api.origins.origin

import dev.racci.minix.api.utils.collections.MultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.exceptions.OriginCreationException
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.origins.states.State
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
    private val specials = arrayOfNulls<Boolean>(2)

    open val name: String = this::class.simpleName
        ?.withIndex()
        ?.takeWhile { it.value.isLetter() || it.index == 0 }
        ?.map(IndexedValue<Char>::value)?.toString() ?: throw OriginCreationException("Origin name is null")

    open val colour: TextColor = NamedTextColor.WHITE

    open val permission: String? = null

    open val becomeOriginTitle: TitleBuilder? = null

    open var fireImmunity: Boolean
        get() = specials[0] ?: false
        protected set(value) {
            specials[0] = value
        }
    open var waterBreathing: Boolean
        get() = specials[1] ?: false
        protected set(value) {
            specials[1] = value
        }

    val item: OriginItem = OriginItem()
    val sounds: SoundEffects = SoundEffects()
    val displayName: Component by lazy { Component.text(name).color(colour) }

    val potions: MultiMap<State, PotionEffect> by lazy(::multiMapOf)
    val damageTicks: MutableMap<State, Double> by lazy(::mutableMapOf)
    val titles: MutableMap<State, TitleBuilder> by lazy(::mutableMapOf)
    val abilities: MutableMap<KeyBinding, Ability> by lazy(::mutableMapOf)
    val customFoodProperties: HashMap<Material, FoodProperties> by lazy(::hashMapOf)
    val foodAttributes: MultiMap<Material, TimedAttributeBuilder> by lazy(::multiMapOf)
    val foodBlocks: MutableMap<Material, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val stateBlocks: MutableMap<State, suspend (Player) -> Unit> by lazy(::mutableMapOf)
    val attributeModifiers: MultiMap<State, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)
}
