package dev.racci.terix.core.origins

import arrow.core.Option
import com.google.common.collect.Multimap
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.round
import dev.racci.minix.api.utils.PropertyFinder
import dev.racci.minix.api.utils.collections.muiltimap.MultiMap
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.data.Lang
import dev.racci.terix.api.dsl.AttributeModifierBuilder
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.origin.OriginValues.StateData
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.ImmutableSet
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.minecraft.world.food.Foods
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageEvent
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty1

public object OriginInfo {

    public class Info internal constructor(
        public val defaultMethod: Origin.() -> Component
    ) {
        private var _cached: Component? = null
        public lateinit var name: String; private set

        public operator fun getValue(thisRef: Any?, property: KProperty<*>): Info {
            name = property.name
            return this@Info
        }

        public fun get(origin: Origin): Component {
            if (_cached == null) _cached = defaultMethod(origin)
            return _cached!!
        }
    }

    // FIXME
    public object Types : PropertyFinder<Info>(KeyMode.CAPITALISED) {
        public val GENERAL: Info by Info {
            text {
                appendKeyValue("Name", name)
                appendKeyValue("Colour", colour)
                appendKeyValue("DisplayName", displayName)
            }
        }

        public val SPECIALS: Info by Info {
            text {
                appendKeyValue("Fire Immunity", fireImmunity)
                appendKeyValue("Water Breathing", waterBreathing)
            }
        }

        public val SOUNDS: Info by Info {
            text {
                appendKeyValue("Hurt", formatSound(sounds.hurtSound))
                appendKeyValue("Death", formatSound(sounds.deathSound))
                appendKeyValue("Ambient", formatSound(sounds.ambientSound))
            }
        }

        public val ABILITIES: Info by Info {
            text {
                iterate(this, abilityData.generators, OriginValues.AbilityGenerator<*>::name)
            }
        }

        public fun <T : Any> Origin.all(
            property: KProperty1<StateData, T>,
            predicate: (T) -> Boolean
        ): Boolean = stateData.values.all { predicate(property.get(it)) }

        public val STATE_POTIONS: Info by Info {
            if (all(StateData::potions, ImmutableSet<*>::isEmpty)) return@Info Component.empty()

            text {
                iterate(this, StateData::potions, ::formatPotion)
            }
        }

//        public val STATE_DAMAGE_TICKS: Info by Info {
//            if (all(StateData::damage, Option<*>::isEmpty)) return@Info Component.empty()
//
//            text {
//                iterate(this, StateData::damage, State::name, Double::toString)
//            }
//        }

        public val STATE_TITLES: Info by Info {
            if (all(StateData::title, Option<*>::isEmpty)) return@Info Component.empty()

            text {
                stateData.mapValues { it.value.title }
                    .filterValues { it.isDefined() }
                    .mapValues { it.value.orNull()!! }
                    .mapValues { (_, builder) ->
                        Component.text("Title: ")
                            .append(builder.title)
                            .append(Component.text("Subtitle: "))
                            .append(builder.subtitle)
                            .append(Component.text("Fade In: "))
                            .append(Component.text(builder.times.fadeIn().toMillis()))
                            .append(Component.text("Stay: "))
                            .append(Component.text(builder.times.stay().toMillis()))
                            .append(Component.text("Fade Out: "))
                            .append(Component.text(builder.times.fadeOut().toMillis()))
                            .append(Component.text("Sound: "))
                            .append(Component.text(builder.sound.asString()))
                    }.forEach { (state, title) -> appendKeyValue(state.name, title) }
            }
        }
//
//        public val STATE_BLOCKS: Info by Info {
//            if (all(StateData::action, Option<*>::isEmpty)) return@Info Component.empty()
//
//            text {
//                iterate(this, StateData::action, State::name, Any::toString)
//            }
//        }

        public val FOOD_PROPERTIES: Info by Info {
            if (this.foodData.materialProperties.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, foodData.materialProperties, Material::name) { mat ->
                    val default = Foods.DEFAULT_PROPERTIES[mat.key.key]

                    buildString {
                        fun node(
                            nodeName: String,
                            defaultValue: Any?,
                            newValue: Any
                        ) {
                            if (defaultValue == newValue) return

                            append("\n        ")
                            append(nodeName)
                            append(": ")
                            append(defaultValue)
                            append(" -> ")
                            append(newValue)
                        }

                        node("Nutrition", default?.nutrition, nutrition)
                        node("Saturation", default?.saturationModifier, saturationModifier)
                        node("Meat", default?.isMeat, isMeat)
                        node("Always Edible", default?.canAlwaysEat(), canAlwaysEat())
                        node("Fast Eating", default?.isFastFood, isFastFood)
                        if (effects.isNotEmpty()) {
                            append("\n        Effects: ")
                            effects.joinToString("\n            ", "\n            ") {
                                //                                formatPotion(PotionEffectBuilder(it.first))
                                "TODO"
                            }
                        }
                    }
                }
            }
        }

