package me.racci.sylphia.listeners

import me.racci.raccicore.utils.extensions.KotlinListener
import org.bukkit.event.EventHandler

class RunnableListener : KotlinListener {

    @EventHandler
    fun onBurnInSunlight(event: me.racci.sylphia.events.BurnInSunLightEvent) {
        if(event.isCancelled) return
        val player = event.player
        player.damage(event.damage)
        player.world.playSound(player.location, org.bukkit.Sound.ENTITY_GENERIC_BURN, 1f, 1f)
    }

    @EventHandler
    fun onRainDamage(event: me.racci.sylphia.events.RainDamageEvent) {
        if(event.isCancelled) return
        event.player.damage(event.damage)
    }

    @EventHandler
    fun onWaterDamage(event: me.racci.sylphia.events.WaterDamageEvent) {
        if(event.isCancelled) return
        event.player.damage(event.damage)
    }



}