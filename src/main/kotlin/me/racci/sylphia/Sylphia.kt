@file:JvmName("Sylphia")
package me.racci.sylphia

import co.aikar.commands.BukkitCommandExecutionContext
import co.aikar.commands.PaperCommandManager
import co.aikar.taskchain.BukkitTaskChainFactory
import co.aikar.taskchain.TaskChainFactory
import me.racci.raccilib.Level
import me.racci.raccilib.log
import me.racci.sylphia.commands.OriginCommand
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.data.configuration.OptionL
import me.racci.sylphia.data.storage.StorageProvider
import me.racci.sylphia.data.storage.YamlStorageProvider
import me.racci.sylphia.hook.PlaceholderAPIHook
import me.racci.sylphia.hook.perms.LuckPermsHook
import me.racci.sylphia.hook.perms.PermManager
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.listeners.PlayerConsumeEvent
import me.racci.sylphia.listeners.PlayerJoinLeaveEvent
import me.racci.sylphia.listeners.PlayerMoveEvent
import me.racci.sylphia.origins.OriginHandler
import me.racci.sylphia.origins.objects.Origin
import me.racci.sylphia.utils.eventlistners.PlayerMoveFullListener
import me.racci.sylphia.utils.eventlistners.PlayerMoveListener
import me.racci.sylphia.utils.minecraft.WorldManager
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader


class Sylphia: JavaPlugin() {

    // Configs
    private var lang: Lang? = null
    private var optionLoader: OptionL? = null
    // Plugin Integrations
    var luckPerms: Boolean? = null
    var placeholderAPI: Boolean? = null
    // Misc
    var storageProvider: StorageProvider? = null
    var taskChainFactory: TaskChainFactory? = null
    // Managers
    var permManager: PermManager? = null
    var worldManager: WorldManager? = null
    var playerManager: PlayerManager? = null
    var originHandler: OriginHandler? = null
    var commandManager: PaperCommandManager? = null

    override fun onEnable() {
        log(Level.OUTLINE, "&m---------------------------------------------")
        log(Level.INFO, "Sylphia has started loading!")

        loadConfig()
        loadLang()
        loadHooks()

        loadPermManager()
        loadWorldManager()
        loadStorageManager()
        loadOriginsManager()

        registerCommands()
        registerEvents()

        taskChainFactory = BukkitTaskChainFactory.create(this)

        log(Level.SUCCESS, "Sylphia has finished loading successfully!")
        log(Level.OUTLINE, "&m---------------------------------------------")
    }

    override fun onDisable() {
        for(playerData: PlayerData in playerManager!!.playerDataMap.values) {
            storageProvider!!.save(playerData.player, true)
        }
        playerManager!!.playerDataMap.clear()
        if(File(dataFolder, "config.yml").exists()) {
            reloadConfig()
            saveConfig()
        }
    }

    // TODO This shit???
    private fun loadConfig() {
        config.options().copyDefaults(true)
        saveDefaultConfig()

        val inputStream: InputStream = getResource("config.yml")!!
        val defaultConfig: YamlConfiguration = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
        val config: ConfigurationSection = defaultConfig.getConfigurationSection("")!!
        for(key: String in config.getKeys(true)) {
            if(!config.contains(key)) {
                config.set(key, defaultConfig.get(key))
            }
        }
        saveConfig()
        optionLoader = OptionL(this)
        optionLoader!!.loadOptions()
    }

    // TODO This shit???
    private fun loadLang() {
        lang = Lang(this)
        server.pluginManager.registerEvents(lang!!, this)
        lang!!.load()
        lang!!.loadEmbeddedMessages(commandManager)
        lang!!.loadLanguages(commandManager)
    }

    private fun loadHooks() {
        placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
        if (placeholderAPI as Boolean) {
            PlaceholderAPIHook(this).register()
            log(Level.INFO, "Hooked PlaceholderAPI!")
        }
    }

    private fun loadPermManager() {
        try {
            if (Bukkit.getServicesManager().getRegistration(LuckPerms::class.java) != null) {
                    permManager = LuckPermsHook()
                    luckPerms = true
                    log(Level.INFO, "Hooked LuckPerms!")
                }
        } catch (e: NoClassDefFoundError) {
            log(Level.INFO, "No permission manager found, please make sure one is installed!")
        }
    }

    private fun loadWorldManager() {
        worldManager = WorldManager(this)
        worldManager!!.loadWorlds()
        log(Level.INFO, "Loaded World Manager")
    }

    private fun loadStorageManager() {
        playerManager = PlayerManager()
        storageProvider = YamlStorageProvider(this)
        log(Level.INFO, "Loaded Storage Provider")
    }

    private fun loadOriginsManager() {
        originHandler = OriginHandler(this)
    }

    private fun registerCommands() {
        commandManager = PaperCommandManager(this)
        commandManager!!.enableUnstableAPI("help") // TODO Look into why this is deprecated
        commandManager!!.usePerIssuerLocale(true, false) // TODO Look into if this is actually needed or not
        // Context? TODO i don't know what the fuck context is used for!!!
        commandManager!!.commandContexts.registerContext(Origin::class.java) // TODO Make this shit smaller again???
            { c: BukkitCommandExecutionContext -> Origin.valueOf(c.popFirstArg())}
        // Completions
        commandManager!!.commandCompletions.registerAsyncCompletion("origins") {
            val values: MutableList<String> = ArrayList()
            for (origin in Origin.values()) {
                values.add(origin.toString().lowercase())
            }
            values
        }
        // Command Registering
        commandManager!!.registerCommand(OriginCommand(this))
//		commandManager!!.registerCommand(OriginSelector());
    }

    private fun registerEvents() {
        val pm: PluginManager = server.pluginManager
        pm.registerEvents(PlayerMoveListener(), this)
        pm.registerEvents(PlayerMoveEvent(this), this)
        pm.registerEvents(PlayerConsumeEvent(this), this)
        pm.registerEvents(PlayerJoinLeaveEvent(this), this)
        pm.registerEvents(PlayerMoveFullListener(this), this)
    }



}