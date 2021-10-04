package me.racci.sylphia.listeners

import me.racci.raccicore.skedule.skeduleAsync
import me.racci.sylphia.originManager
import me.racci.sylphia.plugin
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent

class PlayerConsumeListener : Listener {

    @EventHandler(priority = EventPriority.HIGH)
    fun onConsume(event: PlayerItemConsumeEvent) {
        skeduleAsync(plugin) {
            if (event.isCancelled || originManager.getOrigin(event.player.uniqueId) == null) {
                return@skeduleAsync
            }
            if (event.item.type == Material.MILK_BUCKET) {
                waitFor(20)
                originManager.refreshAll(event.player)
            }
        }
    }

}