package me.racci.sylphia.runnables

import me.racci.raccicore.runnables.KotlinRunnable
import me.racci.raccicore.utils.pm
import me.racci.sylphia.Sylphia
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import org.bukkit.entity.Player

class WaterRunnable(plugin: Sylphia) : KotlinRunnable(plugin, true) {

    companion object {
        val waterablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in waterablePlayers) {
            if(player.isInWaterOrBubbleColumn) {
                me.racci.raccicore.skedule.skeduleSync(plugin) {
                    pm.callEvent(me.racci.sylphia.events.BurnInSunLightEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).damage.water / 100)))
                }
            }
        }
    }

}