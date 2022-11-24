package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.terix.api.data.Cooldown
import dev.racci.terix.api.events.abilities.KeybindAbilityActivateEvent
import dev.racci.terix.api.events.abilities.KeybindAbilityDeactivateEvent
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
            OriginHelper.shouldIgnorePlayer(abilityPlayer) -> return
            this.shouldIgnoreKeybind(event) -> return
            this.isActivated -> this.deactivate(true)
            this.cooldown.notExpired() -> return
            else -> this.activate()
        }
    }

    final override suspend fun handleInternalLost() {
        if (this.isActivated) this.deactivate(false)
    }

    private suspend fun activate(): Boolean = KeybindAbilityActivateEvent(this).onSuccess {
        this.isActivated = true
        this.addToPersistentData()
        this.cooldown = Cooldown.of(this.cooldownDuration)
        sentryScoped(abilityPlayer, SCOPE, "$name.activate", context = plugin.asyncDispatcher, block = this::handleActivation)
    }

    private suspend fun deactivate(cancellable: Boolean): Boolean = KeybindAbilityDeactivateEvent(this, cancellable).onSuccess {
        this.isActivated = false
        this.removeFromPersistentData()
        sentryScoped(abilityPlayer, SCOPE, "$name.deactivate", context = plugin.asyncDispatcher, block = this::handleDeactivation)
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.toggling"
    }
}
