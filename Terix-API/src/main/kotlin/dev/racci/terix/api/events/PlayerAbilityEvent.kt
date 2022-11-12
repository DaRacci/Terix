package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

public sealed class PlayerAbilityEvent(
    player: Player,
    public val ability: KeybindAbility
) : KPlayerEvent(player, true) {
    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
