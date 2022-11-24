package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.terix.api.data.Cooldown
import dev.racci.terix.api.events.abilities.KeybindAbilityActivateEvent
import dev.racci.terix.api.extensions.onSuccess
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.sentryScoped
import org.bukkit.event.Event

public abstract class TriggeringKeybindAbility : KeybindAbility() {

    /** Called when the abilities keybind has been triggered. */
    public abstract suspend fun handleTrigger()

    override suspend fun handleKeybind(event: Event) {
        when {
            OriginHelper.shouldIgnorePlayer(abilityPlayer) -> return
            this.shouldIgnoreKeybind(event) -> return
            this.cooldown.notExpired() -> return
            else -> KeybindAbilityActivateEvent(this).onSuccess {
                this.cooldown = Cooldown.of(this.cooldownDuration)
                sentryScoped(
                    abilityPlayer,
                    SCOPE,
                    "$name.trigger",
                    context = plugin.asyncDispatcher,
                    block = this::handleTrigger
                )
            }
        }
    }

    private companion object {
        const val SCOPE: String = "origin.abilities.triggering"
    }
}
