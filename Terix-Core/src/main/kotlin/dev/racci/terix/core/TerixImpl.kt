package dev.racci.terix.core

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.updater.Version
import dev.racci.terix.api.Terix
import dev.racci.terix.core.data.Config
import org.koin.core.component.get
import java.util.logging.Level

@MappedPlugin(14443, Terix::class)
class TerixImpl : Terix() {

    override suspend fun handleLoad() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        logger.level = if (get<DataService>().get<Config>().debug || Version(description.version).isPreRelease) Level.ALL else Level.INFO
        CommandAPI.onLoad(
            CommandAPIConfig()
                .silentLogs(!log.infoEnabled)
                .verboseOutput(log.debugEnabled)
        )
        // TODO: Fix this
//        try {
//            if (exists("com.willfp.ecoenchants.EcoEnchantsPlugin")) SunResistance()
//        } catch (e: NullPointerException) {
//            log.error(e) { "Failed to load EcoEnchants" }
//        }
    }

    override suspend fun handleEnable() {
        CommandAPI.onEnable(this)
    }
}
