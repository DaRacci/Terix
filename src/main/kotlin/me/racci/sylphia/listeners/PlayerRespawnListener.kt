package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerRespawnListener(private val plugin: Sylphia): Listener {

    private val originManager: OriginManager = plugin.originManager

    @EventHandler(priority = EventPriority.NORMAL)
    fun onRespawn(event: PlayerRespawnEvent) {
        if(originManager.getOrigin(event.player.uniqueId) == null) return
        skeduleAsync(plugin) {
            waitFor(10)
            originManager.refreshAll(event.player)
        }
    }

}