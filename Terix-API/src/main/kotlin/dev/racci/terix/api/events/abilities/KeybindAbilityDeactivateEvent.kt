package dev.racci.terix.api.events.abilities

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.origins.abilities.keybind.TogglingKeybindAbility
import org.bukkit.event.HandlerList

/**
 * Called when a [TogglingKeybindAbility] is deactivated.
 *
 * @param ability The ability that was deactivated.
 * @param cancellable If this event can be cancelled.
 */
public class KeybindAbilityDeactivateEvent internal constructor(
    ability: TogglingKeybindAbility,
    public val cancellable: Boolean
) : AbilityEvent<TogglingKeybindAbility>(ability, true) {
    override fun isCancelled(): Boolean {
        return cancellable && super.isCancelled()
    }

    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
