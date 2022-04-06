package dev.racci.terix.core

import dev.jorel.commandapi.CommandAPI
import dev.jorel.commandapi.CommandAPIConfig
import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.exists
import dev.racci.terix.api.Terix
import dev.racci.terix.core.data.Config
import dev.racci.terix.core.enchantments.SunResistance
import dev.racci.terix.core.services.CommandService
import dev.racci.terix.core.services.EventForwarderService
import dev.racci.terix.core.services.GUIService
import dev.racci.terix.core.services.HookService
import dev.racci.terix.core.services.ListenerService
import dev.racci.terix.core.services.OriginService
import dev.racci.terix.core.services.RunnableService
import dev.racci.terix.core.services.SoundService
import dev.racci.terix.core.services.SpecialService
import dev.racci.terix.core.services.StorageService
import org.koin.core.component.get
import java.util.logging.Level

@MappedPlugin(
    14443,
    Terix::class,
    [
        CommandService::class,
        EventForwarderService::class,
        GUIService::class,
        HookService::class,
        ListenerService::class,
        OriginService::class,
        RunnableService::class,
        SoundService::class,
        SpecialService::class,
        StorageService::class
    ]
)
class TerixImpl : Terix() {

    override suspend fun handleLoad() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        logger.level = if (get<DataService>().get<Config>().debug) Level.ALL else Level.INFO
        CommandAPI.onLoad(
            CommandAPIConfig()
                .silentLogs(!log.infoEnabled)
                .verboseOutput(log.debugEnabled)
        )
        try {
            if (exists("com.willfp.ecoenchants.EcoEnchantsPlugin")) SunResistance()
        } catch (e: NullPointerException) {
            log.error(e) { "Failed to load EcoEnchants" }
        }
    }

    override suspend fun handleEnable() {
        CommandAPI.onEnable(this)
    }
}
