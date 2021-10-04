package me.racci.sylphia.runnables

import me.racci.raccicore.utils.pm
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.plugin
import org.bukkit.entity.Player

class RainRunnable : org.bukkit.scheduler.BukkitRunnable() {

    companion object {
        val rainablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in rainablePlayers) {
            if(player.isInRain) {
                me.racci.raccicore.skedule.skeduleSync(plugin) {
                    pm.callEvent(me.racci.sylphia.events.RainDamageEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).damage.rain / 100)))
                }
            }
        }
    }
}