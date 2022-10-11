package dev.racci.terix.api.origins.enums

import dev.racci.minix.api.events.keybind.ComboEvent
import dev.racci.minix.api.events.keybind.PlayerDoublePrimaryEvent
import dev.racci.minix.api.events.keybind.PlayerDoubleSecondaryEvent
import dev.racci.minix.api.events.keybind.PlayerPrimaryEvent
import dev.racci.minix.api.events.keybind.PlayerSecondaryEvent
import dev.racci.minix.api.events.keybind.PlayerSneakDoubleOffhandEvent
import dev.racci.minix.api.events.keybind.PlayerSneakDoublePrimaryEvent
import dev.racci.minix.api.events.keybind.PlayerSneakDoubleSecondaryEvent
import dev.racci.minix.api.events.keybind.PlayerSneakOffhandEvent
import dev.racci.minix.api.events.keybind.PlayerSneakPrimaryEvent
import dev.racci.minix.api.events.keybind.PlayerSneakSecondaryEvent
import kotlin.reflect.KClass

enum class KeyBinding(val event: KClass<out ComboEvent>) {
    DOUBLE_OFFHAND(dev.racci.minix.api.events.keybind.PlayerDoubleOffhandEvent::class),

    // DOUBLE_SNEAK(), // TODO: Event
    DOUBLE_RIGHT_CLICK(PlayerDoubleSecondaryEvent::class),
    DOUBLE_LEFT_CLICK(PlayerDoublePrimaryEvent::class),

    SINGLE_OFFHAND(dev.racci.minix.api.events.keybind.PlayerOffhandEvent::class),

    // SINGLE_SNEAK, // TODO: Event
    SINGLE_RIGHT_CLICK(PlayerSecondaryEvent::class),
    SINGLE_LEFT_CLICK(PlayerPrimaryEvent::class),

    SNEAK_OFFHAND(PlayerSneakOffhandEvent::class),
    SNEAK_RIGHT_CLICK(PlayerSneakSecondaryEvent::class),
    SNEAK_LEFT_CLICK(PlayerSneakPrimaryEvent::class),

    SNEAK_DOUBLE_OFFHAND(PlayerSneakDoubleOffhandEvent::class),
    SNEAK_DOUBLE_RIGHT_CLICK(PlayerSneakDoubleSecondaryEvent::class),
    SNEAK_DOUBLE_LEFT_CLICK(PlayerSneakDoublePrimaryEvent::class);

    companion object {
        fun fromEvent(clazz: KClass<out ComboEvent>): KeyBinding = values().find { it.event === clazz } ?: throw IllegalArgumentException("Unknown event class $clazz")

        inline fun <reified T : ComboEvent> fromEvent(): KeyBinding = fromEvent(T::class)
    }
}
