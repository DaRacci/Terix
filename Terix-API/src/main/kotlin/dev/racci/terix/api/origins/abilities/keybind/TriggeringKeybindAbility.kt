package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.sentryScoped

public abstract class TriggeringKeybindAbility : KeybindAbility() {

    /** Will always equal false unless overridden in the ability for custom behaviour. */
    override val isActivated: Boolean = false

    /** Called when the abilities keybind has been triggered. */
    public abstract suspend fun handleTrigger()

    internal suspend fun trigger() {
        if (OriginHelper.shouldIgnorePlayer(abilityPlayer)) return
        if (!this.cooldownExpired()) return
        if (this.isActivated) return

        PlayerAbilityActivateEvent(abilityPlayer, this).onSuccess {
            this.activatedAt = now()
            sentryScoped(abilityPlayer, SCOPE, "$name.trigger", context = plugin.asyncDispatcher, block = this::handleTrigger)
        }
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.triggering"
    }
}
