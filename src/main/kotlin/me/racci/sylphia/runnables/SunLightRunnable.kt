package me.racci.sylphia.runnables

import me.racci.raccicore.runnables.KotlinRunnable
import me.racci.raccicore.utils.pm
import me.racci.sylphia.Sylphia
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.extensions.PlayerExtension.isDay
import org.bukkit.entity.Player

class SunLightRunnable(plugin: Sylphia) : KotlinRunnable(plugin, true) {

    companion object {
        val burnablePlayers = HashSet<Player>()
    }

    override fun run() {
        for(player in burnablePlayers) {
            if(player.isDay && !player.isInWaterOrRainOrBubbleColumn && player.location.block.temperature > 0.0 && player.location.block.lightFromSky.toInt() == 15) {
                me.racci.raccicore.skedule.skeduleSync(plugin) {
                    pm.callEvent(me.racci.sylphia.events.BurnInSunLightEvent(player, (2.0 * (player.currentOrigin ?: return@skeduleSync).damage.sun / 100)))
                }
            }
        }
    }
}