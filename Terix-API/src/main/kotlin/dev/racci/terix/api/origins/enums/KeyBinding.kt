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

enum class KeyBinding(val event: KClass<out AbstractComboEvent>) {
    DOUBLE_OFFHAND(PlayerDoubleOffhandEvent::class),

    // DOUBLE_SNEAK(), // TODO: Event
    DOUBLE_RIGHT_CLICK(PlayerDoubleRightClickEvent::class),
    DOUBLE_LEFT_CLICK(PlayerDoubleLeftClickEvent::class),

    SINGLE_OFFHAND(PlayerOffhandEvent::class),

    // SINGLE_SNEAK, // TODO: Event
    SINGLE_RIGHT_CLICK(PlayerRightClickEvent::class),
    SINGLE_LEFT_CLICK(PlayerLeftClickEvent::class),

    SNEAK_OFFHAND(PlayerShiftOffhandEvent::class),
    SNEAK_RIGHT_CLICK(PlayerShiftRightClickEvent::class),
    SNEAK_LEFT_CLICK(PlayerShiftLeftClickEvent::class),

    SNEAK_DOUBLE_OFFHAND(PlayerShiftDoubleOffhandEvent::class),
    SNEAK_DOUBLE_RIGHT_CLICK(PlayerShiftDoubleRightClickEvent::class),
    SNEAK_DOUBLE_LEFT_CLICK(PlayerShiftDoubleLeftClickEvent::class);

    companion object {
        fun fromEvent(clazz: KClass<out AbstractComboEvent>): KeyBinding = values().find { it.event === clazz } ?: throw IllegalArgumentException("Unknown event class $clazz")

        inline fun <reified T : AbstractComboEvent> fromEvent(): KeyBinding = fromEvent(T::class)
    }
}
