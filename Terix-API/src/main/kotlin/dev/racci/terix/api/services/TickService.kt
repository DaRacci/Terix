package dev.racci.terix.api.services

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.Terix
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import org.bukkit.entity.Player

public interface TickService : WithPlugin<Terix> {
    public val playerFlow: SharedFlow<Player>

    public val threadContext: CoroutineDispatcher

    public companion object : TickService by getKoin().get() {
        public const val TICK_RATE: Int = 2
    }
}
