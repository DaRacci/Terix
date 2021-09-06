package me.racci.sylphia.events

import me.racci.sylphia.origins.objects.Origin
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class OriginChangeEvent(val player: Player, val oldOrigin: Origin, val newOrigin: Origin) : Event(true), Cancellable {

    private var cancelled = false
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled(value: Boolean) {
        cancelled = value
    }

    companion object {
        private val handlers = HandlerList()
    }
}