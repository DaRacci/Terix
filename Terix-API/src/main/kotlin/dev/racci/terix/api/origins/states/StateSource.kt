package dev.racci.terix.api.origins.states

import dev.racci.terix.api.data.player.TerixPlayer

public sealed interface StateSource<I : Any> {

    public operator fun get(input: I): Boolean

    public operator fun get(player: TerixPlayer): Boolean {
        return get(player as? I ?: throw UnsupportedOperationException("Player is not supported"))
    }
}
