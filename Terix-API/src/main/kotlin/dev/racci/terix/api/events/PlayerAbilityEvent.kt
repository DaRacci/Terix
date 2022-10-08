package dev.racci.terix.api.events

import dev.racci.minix.api.events.CompanionEventHandler
import dev.racci.minix.api.events.KPlayerEvent
import dev.racci.terix.api.origins.abilities.Ability
import org.bukkit.entity.Player

sealed class PlayerAbilityEvent(
    player: Player,
    val ability: Ability
) : KPlayerEvent(player, true) {
    companion object : CompanionEventHandler() {
        @JvmStatic override fun getHandlerList() = super.getHandlerList()
    }
}
