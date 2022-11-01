package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.abilities.Ability
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

public class PlayerAbilityActivateEvent(
    player: Player,
    ability: Ability
) : PlayerAbilityEvent(player, ability) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
