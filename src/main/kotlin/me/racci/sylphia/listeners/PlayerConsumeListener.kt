@file:Suppress("unused")
@file:JvmName("PlayerConsumeEvent")
package me.racci.sylphia.listeners

import me.racci.raccilib.skedule.SynchronizationContext
import me.racci.raccilib.skedule.skeduleAsync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent

class PlayerConsumeListener(private val plugin: Sylphia) : Listener {

    private var originManager: OriginManager = plugin.originManager

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