        public val FOOD_ACTIONS: Info by Info {
            if (this.foodData.materialActions.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, foodData.materialActions, Material::name, Any::toString)
            }
        }

        public val STATE_ATTRIBUTES: Info by Info {
            if (all(StateData::modifiers, ImmutableSet<*>::isEmpty)) return@Info Component.empty()

            text {
                iterate(this, StateData::modifiers, ::formatModifier)
            }
        }

        public val DAMAGE_ACTIONS: Info by Info {
            if (this.damageActions.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, damageActions, EntityDamageEvent.DamageCause::name, Any::toString)
            }
        }

        private fun TextComponent.Builder.appendKeyValue(
            key: String,
            value: Any?
        ) = append {
            getKoin().get<Lang>().origin.descriptor.keyedBodyLine[
                "key" to { key },
                "value" to { value ?: "null" }
            ]
        }

        private fun TextComponent.Builder.appendValue(value: () -> Any) = append {
            getKoin().get<Lang>().origin.descriptor.bodyLine[
                "value" to { value() }
            ]
        }

        private fun <E, V> iterate(
            builder: TextComponent.Builder,
            multiMap: MultiMap<E, V>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String = { toString() },
            entryMapper: (V.(E) -> String)? = null
        ) = iterate(builder, multiMap.entries, keyMapper, valueMapper, entryMapper)

        private fun <E, V> iterate(
            builder: TextComponent.Builder,
            multiMap: Multimap<E, V>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String = { toString() },
            entryMapper: (V.(E) -> String)? = null
        ) = iterate(builder, multiMap.entries(), keyMapper, valueMapper, entryMapper)

        private fun <E, V> iterate(
            builder: TextComponent.Builder,
            map: Map<E, V>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String = { toString() },
            entryMapper: (V.(E) -> String)? = null
        ) = iterate(builder, map.entries, keyMapper, valueMapper, entryMapper)

        private fun <V> Origin.iterate(
            builder: TextComponent.Builder,
            property: KProperty1<StateData, Collection<V>>,
            valueMapper: V.() -> String = { toString() },
            entryMapper: (V.(State) -> String)? = null
        ) {
            val entries = stateData.entries.flatMap { (state, data) -> property.get(data).associateBy { state }.entries }

            iterate(
                builder = builder,
                collection = entries,
                keyMapper = State::name,
                valueMapper = valueMapper,
                entryMapper = entryMapper
            )
        }

        private fun <E, V> iterate(
            builder: TextComponent.Builder,
            collection: Collection<Map.Entry<E, *>>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String,
            entryMapper: (V.(E) -> String)?
        ) {
            if (collection.isEmpty()) return

            for ((key, value) in collection) {
                fun format(obj: Any): String = when (entryMapper) {
                    null -> valueMapper(obj.castOrThrow())
                    else -> entryMapper(obj.castOrThrow(), key)
                }

                val keyName = keyMapper(key)
                val formatted = if (value is Collection<*>) {
                    value.joinToString("\n    ", "\n    ", "\n") { format(it.castOrThrow()) }
                } else format(value.castOrThrow())

                builder.appendKeyValue(keyName, formatted)
            }
        }

        private fun <V> iterate(
            builder: TextComponent.Builder,
            collection: Collection<V>,
            valueMapper: V.() -> String
        ) {
            for (value in collection) {
                builder.appendValue {
                    when {
                        value is Collection<*> && value.size > 1 -> value.joinToString("\n    ", "\n    ", "\n") { valueMapper(it.castOrThrow()) }
                        value is Collection<*> -> valueMapper(value.first().castOrThrow())
                        else -> valueMapper(value)
                    }
                }
            }
        }

        private fun formatModifier(builder: AttributeModifierBuilder) = buildString {
            append(builder.name)
            append(" = ( Op: ")
            append(builder.operation.name)
            append(" | Val: ")
            append(builder.amount.round(4))
            append(" )")
        }

        private fun formatPotion(potion: PotionEffectBuilder) = buildString {
            append(potion.type.name)
            append(" - ")
            append(potion.amplifier)
            append("L")
            append(" - ")
            append(potion.duration / 50)
            append("t")
        }

        private fun formatSound(sound: SoundEffect?) = buildString {
            if (sound == null) {
                append("null")
                return@buildString
            }

            append(sound.resourceKey)
            append(" - ")
            append(sound.pitch)
            append("P - ")
            append(sound.volume)
            append("V - ")
            append(sound.distance)
            append("D")
        }
    }
}
