package dev.racci.terix.api.events

import dev.racci.minix.api.events.KEvent
import dev.racci.terix.api.origins.abilities.Ability
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

class PlayerAbilityActivateEvent(
    player: Player,
    ability: Ability
) : PlayerAbilityEvent(player, ability) {
    companion object {
        @JvmStatic
        fun getHandlerList(): HandlerList = KEvent.handlerMap[PlayerOriginChangeEvent::class]
    }
}
