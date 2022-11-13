package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList

/**
 * Player ability deactivate event
 *
 * @param player The player who activated the ability
 * @param ability The ability that was deactivated.
 * @param cancellable If this event can be cancelled.
 */
public class PlayerAbilityDeactivateEvent(
    player: Player,
    ability: KeybindAbility,
    public val cancellable: Boolean
) : PlayerAbilityEvent(player, ability) {
    override fun isCancelled(): Boolean {
        return cancellable && super.isCancelled()
    }

    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
