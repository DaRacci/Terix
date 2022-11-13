package dev.racci.terix.api.origins.origin

import arrow.analysis.pre
import arrow.core.Either
import com.google.common.collect.Multimap
import dev.racci.minix.api.annotations.MinixInternal
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.minix.api.utils.collections.muiltimap.MutableMultiMap
import dev.racci.minix.api.utils.collections.multiMapOf
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.exceptions.OriginCreationException
import dev.racci.terix.api.extensions.concurrentMultimap
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.abilities.passive.PassiveAbility
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
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

// TODO -> Return non mutable versions of all elements
public sealed class OriginValues : WithPlugin<MinixPlugin> {
    @MinixInternal
    public val eventListener: KListener<MinixPlugin> = object : KListener<MinixPlugin> {
        override val plugin get() = this@OriginValues.plugin
    }

    public open val name: String = this::class.simpleName
        ?.withIndex()
        ?.takeWhile { it.value.isLetter() || it.index == 0 }
        ?.map(IndexedValue<Char>::value)?.toString() ?: throw OriginCreationException("Origin name is null")

    public open val colour: TextColor = NamedTextColor.WHITE

    public open val becomeOriginTitle: TitleBuilder? = null

    public open val requirements: PersistentList<Pair<Component, (Player) -> Boolean>> = persistentListOf()

    public open var fireImmunity: Boolean = false
    public open var waterBreathing: Boolean = false

    public val item: OriginItem = OriginItem()
    public val sounds: SoundEffects = SoundEffects()
    public val displayName: Component by lazy { Component.text(name).color(colour) }

    public val statePotions: MutableMultiMap<State, PotionEffect> by lazy(::multiMapOf)
    public val stateDamageTicks: MutableMap<State, Double> by lazy(::mutableMapOf)
    public val stateTitles: MutableMap<State, TitleBuilder> by lazy(::mutableMapOf)
    public val stateBlocks: MutableMap<State, suspend (Player) -> Unit> by lazy(::mutableMapOf)

    // TODO -> More flexibility with keybindings
    public val keybindAbilityGenerators: Multimap<KeyBinding, AbilityGenerator<KeybindAbility>> by lazy(::concurrentMultimap)
    public val passiveAbilityGenerators: ArrayList<AbilityGenerator<PassiveAbility>> by lazy(::arrayListOf)

    public val activeKeybindAbilities: Multimap<Player, KeybindAbility> by lazy(::concurrentMultimap)
    public val activePassiveAbilities: Multimap<Player, PassiveAbility> by lazy(::concurrentMultimap)

    public val customMatcherFoodProperties: HashMap<ItemMatcher, FoodProperties> by lazy(::hashMapOf)
    public val customFoodProperties: HashMap<Material, FoodProperties> by lazy(::hashMapOf)
    public val customFoodActions: MutableMultiMap<Material, Either<ActionPropBuilder, TimedAttributeBuilder>> by lazy(::multiMapOf)

    public val attributeModifiers: MutableMultiMap<State, Pair<Attribute, AttributeModifier>> by lazy(::multiMapOf)
    public val damageActions: MutableMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> by lazy(::mutableMapOf)

    public data class AbilityGenerator<A : Ability> @PublishedApi internal constructor(
        public val abilityKClass: KClass<out A>,
        public val abilityBuilder: suspend A.(abilityPlayer: Player) -> Unit
    ) {
        public val name: String = abilityKClass.simpleName!!

        public suspend fun of(player: Player): A {
            pre(player.isOnline) { "Player must be online to generate ability" }

            val constructor = abilityKClass.primaryConstructor ?: throw OriginCreationException("No primary constructor for ability ${abilityKClass.simpleName}")
            val ability = constructor.call(player, TerixPlayer.cachedOrigin(player))

            abilityBuilder(ability, player)
            return ability
        }
    }
}
