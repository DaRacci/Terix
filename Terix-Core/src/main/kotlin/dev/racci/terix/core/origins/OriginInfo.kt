package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.round
import dev.racci.minix.api.utils.PropertyFinder
import dev.racci.minix.api.utils.collections.muiltimap.MultiMap
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.abilities.KeybindAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.data.Lang
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.minecraft.world.food.Foods
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.craftbukkit.v1_19_R1.potion.CraftPotionUtil
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffect
import kotlin.reflect.KProperty

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

    // TODO: Nightvision, water breathing, fire immune
    // TODO: Attribute modifiers
    // TODO: Titles
    // TODO: Potions
    // TODO: food (blocks, potions, attributes, multipliers)
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
            if (this.abilities.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, abilities, KeyBinding::name, KeybindAbility::name)
            }
        }

        public val STATE_POTIONS: Info by Info {
            if (this.statePotions.isEmpty) return@Info Component.empty()

            text {
                iterate(this, statePotions, State::name, ::formatPotion)
            }
        }

        public val STATE_DAMAGE_TICKS: Info by Info {
            if (this.stateDamageTicks.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, stateDamageTicks, State::name, Double::toString)
            }
        }

        public val STATE_TITLES: Info by Info {
            if (this.stateTitles.isEmpty()) return@Info Component.empty()

            text {
                for ((state, title) in stateTitles) {
                    appendKeyValue(
                        state.name,
                        Component.text("Title: ")
                            .append(title.title)
                            .append(Component.text("Subtitle: "))
                            .append(title.subtitle)
                            .append(Component.text("Fade In: "))
                            .append(Component.text(title.times.fadeIn().toMillis()))
                            .append(Component.text("Stay: "))
                            .append(Component.text(title.times.stay().toMillis()))
                            .append(Component.text("Fade Out: "))
                            .append(Component.text(title.times.fadeOut().toMillis()))
                            .append(Component.text("Sound: "))
                            .append(Component.text(title.sound.asString()))
                    )
                }
            }
        }

        public val STATE_BLOCKS: Info by Info {
            if (this.stateBlocks.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, stateBlocks, State::name, Any::toString)
            }
        }

        public val FOOD_PROPERTIES: Info by Info {
            if (this.customFoodProperties.isEmpty()) return@Info Component.empty()

            text {
                iterate(this, customFoodProperties, Material::name) { mat ->
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
                                formatPotion(CraftPotionUtil.toBukkit(it.first))
                            }
                        }
                    }
                }
            }
        }

        public val FOOD_ACTIONS: Info by Info {
            if (this.customFoodActions.isEmpty) return@Info Component.empty()

            text {
                iterate(this, customFoodActions, Material::name, Any::toString)
            }
        }

        public val STATE_ATTRIBUTES: Info by Info {
            if (this.attributeModifiers.isEmpty) return@Info Component.empty()

            text {
                iterate(this, attributeModifiers, State::name, ::formatModifier)
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
            getKoin().get<Lang>().origin.descriptor.bodyLine[
                "key" to { key },
                "value" to { value ?: "null" }
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
            map: Map<E, V>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String = { toString() },
            entryMapper: (V.(E) -> String)? = null
        ) = iterate(builder, map.entries, keyMapper, valueMapper, entryMapper)

        private fun <E, V> iterate(
            builder: TextComponent.Builder,
            collection: Collection<Map.Entry<E, *>>,
            keyMapper: E.() -> String,
            valueMapper: V.() -> String,
            entryMapper: (V.(E) -> String)?
        ) {
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

        private fun formatModifier(pair: Pair<Attribute, AttributeModifier>) = buildString {
            append(pair.first.name)
            append(" = ( Op: ")
            append(pair.second.operation.name)
            append(" | Val: ")
            append(pair.second.amount.round(4))
            append(" )")
        }

        private fun formatPotion(potion: PotionEffect) = buildString {
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
