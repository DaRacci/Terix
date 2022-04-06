package me.racci.terix

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandExecutionContext
import com.github.shynixn.mccoroutine.asyncDispatcher
import com.github.shynixn.mccoroutine.minecraftDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import me.racci.raccicore.RacciPlugin
import me.racci.raccicore.runnables.KotlinRunnable
import me.racci.raccicore.utils.extensions.KotlinListener
import me.racci.raccicore.utils.extensions.pm
import me.racci.raccicore.utils.pm
import me.racci.terix.commands.OriginCommand
import me.racci.terix.commands.SpecialCommands
import me.racci.terix.commands.TerixCommand
import me.racci.terix.data.PlayerManager
import me.racci.terix.data.configuration.OptionL
import me.racci.terix.data.storage.StorageProvider
import me.racci.terix.data.storage.YamlStorageProvider
import me.racci.terix.factories.Origin
import me.racci.terix.hook.PlaceholderAPIHook
import me.racci.terix.hook.perms.LuckPermsHook
import me.racci.terix.hook.perms.PermManager
import me.racci.terix.lang.Lang
import me.racci.terix.listeners.OriginEventListener
import me.racci.terix.listeners.PlayerChangeWorldListener
import me.racci.terix.listeners.PlayerConsumeListener
import me.racci.terix.listeners.PlayerDamageListener
import me.racci.terix.listeners.PlayerJoinLeaveListener
import me.racci.terix.listeners.PlayerMoveListener
import me.racci.terix.listeners.PlayerRespawnListener
import me.racci.terix.listeners.RunnableListener
import me.racci.terix.managers.ItemManager
import me.racci.terix.managers.WorldManager
import me.racci.terix.origins.OriginManager
import me.racci.terix.origins.abilityevents.OffhandListener
import me.racci.terix.runnables.RainRunnable
import me.racci.terix.runnables.SunLightRunnable
import me.racci.terix.runnables.WaterRunnable
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.metadata.FixedMetadataValue
import java.io.InputStreamReader
import java.util.Locale

class Terix : RacciPlugin(
    "&6",
    "Terix",
) {

    internal companion object {
        lateinit var instance: Terix ; private set
        lateinit var permManager: PermManager ; private set
        lateinit var storageManager: StorageProvider ; private set
        lateinit var audienceManager: BukkitAudiences ; private set

        val sync = instance.minecraftDispatcher
        val async = instance.asyncDispatcher

        var luckPerms = false
        var protocolLib = false
        var placeholderAPI = false

        val log = instance.log

        fun setMetadata(block: Block, key: String) =
            block.setMetadata(key, FixedMetadataValue(instance, true))
        fun removeMetadata(block: Block, key: String) =
            block.removeMetadata(key, instance)
        suspend fun run(
            unit: Runnable.() -> Unit,
            delay: Long = 0,
            async: Boolean = false
        ) {
            delay(delay / 50)
            withContext(if (async) instance.asyncDispatcher else instance.minecraftDispatcher) { unit }
        }
    }

    // Configs
    private lateinit var optionLoader: OptionL

    override suspend fun handleEnable() {

        instance = this

        config.options().copyDefaults(true)
        saveDefaultConfig()
        val inputStream = getResource("config.yml")!!
        val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
        for (key: String in config.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key))
            }
        }
        saveConfig()
        optionLoader = OptionL(this)
        optionLoader.loadOptions()

        Lang.init()

        placeholderAPI = pm.isPluginEnabled("PlaceholderAPI")
        if (placeholderAPI) {
            PlaceholderAPIHook().register()
            log.info("Hooked PlaceholderAPI!")
        }
        protocolLib = pm.isPluginEnabled("ProtocolLib")
        if (protocolLib) {
            log.info("Hooked ProtocolLib")
        }
        try {
            if (Bukkit.getServicesManager().getRegistration(LuckPerms::class.java) != null) {
                permManager = LuckPermsHook()
                luckPerms = true
                log.info("Hooked LuckPerms!")
            }
        } catch (e: NoClassDefFoundError) {
            log.info("No permission manager found, please make sure one is installed!")
        }

        OriginManager.init()
        storageManager = YamlStorageProvider(this)
        audienceManager = BukkitAudiences.create(this)

        commandManager.commandContexts.registerContext(Origin::class.java) {
            OriginManager.valueOf(it.popFirstArg())
        }
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            OriginManager.values().map {}
        }
    }

    override suspend fun handleUnload() {
        PlayerManager.close()
        Lang.close()
        reloadConfig() ; saveConfig()
        OriginManager.close()
        PlayerManager.close()
        audienceManager.close()
    }

    override suspend fun registerListeners(): List<KotlinListener> {
        return listOf(
            PlayerConsumeListener(),
            PlayerMoveListener(),
            PlayerJoinLeaveListener(),
            PlayerRespawnListener(),
            PlayerDamageListener(),
            PlayerChangeWorldListener(),
            OriginEventListener(),
            RunnableListener(),
            OffhandListener(this),
        )
    }

    override suspend fun registerCommands(): List<BaseCommand> {
        return listOf(
            TerixCommand(this),
            OriginCommand(this),
            SpecialCommands()
        )
    }

    override suspend fun registerRunnables(): List<KotlinRunnable> {
        return listOf(
            SunLightRunnable(this),
            RainRunnable(this),
            WaterRunnable(this),
        )
    }

    override suspend fun handleReload() {
        log.info("")
        log.info("Terix has started reloading!")

        optionLoader.loadOptions()
        Lang.init()
        audienceManager.close()
        audienceManager = BukkitAudiences.create(this)
        WorldManager.init(this)
        OriginManager.init()
        Bukkit.getOnlinePlayers().forEach { player1x ->
            if (OriginManager.getOrigin(player1x.uniqueId) == null) return@forEach
            OriginManager.removeAll(player1x)
        }
        ItemManager.loadItems()
        commandManager.commandContexts.registerContext(Origin::class.java) { c: BukkitCommandExecutionContext -> OriginManager.valueOf(c.popFirstArg()) }
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            val values: MutableList<String> = ArrayList()
            for (origin in OriginManager.values()) {
                values.add(origin.identity.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            }
            values
        }

        log.info()
        log.success("Terix has finished reloading successfully!")
        log.info()
    }
}
