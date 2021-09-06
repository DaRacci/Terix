@file:JvmName("PlayerMoveEvent")
package me.racci.sylphia.listeners

import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.schedule
import me.racci.raccilib.utils.OriginConditionException
import me.racci.raccilib.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.objects.Origin.OriginValue
import me.racci.sylphia.utils.eventlistners.PlayerEnterLiquidEvent
import me.racci.sylphia.utils.eventlistners.PlayerExitLiquidEvent
import org.bukkit.Bukkit
import org.bukkit.World.Environment.*
import org.bukkit.entity.Player
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
            val condition: OriginValue = getCondition(player, liquidType)
            player.sendMessage(condition.toString())
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    fun onPlayerExitLiquid(event: PlayerExitLiquidEvent) {
        scheduler.schedule(plugin, SynchronizationContext.ASYNC) {
            val player = event.player
            val condition: OriginValue = getCondition(player, event.liquidType)
            player.sendMessage(condition.toString())
        }
    }

    /**
     * @param [player] The player for this condition
     * @param [var1] The current liquid type; 0 = nothing, 1 = water, 2 = lava
     * @return [OriginValue] This is the condition set for which effects to apply
     */
    private fun getCondition(player: Player, var1: Int): OriginValue {
        var var2 = when(player.world.environment) {
            NORMAL -> {
                when(WorldTime.isDay(player) ) {
                    true -> "OD"
                    false -> "ON"
                }
            }
            NETHER -> "N"
            THE_END -> "E"
            else -> throw OriginConditionException("Unexpected value: " + player.world.environment)
        }
        var2 = when(var1) {
            1 -> var2 + "W"
            2 -> var2 + "L"
            else -> var2
        }
        return OriginValue.valueOf(var2)
    }
}