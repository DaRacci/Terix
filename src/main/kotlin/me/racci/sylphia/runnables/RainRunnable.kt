package me.racci.sylphia.runnables

import me.racci.raccicore.runnables.KotlinRunnable
import me.racci.raccicore.utils.pm
import me.racci.sylphia.Sylphia
import me.racci.sylphia.events.RainDamageEvent
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import org.bukkit.entity.Player

class RainRunnable(plugin: Sylphia) : KotlinRunnable(plugin, true) {

    companion object {
        val rainablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in rainablePlayers) {
            if(player.isInRain) {
                me.racci.raccicore.skedule.skeduleSync(plugin) {
                    pm.callEvent(RainDamageEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).damage.rain / 100)))
                }
            }
        }
    }
}