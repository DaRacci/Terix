package me.racci.sylphia.runnables

import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import org.bukkit.entity.Player

class WaterRunnable(private val manager: org.bukkit.plugin.PluginManager) : org.bukkit.scheduler.BukkitRunnable() {

    companion object {
        val waterablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in waterablePlayers) {
            if(player.isInWaterOrBubbleColumn) {
                me.racci.raccilib.skedule.skeduleSync(me.racci.sylphia.Sylphia.instance) {
                    manager.callEvent(me.racci.sylphia.events.BurnInSunLightEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).waterAmount / 100)))
                }
            }
        }
    }

}