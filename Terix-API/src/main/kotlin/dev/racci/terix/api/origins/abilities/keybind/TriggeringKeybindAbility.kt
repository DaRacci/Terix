package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.terix.api.data.Cooldown
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.sentryScoped
import org.bukkit.event.Event

public abstract class TriggeringKeybindAbility : KeybindAbility() {

    /** Called when the abilities keybind has been triggered. */
    public abstract suspend fun handleTrigger()

    override suspend fun handleKeybind(event: Event) {
        if (OriginHelper.shouldIgnorePlayer(abilityPlayer)) return
        if (this.cooldown.notExpired()) return

        PlayerAbilityActivateEvent(abilityPlayer, this).onSuccess {
            this.cooldown = Cooldown.of(this.cooldownDuration)
            sentryScoped(abilityPlayer, SCOPE, "$name.trigger", context = plugin.asyncDispatcher, block = this::handleTrigger)
        }
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.triggering"
    }
}
