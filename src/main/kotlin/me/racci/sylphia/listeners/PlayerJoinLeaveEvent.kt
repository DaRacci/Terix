@file:Suppress("unused")
@file:JvmName("PlayerJoinLeaveEvent")
package me.racci.sylphia.listeners

import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.schedule
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.data.storage.StorageProvider
import me.racci.sylphia.origins.OriginManager
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitScheduler

class PlayerJoinLeaveEvent(private val plugin: Sylphia) : Listener {

    private val scheduler: BukkitScheduler = Bukkit.getScheduler()
    private val originManager: OriginManager = plugin.originManager!!
    private val playerManager: PlayerManager = plugin.playerManager!!
    private val storageProvider: StorageProvider = plugin.storageProvider!!


    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        scheduler.schedule(plugin, SynchronizationContext.SYNC) {
            val player = event.player
            if (playerManager.getPlayerData(player) == null) {
                storageProvider.load(player)
                waitFor(15)
                originManager

            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        scheduler.schedule(plugin, SynchronizationContext.SYNC) {
            storageProvider.save(event.player, true)
        }
    }
}