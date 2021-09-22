package me.racci.sylphia.listeners

import me.racci.raccilib.skedule.skeduleAsync
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class PlayerJoinLeaveListener(private val plugin: me.racci.sylphia.Sylphia) : org.bukkit.event.Listener {

    private val originManager: me.racci.sylphia.origins.OriginManager = plugin.originManager
    private val playerManager: me.racci.sylphia.data.PlayerManager = plugin.playerManager
    private val storageProvider: me.racci.sylphia.data.storage.StorageProvider = plugin.storageProvider


    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        skeduleAsync(plugin) {
            val player = event.player
            if (playerManager.getPlayerData(player) == null) {
                storageProvider.load(player)
                waitFor(10)
            }
            if(originManager.getOrigin(player) == null) return@skeduleAsync
            val origin = player.currentOrigin ?: return@skeduleAsync
            originManager.refreshAll(player, origin)
            if(origin.enableSun) SunLightRunnable.burnablePlayers.add(event.player)
            if(origin.enableRain) RainRunnable.rainablePlayers.add(event.player)
            if(origin.enableWater) WaterRunnable.waterablePlayers.add(event.player)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
        skeduleAsync(plugin) {
            val burnablePlayers = SunLightRunnable.burnablePlayers
            val rainablePlayers = RainRunnable.rainablePlayers
            val waterablePlayers = WaterRunnable.waterablePlayers
            if(burnablePlayers.contains(event.player)) burnablePlayers.remove(event.player)
            if(rainablePlayers.contains(event.player)) rainablePlayers.remove(event.player)
            if(waterablePlayers.contains(event.player)) waterablePlayers.remove(event.player)
            storageProvider.save(event.player, true)
        }
    }
}