package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.terix.api.origins.abilities.Ability
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

sealed class PlayerAbilityEvent(
    player: Player,
    val ability: Ability
) : KPlayerEvent(player, true) {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList = KEvent.handlerMap[PlayerOriginChangeEvent::class]
    }
}
