package dev.racci.terix.api.events

import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.entity.Player

public sealed class OriginEvent(
    player: Player,
    public val origin: Origin
) : KPlayerEvent(player, true)
