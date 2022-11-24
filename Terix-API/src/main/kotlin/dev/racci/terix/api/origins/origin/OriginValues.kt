package dev.racci.terix.api.origins.origin

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
import kotlin.jvm.Throws
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType

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

    public data class AbilityGenerator<out A : Ability> @PublishedApi internal constructor(
        public val abilityKClass: KClass<out A>,
        public val abilityBuilder: (@UnsafeVariance A).() -> Unit,
        public val additionalConstructorParams: Array<Pair<KProperty1<@UnsafeVariance A, *>, *>> = emptyArray()
    ) {
        public val name: String = abilityKClass.simpleName!!

        @Throws(IllegalArgumentException::class)
        public fun of(player: Player): A {
            val constructor = abilityKClass.primaryConstructor ?: throw OriginCreationException("No primary constructor for ability ${abilityKClass.simpleName}")

            val constructorMap = buildMap(constructor.parameters.size + 1) {
                fun addRequiredParameter(
                    name: String,
                    value: Any?
                ) {
                    val parameter = constructor.findParameterByName(name)

                    requireNotNull(parameter) { "No parameter with name $name" }
                    require((parameter.type.isMarkedNullable && value != null) || !parameter.type.isMarkedNullable) { "Value for parameter $name is null but parameter is not nullable" }
                    if (value != null) require(parameter.type.classifier!!.starProjectedType.isSupertypeOf(value::class.starProjectedType)) { "Value for parameter $name is not of type ${parameter.type}" }

                    put(parameter, value)
                }

                addRequiredParameter("abilityPlayer", player)
                addRequiredParameter("linkedOrigin", TerixPlayer.cachedOrigin(player))

                additionalConstructorParams.forEach { (property, value) -> addRequiredParameter(property.name, value) }
            }

            val missingParams = constructor.parameters.filterNot { it.isOptional || it in constructorMap.keys }
            return if (missingParams.isEmpty()) {
                constructor.callBy(constructorMap).apply(abilityBuilder)
            } else throw IllegalArgumentException("Missing parameters for ability $name: ${missingParams.joinToString { it.name!! }}")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AbilityGenerator<*>) return false

            if (abilityKClass != other.abilityKClass) return false
            if (abilityBuilder != other.abilityBuilder) return false
            if (!additionalConstructorParams.contentEquals(other.additionalConstructorParams)) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = abilityKClass.hashCode()
            result = 31 * result + abilityBuilder.hashCode()
            result = 31 * result + additionalConstructorParams.contentHashCode()
            result = 31 * result + name.hashCode()
            return result
        }

        override fun toString(): String {
            return "AbilityGenerator(abilityKClass=$abilityKClass, abilityBuilder=$abilityBuilder, additionalConstructorParams=${additionalConstructorParams.contentToString()}, name='$name')"
        }
    }
}
