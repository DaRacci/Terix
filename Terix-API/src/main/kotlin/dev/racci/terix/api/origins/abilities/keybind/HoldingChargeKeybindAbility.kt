package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.utils.now

public abstract class HoldingChargeKeybindAbility : ChargeKeybindAbility() {

    final override var charge: Float by observable(
        onOvercharge = {
            activatedAt = now()
            handleChargeFull()
        },
        onChange = { _, old, new ->
            when {
                old == 0F && new > 0F -> handleChargeStart()
                old > new -> handleChargeDecrease(new)
                old < new -> handleChargeIncrease(new)
            }
        }
    )

    /**
     * Called each tick while the charge has reached full and is still being held.
     */
    protected abstract suspend fun handleChargeFull()
}
