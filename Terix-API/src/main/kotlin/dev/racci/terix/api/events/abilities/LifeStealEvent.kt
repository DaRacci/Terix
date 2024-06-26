package dev.racci.terix.api.events.abilities

import dev.racci.terix.api.origins.abilities.passive.LifeSteal
import org.bukkit.event.entity.EntityDamageByEntityEvent

public class LifeStealEvent internal constructor(
    ability: LifeSteal,
    public val rootCause: EntityDamageByEntityEvent,
    public var rawStolenAmount: Double
) : AbilityEvent<LifeSteal>(ability, false) {
    public val actualStolenAmount: Double
        get() = rawStolenAmount.coerceAtMost(ability.abilityPlayer.maxHealth - ability.abilityPlayer.health)

    override fun isCancelled(): Boolean {
        return rawStolenAmount <= 0.0 || super.isCancelled()
    }
}
