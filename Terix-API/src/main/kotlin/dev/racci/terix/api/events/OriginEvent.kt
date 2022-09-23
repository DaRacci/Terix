package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.utils.kotlin.companionParent
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

sealed class OriginEvent(
    player: Player,
    val origin: Origin
) : KPlayerEvent(player, true) {

    open class CompanionEventHandler {
        init {
            if (!this::class.isCompanion) error("CompanionEventHandler must be a companion object.")
            if (!this::class.companionParent!!.isInstance(Event::class)) error("CompanionEventHandler must be a companion object of an event.")
        }

        open fun getHandlerList(): HandlerList = KEvent.handlerMap[this::class.companionParent.castOrThrow()]
    }
}
