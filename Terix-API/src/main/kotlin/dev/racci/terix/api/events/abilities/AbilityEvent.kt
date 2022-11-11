package dev.racci.terix.api.events.abilities

import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.abilities.PassiveAbility

public sealed class AbilityEvent(
    public val ability: PassiveAbility,
    async: Boolean = false,
) : KPlayerEvent(ability.abilityPlayer, async)
