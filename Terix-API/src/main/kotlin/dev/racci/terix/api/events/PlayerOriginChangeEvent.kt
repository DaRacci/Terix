package dev.racci.terix.api.events

import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.terix.api.origins.AbstractOrigin
import org.bukkit.entity.Player

class PlayerOriginChangeEvent(
    player: Player,
    val preOrigin: AbstractOrigin,
    val newOrigin: AbstractOrigin
) : KPlayerEvent(player, true)
