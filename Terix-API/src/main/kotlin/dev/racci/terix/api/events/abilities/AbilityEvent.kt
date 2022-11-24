package dev.racci.terix.api.events.abilities

import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.abilities.Ability

public sealed class AbilityEvent<A : Ability>(
    public val ability: A,
    async: Boolean = false
) : KPlayerEvent(ability.abilityPlayer, async)
