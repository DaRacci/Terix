package dev.racci.terix.api.origins.origin

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.firstOrNone
import arrow.core.getOrElse
import arrow.optics.lens
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.annotations.MinixInternal
import dev.racci.minix.api.extensions.KListener
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.exceptions.OriginCreationException
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.sounds.SoundEffects
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.food.FoodProperties
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import kotlin.jvm.Throws
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmErasure

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

    public var damageActions: PersistentMap<EntityDamageEvent.DamageCause, suspend EntityDamageEvent.() -> Unit> = persistentMapOf(); internal set

    public var abilityData: AbilityData = AbilityData(
        persistentSetOf(),
        Caffeine.newBuilder().removalListener<Player, AbilityData.PlayerAbilityHolder> { _, value, _ ->
            runBlocking { value?.close() }
        }.weakKeys().build()
    )
    public var foodData: FoodData = FoodData.empty; internal set
    public var stateData: ImmutableStateMap<StateData> = ImmutableStateMap.building(StateData::empty); internal set

    public class ImmutableStateMap<V> private constructor(private val backingMap: PersistentMap<State, V>) : ImmutableMap<State, V> {
        override val size: Int by backingMap::size
        override val entries: ImmutableSet<Map.Entry<State, V>> by backingMap::entries
        override val keys: ImmutableSet<State> by backingMap::keys
        override val values: ImmutableCollection<V> by backingMap::values

        override fun containsKey(key: State): Boolean = true

        override fun containsValue(value: V): Boolean = backingMap.containsValue(value)

        override fun get(key: State): V = backingMap[key]!!

        override fun isEmpty(): Boolean = false

        public fun <A> modify(
            state: State,
            property: KProperty1<V, A>,
            map: (focus: A) -> A
        ): ImmutableStateMap<V> {
            val currentValue = this[state]
            val newValue = property.lens.modify(currentValue, map)
            return ImmutableStateMap(backingMap.put(state, newValue))
        }

        internal companion object {
            fun <T> building(creator: () -> T): ImmutableStateMap<T> = ImmutableStateMap(State.values.associateWith { creator() }.toPersistentMap())
        }
    }

    public data class AbilityData internal constructor(
        public val generators: PersistentSet<AbilityGenerator<*>>,
        private val cache: Cache<Player, PlayerAbilityHolder>
    ) {
        public operator fun get(player: Player): PlayerAbilityHolder = cache.getIfPresent(player) ?: PlayerAbilityHolder.empty

        internal suspend fun create(player: Player) {
            val holder = this(player)
            cache.put(player, holder)
        }

        internal suspend fun close(player: Player) {
            cache.getIfPresent(player)?.close()
        }

        private suspend operator fun invoke(player: Player): PlayerAbilityHolder {
            val abilities = mutableSetOf<Ability>()

            for (generator in generators) {
                val ability = generator(player).also(abilities::add)

                if (ability is KeybindAbility) {
                    require(generator.keybinding is Some) { "Keybinding must be set for keybind ability, $ability." }
                    ability.activateWithKeybinding(generator.keybinding.value)
                }

                ability.register()
            }

            return PlayerAbilityHolder(abilities.toPersistentSet())
        }

        @JvmInline
        public value class PlayerAbilityHolder internal constructor(public val abilities: PersistentSet<Ability>) : ImmutableSet<Ability> by abilities {
            public suspend fun close() { abilities.forEach { ability -> ability.unregister() } }

            public companion object {
                public val empty: PlayerAbilityHolder = PlayerAbilityHolder(persistentSetOf())
            }
        }
    }

    public data class FoodData internal constructor(
        val matcherProperties: PersistentMap<ItemMatcher, FoodProperties>,
        val materialProperties: PersistentMap<Material, FoodProperties>,
        val materialActions: PersistentMap<Material, PlayerLambda>
    ) {
        /**
         * Attempts to find present food properties for the given [itemStack]
         * Attempts to find a [FoodProperties] instance from the matchers then fallback to the material.
         *
         * @param itemStack The item stack to find food properties for
         * @return The food properties for the given item stack or null if none are found.
         */
        public fun getProperties(itemStack: ItemStack): FoodProperties? {
            return matcherProperties.entries
                .firstOrNone { it.key.matches(itemStack) }
                .map(Map.Entry<*, FoodProperties>::value)
                .getOrElse { materialProperties[itemStack.type] }
        }

        /**
         * Attempts to find present food actions for the given [itemStack]
         * Attempts to find a [PlayerLambda] instance from the matchers then fallback to the material.
         *
         * @param itemStack The item stack to find food actions for
         * @return The food actions for the given item stack or null if none are found.
         */
        public fun getAction(itemStack: ItemStack): PlayerLambda? {
            return materialActions[itemStack.type]
        }

        internal companion object {
            val empty: FoodData = FoodData(
                persistentMapOf(),
                persistentMapOf(),
                persistentMapOf()
            )
        }
    }

    public data class StateData internal constructor(
        val title: Option<TitleBuilder>,
        val action: Option<PlayerLambda>,
        val damage: Option<Double>,
        val potions: PersistentSet<PotionEffectBuilder>,
        val modifiers: PersistentSet<AttributeModifierBuilder>
    ) {
        public fun isEmpty(): Boolean = title.isEmpty() && action.isEmpty() && damage.isEmpty() && potions.isEmpty() && modifiers.isEmpty()

        internal companion object {
            val empty: StateData = StateData(
                None,
                None,
                None,
                persistentSetOf(),
                persistentSetOf()
            )
        }
    }

    public data class AbilityGenerator<A : Ability> @PublishedApi internal constructor(
        public val keybinding: Option<KeyBinding>,
        public val abilityKClass: KClass<out A>,
        public val abilityBuilder: (abilityInstance: A) -> Unit,
        public val additionalConstructorParams: Array<out Pair<KProperty1<A, *>, *>> = emptyArray()
    ) {
        public val name: String = abilityKClass.simpleName!!

        @Throws(IllegalArgumentException::class)
        public operator fun invoke(player: Player): A {
            val constructor = abilityKClass.primaryConstructor ?: throw OriginCreationException("No primary constructor for ability ${abilityKClass.simpleName}")

            val constructorMap = buildMap(constructor.parameters.size + 1) {
                fun addRequiredParameter(
                    name: String,
                    value: Any?
                ) {
                    val parameter = constructor.findParameterByName(name)

                    // TODO -> Better type checks
                    requireNotNull(parameter) { "No parameter with name $name" }
                    require((parameter.type.isMarkedNullable && value != null) || !parameter.type.isMarkedNullable) { "Value for parameter $name is null but parameter is not nullable" }
                    if (value != null && parameter.type.classifier != null) {
                        val requiredErasure = parameter.type.jvmErasure.java
                        val actualErasure = value::class.starProjectedType.jvmErasure.java
                        require(requiredErasure.isAssignableFrom(actualErasure)) { "Value for parameter $name is not of type ${parameter.type}, found ${value::class}" }
                    }

                    put(parameter, value)
                }

                addRequiredParameter("abilityPlayer", TerixPlayer[player])
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

        override fun toString(): String = buildString {
            append("AbilityGenerator(")
            append("abilityKClass=[").append(abilityKClass.simpleName).append("], ")
            append("abilityKeybinding=[").append(keybinding).append("], ")
            append("abilityBuilder=[").append(abilityBuilder).append("], ")
            append("additionalConstructorParams=[").append(additionalConstructorParams.joinToString(", ", "{", "}") { "${it.first.name}:${it.second}" }).append("]")
        }
    }
}
