package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerRespawnEvent

class PlayerRespawnListener : KotlinListener {

    @EventHandler(priority = EventPriority.NORMAL)
    fun onRespawn(event: PlayerRespawnEvent) {
        if(OriginManager.getOrigin(event.player.uniqueId) == null) return
        skeduleAsync(Sylphia.instance) {
            waitFor(10)
            OriginManager.removeAll(event.player)
        }
    }

}