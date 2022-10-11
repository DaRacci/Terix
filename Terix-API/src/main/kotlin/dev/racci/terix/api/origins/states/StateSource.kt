package dev.racci.terix.api.origins.states

import dev.racci.minix.api.extensions.reflection.safeCast
import org.bukkit.entity.Player

interface StateSource<I : Any> {

    fun getState(input: I): Boolean

    fun fromPlayer(player: Player): Boolean {
        return getState(player.safeCast() ?: throw UnsupportedOperationException("Player is not supported"))
    }
}
