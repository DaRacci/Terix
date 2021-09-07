@file:Suppress("unused")
@file:JvmName("DataLoadEvent")
package me.racci.sylphia.events

import me.racci.sylphia.data.PlayerData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class DataLoadEvent(val playerData: PlayerData) : Event(true) {
    override fun getHandlers(): HandlerList {
        return Companion.handlers
    }

    companion object {
        private val handlers = HandlerList()
    }
}