@file:Suppress("unused")
@file:JvmName("PlayerMoveEvent")
package me.racci.sylphia.listeners

import me.racci.raccilib.events.PlayerEnterLiquidEvent
import me.racci.raccilib.events.PlayerExitLiquidEvent
import me.racci.raccilib.skedule.SynchronizationContext
import me.racci.raccilib.skedule.skeduleAsync
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.utils.getCondition
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class PlayerMoveListener(private val plugin: Sylphia): Listener {

    private val originManager: OriginManager = plugin.originManager!!

    @EventHandler(priority = EventPriority.NORMAL)
    fun onEnterLiquid(event: PlayerEnterLiquidEvent) {
        skeduleAsync(plugin) {
            val player = event.player
            val condition = getCondition(player)
            val origin = originManager.getOrigin(player)!!
            player.sendMessage(condition.toString())
            switchContext(SynchronizationContext.SYNC)
            originManager.refresh(player, origin, condition)
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onExitLiquid(event: PlayerExitLiquidEvent) {
        skeduleAsync(plugin) {
            val player = event.player
            val condition = getCondition(player)
            val origin = originManager.getOrigin(player)!!
            player.sendMessage(condition.toString())
            switchContext(SynchronizationContext.SYNC)
            originManager.refresh(player, origin, condition)
        }
    }


}