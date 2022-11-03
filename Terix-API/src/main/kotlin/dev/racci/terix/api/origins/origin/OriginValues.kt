package dev.racci.terix.api.origins.origin

import arrow.core.Either
import dev.racci.minix.api.annotations.MinixInternal
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.collections.muiltimap.MutableMultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.exceptions.OriginCreationException
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
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

public sealed class OriginValues : WithPlugin<MinixPlugin> {
    private val specials = arrayOfNulls<Boolean>(2)

    @MinixInternal
    public val eventListener: KListener<MinixPlugin> = object : KListener<MinixPlugin> {
        override val plugin get() = this@OriginValues.plugin
    }

    public open val name: String = this::class.simpleName
        ?.withIndex()
        ?.takeWhile { it.value.isLetter() || it.index == 0 }
        ?.map(IndexedValue<Char>::value)?.toString() ?: throw OriginCreationException("Origin name is null")

    public open val colour: TextColor = NamedTextColor.WHITE

    public open val permission: String? = null

    public open val becomeOriginTitle: TitleBuilder? = null

    public open val requirements: PersistentList<Pair<Component, (Player) -> Boolean>> = persistentListOf()

    public open var fireImmunity: Boolean
        get() = specials[0] ?: false
        protected set(value) {
            specials[0] = value
        }
    public open var waterBreathing: Boolean
        get() = specials[1] ?: false
        protected set(value) {
            specials[1] = value
        }

    public val item: OriginItem = OriginItem()
    public val sounds: SoundEffects = SoundEffects()
    public val displayName: Component by lazy { Component.text(name).color(colour) }

    public val statePotions: MutableMultiMap<State, PotionEffect> by lazy(::multiMapOf)
    public val stateDamageTicks: MutableMap<State, Double> by lazy(::mutableMapOf)
    public val stateTitles: MutableMap<State, TitleBuilder> by lazy(::mutableMapOf)
    public val stateBlocks: MutableMap<State, suspend (Player) -> Unit> by lazy(::mutableMapOf)

    public val abilities: MutableMap<KeyBinding, Ability> by lazy(::mutableMapOf)

    public val customMatcherFoodProperties: HashMap<ItemMatcher, FoodProperties> by lazy(::hashMapOf)
    public val customFoodProperties: HashMap<Material, FoodProperties> by lazy(::hashMapOf)
    public val customFoodActions: MutableMultiMap<Material, Either<ActionPropBuilder, TimedAttributeBuilder>> by lazy(::multiMapOf)

    public val attributeModifiers: MutableMultiMap<State, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    public val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)
}
