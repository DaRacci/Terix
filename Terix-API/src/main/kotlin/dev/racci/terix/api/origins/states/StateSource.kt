package dev.racci.terix.api.origins.states

import org.bukkit.entity.Player

public sealed interface StateSource<I : Any> {

    public operator fun get(input: I): Boolean

    public operator fun get(player: Player): Boolean {
        return get(player as? I ?: throw UnsupportedOperationException("Player is not supported"))
    }
}
