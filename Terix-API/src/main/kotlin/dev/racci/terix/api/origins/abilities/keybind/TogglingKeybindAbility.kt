package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.events.PlayerAbilityDeactivateEvent
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.sentryScoped

public abstract class TogglingKeybindAbility : KeybindAbility() {

    public override var isActivated: Boolean = false

    /** Called when the abilities keybind has been toggled on. */
    public abstract suspend fun handleActivation()

    /** Called when the ability is deactivated. */
    public abstract suspend fun handleDeactivation()

    /**
     * Attempts to toggle this ability.
     *
     * @return If the ability is on cooldown, this will return null. Otherwise, it will return true.
     */
    public suspend fun toggle(): Boolean? = when {
        OriginHelper.shouldIgnorePlayer(abilityPlayer) -> false
        this.activatedAt != null && this.activatedAt!! + cooldown > now() -> null
        this.isActivated -> this.deactivate(true)
        else -> this.activate()
    }

    internal suspend fun activate(): Boolean = PlayerAbilityActivateEvent(abilityPlayer, this).onSuccess {
        this.isActivated = true
        this.addToPersistentData()
        this.activatedAt = now()
        sentryScoped(abilityPlayer, SCOPE, "$name.activate", context = plugin.asyncDispatcher, block = this::handleActivation)
    }

    internal suspend fun deactivate(cancellable: Boolean): Boolean = PlayerAbilityDeactivateEvent(abilityPlayer, this, cancellable).onSuccess {
        this.isActivated = false
        this.removeFromPersistentData()
        sentryScoped(abilityPlayer, SCOPE, "$name.deactivate", context = plugin.asyncDispatcher, block = this::handleDeactivation)
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.toggling"
    }
}
