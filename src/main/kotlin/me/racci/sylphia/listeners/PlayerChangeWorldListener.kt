package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.utils.strings.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerChangedWorldEvent

class PlayerChangeWorldListener(private val plugin: Sylphia): Listener {

    private val originManager: OriginManager = plugin.originManager

    @EventHandler(priority = EventPriority.NORMAL)
    fun onChangeWorld(event: PlayerChangedWorldEvent) {
        skeduleAsync(plugin) {
            if(originManager.getOrigin(event.player.uniqueId) == null) return@skeduleAsync
            originManager.refreshAll(event.player)
        }
        event.player.playSound(event.player.location, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f)
        event.player.sendTitle(colour("&cRelease.."), colour("&4You feel power flowing through you."), 10, 60, 10)
    }
}