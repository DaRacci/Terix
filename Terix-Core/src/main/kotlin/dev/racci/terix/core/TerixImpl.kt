package dev.racci.terix.core

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import dev.racci.terix.api.Terix
import dev.racci.terix.core.services.CommandService
import dev.racci.terix.core.services.EventForwarderService
import dev.racci.terix.core.services.HookService
import dev.racci.terix.core.services.LangService
import dev.racci.terix.core.services.ListenerService
import dev.racci.terix.core.services.OriginService
import dev.racci.terix.core.services.RunnableService
import dev.racci.terix.core.services.SoundService
import dev.racci.terix.core.services.StorageService
import org.spigotmc.SpigotConfig
import java.util.logging.Level

class TerixImpl : Terix() {

    override val bindToKClass = Terix::class
    override val bStatsId = 14443

    override suspend fun handleLoad() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        logger.level = if (SpigotConfig.debug) Level.ALL else Level.INFO
        CommandAPI.onLoad(
            CommandAPIConfig()
                .silentLogs(!log.infoEnabled)
                .verboseOutput(log.debugEnabled)
        )
    }

    override suspend fun handleEnable() {
        CommandAPI.onEnable(this)

        extensions {
            add(::OriginService)
            add(::StorageService)
            add(::SoundService)
            add(::RunnableService)
            add(::EventForwarderService)
            add(::ListenerService)
            add(::HookService)
            add(::LangService)
            add(::CommandService)
        }
    }
}
