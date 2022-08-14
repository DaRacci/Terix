package dev.racci.terix.api.origins.states

import dev.racci.minix.api.utils.unsafeCast
import org.bukkit.entity.Player

interface StateSource<I> {

    fun getState(input: I): Boolean

    fun fromPlayer(player: Player): Boolean {
        return getState(player.unsafeCast() ?: throw UnsupportedOperationException("Player is not supported"))
    }
}
