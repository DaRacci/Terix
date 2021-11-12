package me.racci.sylphia.listeners

import com.github.shynixn.mccoroutine.asyncDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.sylphia.Sylphia
import me.racci.sylphia.Sylphia.Companion.storageManager
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority

class PlayerJoinLeaveListener : KotlinListener {

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun onJoin(event: org.bukkit.event.player.PlayerJoinEvent) = withContext(Sylphia.instance.asyncDispatcher) {
        val player = event.player
        storageManager.load(player.uniqueId)
        delay(500)
        if(OriginManager.getOrigin(player) == null) return@withContext
        val origin = player.currentOrigin ?: return@withContext
        OriginManager.refreshAll(player, origin)
        if(origin.enable.sun) SunLightRunnable.burnablePlayers.add(event.player)
        if(origin.enable.rain) RainRunnable.rainablePlayers.add(event.player)
        if(origin.enable.water) WaterRunnable.waterablePlayers.add(event.player)

    }

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun onQuit(event: org.bukkit.event.player.PlayerQuitEvent) = withContext(Sylphia.instance.asyncDispatcher) {
        val burnablePlayers = SunLightRunnable.burnablePlayers
        val rainablePlayers = RainRunnable.rainablePlayers
        val waterablePlayers = WaterRunnable.waterablePlayers
        if(burnablePlayers.contains(event.player)) burnablePlayers.remove(event.player)
        if(rainablePlayers.contains(event.player)) rainablePlayers.remove(event.player)
        if(waterablePlayers.contains(event.player)) waterablePlayers.remove(event.player)
        storageManager.save(event.player.uniqueId, true)
    }
}