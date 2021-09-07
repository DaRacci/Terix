@file:Suppress("unused")
@file:JvmName("PlayerConsumeEvent")
package me.racci.sylphia.listeners

import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.schedule
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginHandler
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.scheduler.BukkitScheduler

class PlayerConsumeEvent(private val plugin: Sylphia) : Listener {

    private var scheduler: BukkitScheduler = Bukkit.getScheduler()
    private var originHandler: OriginHandler = plugin.originHandler!!

    @EventHandler(priority = EventPriority.HIGH)
    fun onConsume(event: PlayerItemConsumeEvent) {
        scheduler.schedule(plugin,  SynchronizationContext.ASYNC) {
            if (event.isCancelled) {
                return@schedule
            }

            val player = event.player
            if (event.item.type == Material.MILK_BUCKET) {
                waitFor(20)
//                originHandler.setTest(player)
            }
        }
    }

}