package me.racci.sylphia.runnables

import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import org.bukkit.entity.Player

class RainRunnable(private val manager: org.bukkit.plugin.PluginManager) : org.bukkit.scheduler.BukkitRunnable() {

    companion object {
        val rainablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in rainablePlayers) {
            if(player.isInRain) {
                me.racci.raccicore.skedule.skeduleSync(me.racci.sylphia.Sylphia.instance) {
                    manager.callEvent(me.racci.sylphia.events.RainDamageEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).rainAmount / 100)))
                }
            }
        }
    }
}