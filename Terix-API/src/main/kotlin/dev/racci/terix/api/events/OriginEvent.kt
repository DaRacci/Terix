package dev.racci.terix.api.events

import dev.racci.minix.api.events.player.KPlayerEvent
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.origins.origin.Origin

public sealed class OriginEvent(
    player: TerixPlayer,
    public val origin: Origin
) : KPlayerEvent(player, true)
