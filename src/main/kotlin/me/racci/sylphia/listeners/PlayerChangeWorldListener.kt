package me.racci.sylphia.listeners

import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent

class PlayerChangeWorldListener(plugin: Sylphia): Listener {

    private val originManager: OriginManager = plugin.originManager!!

    @EventHandler(priority = EventPriority.NORMAL)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        originManager.refresh(event.player)
    }

}