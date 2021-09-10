@file:Suppress("unused")
@file:JvmName("PlayerRespawnEvent")
package me.racci.sylphia.listeners

import me.racci.raccilib.skedule.skeduleSync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerRespawnListener(private val plugin: Sylphia): Listener {

    private val originManager: OriginManager = plugin.originManager!!

    @EventHandler(priority = EventPriority.NORMAL)
    fun onRespawn(event: PlayerRespawnEvent) {
        skeduleSync(plugin) {
            waitFor(10)
            originManager.refresh(event.player)
        }
    }

}