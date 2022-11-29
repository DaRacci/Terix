package dev.racci.terix.api.services

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

public interface TickService : WithPlugin<Terix> {
    public val playerFlow: Flow<TerixPlayer>

    public val threadContext: CoroutineDispatcher

    public fun filteredPlayer(player: TerixPlayer): Flow<TerixPlayer>

    public companion object : TickService by getKoin().get() {
        public const val TICK_RATE: Int = 1
    }
}
