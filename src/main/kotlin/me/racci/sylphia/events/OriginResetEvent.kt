@file:Suppress("unused")
@file:JvmName("OriginResetEvent")
package me.racci.sylphia.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class OriginResetEvent(val player: Player) : Event(true), Cancellable {
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