package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.sylphia.originManager
import me.racci.sylphia.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerRespawnListener : Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onRespawn(event: PlayerRespawnEvent) {
        if(originManager.getOrigin(event.player.uniqueId) == null) return
        skeduleAsync(plugin) {
            waitFor(10)
            originManager.removeAll(event.player)
        }
    }

}