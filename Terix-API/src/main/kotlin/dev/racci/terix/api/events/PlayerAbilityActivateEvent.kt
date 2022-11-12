package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

public class PlayerAbilityActivateEvent(
    player: Player,
    ability: KeybindAbility
) : PlayerAbilityEvent(player, ability) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
