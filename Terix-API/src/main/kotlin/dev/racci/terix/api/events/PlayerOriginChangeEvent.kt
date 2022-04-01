package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.terix.api.origins.AbstractOrigin
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerOriginChangeEvent(
    player: Player,
    val preOrigin: AbstractOrigin,
    val newOrigin: AbstractOrigin
) : KPlayerEvent(player, true) {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList = KEvent.handlerMap[PlayerOriginChangeEvent::class]
    }
}
