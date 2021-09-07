@file:Suppress("unused")
@file:JvmName("PlayerDamageListener")
package me.racci.sylphia.listeners

import me.racci.sylphia.Sylphia
import me.racci.sylphia.origins.OriginHandler
import me.racci.sylphia.origins.OriginValue
import org.bukkit.Sound
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent

class PlayerDamageListener(private val plugin: Sylphia): Listener {

    private val originHandler: OriginHandler = plugin.originHandler!!
    private val originSounds: HashMap<OriginValue, Sound> = HashMap()

    init {
//        originHandler.origins.forEach { origin: Origin -> }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onDamage(event: EntityDamageEvent) {

    }

}