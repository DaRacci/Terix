package dev.racci.terix.api.origins.origin

import dev.racci.terix.api.origins.states.State
import net.kyori.adventure.extra.kotlin.plus
import net.kyori.adventure.extra.kotlin.text
import net.kyori.adventure.text.Component
import kotlin.reflect.KProperty

object OriginInfo {

    class Info<T> internal constructor(
        val defaultMethod: Origin.() -> T
    ) {
        private var _cached: T? = null
        lateinit var name: String; private set

        operator fun getValue(thisRef: Any?, property: KProperty<*>): Info<T> {
            name = property.name
            return this@Info
        }

        fun get(origin: Origin): T {
            if (_cached == null) _cached = defaultMethod(origin)
            return _cached!!
        }
    }

    // TODO: General info (name, colour, displayName, permission)
    // TODO: Nightvision, water breathing, fire immune
    // TODO: Attribute modifiers
    // TODO: Titles
    // TODO: Potions
    // TODO: food (blocks, potions, attributes, multipliers)
    object Types {
        val GENERAL by Info {
            text {
                append { Component.text("Name: $name") + Component.newline() }
                append { Component.text("Colour: ${colour.asHexString()}") + Component.newline() }
                append { Component.text("Display Name: $displayName") + Component.newline() }
                append { Component.text("Permission: $permission") + Component.newline() }
            }
        }
        val ABILITIES by Info {
            if (this.abilities.isEmpty()) return@Info Component.empty()

            text {
                append { Component.text("Abilities: ") }

                val iterator = abilities.iterator()
                while (iterator.hasNext()) {
                    val (key, value) = iterator.next()

                    append { Component.text("${key.name}: ${value::class.simpleName}") }
                    if (iterator.hasNext()) append { Component.text(", ") }
                }
            }
        }
        val TRIGGER_BLOCKS by Info {
            if (this.triggerBlocks.isEmpty()) return@Info ""

            val builder = StringBuilder("Trigger Blocks: [ ")
            this@Types.iterate(builder, this.triggerBlocks.iterator())

            builder.toString()
        }
        val DAMAGE_TICKS by Info {
            if (this.damageTicks.isEmpty()) return@Info ""

            val builder = StringBuilder("Damage Ticks: [ ")
            this@Types.iterate(builder, this.damageTicks.iterator())

            builder.toString()
        }

        private fun <V> iterate(
            builder: StringBuilder,
            iterator: Iterator<Map.Entry<State, V>>
        ) {
            while (iterator.hasNext()) {
                val (key, _) = iterator.next()

                builder.append(key.name)
                if (iterator.hasNext()) builder.append(", ") else builder.append(" ]")
            }
        }
    }
}
