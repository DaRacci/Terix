package dev.racci.terix.api.events

import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player

sealed class OriginEvent(
    player: Player,
    val origin: Origin
) : KPlayerEvent(player, true)
