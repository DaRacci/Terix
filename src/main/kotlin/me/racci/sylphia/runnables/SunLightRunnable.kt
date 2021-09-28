package me.racci.sylphia.runnables

import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.extensions.PlayerExtension.isDay
import org.bukkit.entity.Player

class SunLightRunnable(private val manager: org.bukkit.plugin.PluginManager) : org.bukkit.scheduler.BukkitRunnable() {

    companion object {
        val burnablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in burnablePlayers) {
            if(player.isDay && !player.isInWaterOrRainOrBubbleColumn && player.location.block.temperature > 0.0 && player.location.block.lightFromSky.toInt() == 15) {
                me.racci.raccicore.skedule.skeduleSync(me.racci.sylphia.Sylphia.instance) {
                    manager.callEvent(me.racci.sylphia.events.BurnInSunLightEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).sunAmount / 100)))
                }
            }
        }
    }
}