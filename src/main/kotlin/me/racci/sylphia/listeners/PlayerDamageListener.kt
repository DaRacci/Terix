package me.racci.sylphia.listeners

import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.originManager
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent

class PlayerDamageListener : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onOriginDamageChange(event: EntityDamageEvent) {
        if(event.isCancelled || event.entity !is Player) return
        val player = event.entity as Player
        val origin = player.currentOrigin
        when(event.cause) {
            EntityDamageEvent.DamageCause.FALL -> {
                if (origin?.enable?.fall == true) {
                    if (origin.damage.fall == 0) {
                        event.isCancelled = true
                        return
                    }
                    event.damage = event.damage * (origin.damage.fall / 100)
                }
            }
            EntityDamageEvent.DamageCause.LAVA, EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK, EntityDamageEvent.DamageCause.HOT_FLOOR -> {
                if (origin?.enable?.lava == true) {
                    if (origin.damage.lava == 0) {
                        player.fireTicks = 0
                        event.isCancelled = true
                        return
                    }
                    event.damage = event.damage * (origin.damage.lava / 100)
                }
            }
            else -> return
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onCombustEvent(event: EntityCombustEvent) {
        if (event.entity is Player) {
            val origin = (event.entity as Player).currentOrigin ?: return
            if (origin.enable.lava && origin.damage.lava == 0) event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDamage(event: EntityDamageEvent) {
        if(event.isCancelled || event.finalDamage == 0.0) return
        if(event.entity is Player) {
            val player = event.entity as Player
            player.world.playSound(player.location, originManager.getOrigin(player.uniqueId)?.sound?.hurtSound?:Sound.ENTITY_PLAYER_HURT, 1f, 1f)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        if(event.isCancelled) return
        val player = event.entity
//        player.world.playSound(player.location, originManager.getOrigin(player.uniqueId)?.deathSound?:_Sound.ENTITY_PLAYER_DEATH, 1f, 1f)
        event.deathSound = originManager.getOrigin(player.uniqueId)?.sound?.hurtSound ?: Sound.ENTITY_PLAYER_DEATH
    }

}