package dev.racci.terix.api.origins.abilities.keybind

import kotlin.time.Duration

public abstract class TriggeringChargeKeybindAbility(override val chargeTime: Duration) : ChargeKeybindAbility() {

    final override var charge: Float by observable { _, old, new ->
        when {
            old == 0F && new > 0F -> handleChargeStart()
            new >= 1F -> handleActivateInternal()
            old == 1F && new == 0F -> return@observable
            old > new -> handleChargeDecrease(new)
            old < new -> handleChargeIncrease(new)
        }
    }

    /**
     * Called when the ability has reached full charge and is triggered.
     */
    public abstract suspend fun handleActivation()

    private suspend fun handleActivateInternal() {
        handleActivation()
        charge = 0F
    }
}
