package dev.racci.terix.api.origins.enums

import dev.racci.minix.api.events.AbstractComboEvent
import dev.racci.minix.api.events.PlayerDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerLeftClickEvent
import dev.racci.minix.api.events.PlayerOffhandEvent
import dev.racci.minix.api.events.PlayerRightClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftDoubleOffhandEvent
import dev.racci.minix.api.events.PlayerShiftDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerShiftLeftClickEvent
import dev.racci.minix.api.events.PlayerShiftOffhandEvent
import dev.racci.minix.api.events.PlayerShiftRightClickEvent
import kotlin.reflect.KClass

enum class KeyBinding {
    DOUBLE_OFFHAND,
    DOUBLE_SNEAK, // TODO: Event
    DOUBLE_RIGHT_CLICK,
    DOUBLE_LEFT_CLICK,

    SINGLE_OFFHAND,
    SINGLE_SNEAK, // TODO: Event
    SINGLE_RIGHT_CLICK,
    SINGLE_LEFT_CLICK,

    SNEAK_OFFHAND,
    SNEAK_RIGHT_CLICK,
    SNEAK_LEFT_CLICK,

    SNEAK_DOUBLE_OFFHAND,
    SNEAK_DOUBLE_RIGHT_CLICK,
    SNEAK_DOUBLE_LEFT_CLICK;

    companion object {
        fun fromEvent(clazz: KClass<out AbstractComboEvent>): KeyBinding = when (clazz) {
            PlayerRightClickEvent::class -> SINGLE_RIGHT_CLICK
            PlayerLeftClickEvent::class -> SINGLE_LEFT_CLICK
            PlayerOffhandEvent::class -> SINGLE_OFFHAND

            PlayerDoubleRightClickEvent::class -> DOUBLE_RIGHT_CLICK
            PlayerDoubleLeftClickEvent::class -> DOUBLE_LEFT_CLICK
            PlayerDoubleOffhandEvent::class -> DOUBLE_OFFHAND

            PlayerShiftRightClickEvent::class -> SNEAK_RIGHT_CLICK
            PlayerShiftLeftClickEvent::class -> SNEAK_LEFT_CLICK
            PlayerShiftOffhandEvent::class -> SNEAK_OFFHAND

            PlayerShiftDoubleRightClickEvent::class -> SNEAK_DOUBLE_RIGHT_CLICK
            PlayerShiftDoubleLeftClickEvent::class -> SNEAK_DOUBLE_LEFT_CLICK
            PlayerShiftDoubleOffhandEvent::class -> SNEAK_DOUBLE_OFFHAND

            else -> throw IllegalArgumentException("Unknown event class $clazz")
        }

        inline fun <reified T : AbstractComboEvent> fromEvent(): KeyBinding = fromEvent(T::class)
    }
}
