package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.events.PlayerAbilityDeactivateEvent
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.sentryScoped
import org.bukkit.event.Event

public abstract class TogglingKeybindAbility : KeybindAbility() {

    public var isActivated: Boolean = false; private set

    /** Called when the abilities keybind has been toggled on. */
    public abstract suspend fun handleActivation()

    /** Called when the ability is deactivated. */
    public abstract suspend fun handleDeactivation()

    final override suspend fun handleKeybind(event: Event) {
        when {
            OriginHelper.shouldIgnorePlayer(abilityPlayer) || !this.cooldownExpired() -> return
            this.isActivated -> this.deactivate(true)
            else -> this.activate()
        }
    }

    private suspend fun activate(): Boolean = PlayerAbilityActivateEvent(abilityPlayer, this).onSuccess {
        this.isActivated = true
        this.addToPersistentData()
        this.activatedAt = now()
        sentryScoped(abilityPlayer, SCOPE, "$name.activate", context = plugin.asyncDispatcher, block = this::handleActivation)
    }

    private suspend fun deactivate(cancellable: Boolean): Boolean = PlayerAbilityDeactivateEvent(abilityPlayer, this, cancellable).onSuccess {
        this.isActivated = false
        this.removeFromPersistentData()
        sentryScoped(abilityPlayer, SCOPE, "$name.deactivate", context = plugin.asyncDispatcher, block = this::handleDeactivation)
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.toggling"
    }
}
