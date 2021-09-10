@file:Suppress("unused")
@file:JvmName("PlayerJoinLeaveEvent")
package me.racci.sylphia.listeners

import me.racci.raccilib.skedule.SynchronizationContext
import me.racci.raccilib.skedule.skeduleAsync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.data.storage.StorageProvider
import me.racci.sylphia.origins.OriginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

class PlayerJoinLeaveListener(private val plugin: Sylphia) : Listener {

    private val originManager: OriginManager = plugin.originManager!!
    private val playerManager: PlayerManager = plugin.playerManager!!
    private val storageProvider: StorageProvider = plugin.storageProvider!!


    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: PlayerJoinEvent) {
        skeduleAsync(plugin) {
            val player = event.player
            if (playerManager.getPlayerData(player) == null) {
                storageProvider.load(player)
                waitFor(10)
            }
            switchContext(SynchronizationContext.SYNC)
            originManager.refresh(player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event: PlayerQuitEvent) {
        skeduleAsync(plugin) {
            storageProvider.save(event.player, true)
        }
    }
}