package dev.racci.terix.core.extension

import dev.racci.terix.api.origins.AbstractOrigin
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent

suspend fun EntityDamageEvent.DamageCause?.invokeIfPresent(
    event: EntityDamageEvent,
    player: Player,
    action: (AbstractOrigin, Double) -> Unit
) {
    val origin = player.origin()
    origin.damageActions[this]?.invoke(event)
    origin.damageMultipliers[this]?.let { action.invoke(origin, it) }
}
