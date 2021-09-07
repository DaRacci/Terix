@file:Suppress("unused")
@file:JvmName("PlayerMoveEvent")
package me.racci.sylphia.listeners

import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.schedule
import me.racci.raccilib.events.PlayerEnterLiquidEvent
import me.racci.raccilib.events.PlayerExitLiquidEvent
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginValue
import me.racci.sylphia.utils.getCondition
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitScheduler

class PlayerMoveEvent(private val plugin: Sylphia): Listener {

    private val scheduler: BukkitScheduler = Bukkit.getScheduler()


    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerEnterLiquid(event: PlayerEnterLiquidEvent) {
        scheduler.schedule(plugin, SynchronizationContext.ASYNC) {
            val player = event.player
            val liquidType = event.liquidType
            val condition: OriginValue = getCondition(player)
            player.sendMessage(condition.toString())
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerExitLiquid(event: PlayerExitLiquidEvent) {
        scheduler.schedule(plugin, SynchronizationContext.ASYNC) {
            val player = event.player
            val condition: OriginValue = getCondition(player)
            player.sendMessage(condition.toString())
        }
    }


}