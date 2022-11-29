package dev.racci.terix.api.events.abilities

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.terix.api.data.Cooldown
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.abilities.keybind.TogglingKeybindAbility
import dev.racci.terix.api.origins.abilities.keybind.TriggeringKeybindAbility
import org.bukkit.event.HandlerList

/**
 * Called when an ability is activated.
 * This ability can be of the type [TogglingKeybindAbility] or [TriggeringKeybindAbility].
 *
 * @param ability The ability that was activated.
 */
public class KeybindAbilityActivateEvent internal constructor(
    ability: KeybindAbility
) : AbilityEvent<KeybindAbility>(ability, true) {
    public val failedActivation: Boolean
        get() = ability.cooldown == Cooldown.NONE

    public companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList(): HandlerList = super.getHandlerList()
    }
}
