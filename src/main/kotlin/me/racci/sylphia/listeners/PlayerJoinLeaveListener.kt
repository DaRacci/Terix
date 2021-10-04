package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.originManager
import me.racci.sylphia.playerManager
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import me.racci.sylphia.storageManager
import me.racci.sylphia.plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class PlayerJoinLeaveListener : org.bukkit.event.Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
        skeduleAsync(plugin) {
            val player = event.player
            if (playerManager.getPlayerData(player) == null) {
                storageManager.load(player)
                waitFor(10)
            }
            if(originManager.getOrigin(player) == null) return@skeduleAsync
            val origin = player.currentOrigin ?: return@skeduleAsync
            originManager.refreshAll(player, origin)
            if(origin.enable.sun) SunLightRunnable.burnablePlayers.add(event.player)
            if(origin.enable.rain) RainRunnable.rainablePlayers.add(event.player)
            if(origin.enable.water) WaterRunnable.waterablePlayers.add(event.player)
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
            storageManager.save(event.player, true)
        }
    }
}