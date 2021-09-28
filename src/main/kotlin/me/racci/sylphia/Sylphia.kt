package me.racci.sylphia

import co.aikar.commands.BukkitCommandExecutionContext
import co.aikar.commands.ConditionFailedException
import co.aikar.commands.PaperCommandManager
import me.clip.placeholderapi.libs.kyori.adventure.platform.bukkit.BukkitAudiences
import me.racci.raccicore.Level
import me.racci.raccicore.log
import me.racci.sylphia.commands.OriginCommand
import me.racci.sylphia.commands.SpecialCommands
import me.racci.sylphia.commands.SylphiaCommand
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.data.configuration.OptionL
import me.racci.sylphia.data.storage.StorageProvider
import me.racci.sylphia.data.storage.YamlStorageProvider
import me.racci.sylphia.hook.PlaceholderAPIHook
import me.racci.sylphia.hook.perms.LuckPermsHook
import me.racci.sylphia.hook.perms.PermManager
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.listeners.*
import me.racci.sylphia.managers.ItemManager
import me.racci.sylphia.managers.SoundManager
import me.racci.sylphia.managers.WorldManager
import me.racci.sylphia.origins.Origin
import me.racci.sylphia.origins.OriginHandler
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*


class Sylphia: JavaPlugin() {

    // Configs
    lateinit var lang: Lang ; private set
    private lateinit var optionLoader: OptionL ; private set
    // Plugin Integrations
    private var luckPerms: Boolean = false ; private set
    private var placeholderAPI: Boolean = false ; private set
    // Misc
    lateinit var storageProvider: StorageProvider ; private set
    // Managers
    lateinit var audienceManager: BukkitAudiences ; private set
    private lateinit var soundManager: SoundManager ; private set
    private lateinit var permManager: PermManager ; private set
    lateinit var worldManager: WorldManager ; private set
    lateinit var playerManager: PlayerManager ; private set
    lateinit var itemManager: ItemManager ; private set
    lateinit var originHandler: OriginHandler ; private set
    lateinit var originManager: OriginManager ; private set
    private lateinit var commandManager: PaperCommandManager ; private set

    companion object {
        lateinit var instance: Sylphia
    }

    override fun onEnable() {
        log(Level.OUTLINE, "&m---------------------------------------------")
        log(Level.INFO, "Sylphia has started loading!")

        loadConfig()
        loadLang()
        loadHooks()

        instance = this

        loadAudienceManager()
        loadPermManager()
        loadWorldManager()
        loadStorageManager()
        loadOriginsManager()
        loadItemManager()

        registerCommands()
        registerEvents()
        registerRunnables()
        registerSoundManager()


        log(Level.SUCCESS, "Sylphia has finished loading successfully!")
        log(Level.OUTLINE, "&m---------------------------------------------")
    }

    override fun onDisable() {

        playerManager.playerDataMap.values.map(PlayerData::player).map { it }.forEach(storageProvider::save)
        playerManager.playerDataMap.clear()
        audienceManager.close()
        if (File(dataFolder, "config.yml").exists()) {
            reloadConfig()
            saveConfig()
        }

    }

    fun handleReload() {
        log(Level.OUTLINE, "&m---------------------------------------------")
        log(Level.INFO, "Sylphia has started reloading!")

        optionLoader.loadOptions()
        lang.loadLang(commandManager)
        audienceManager.close()
        audienceManager = BukkitAudiences.create(this)
        worldManager.loadWorlds(this)
        originHandler.loadOrigins()
        Bukkit.getOnlinePlayers().forEach { player1x ->
            if(originManager.getOrigin(player1x.uniqueId) == null) return@forEach
            originManager.refreshAll(player1x)
        }
        itemManager.loadItems()
        commandManager.commandContexts.registerContext(Origin::class.java)
        { c: BukkitCommandExecutionContext -> OriginManager.valueOf(c.popFirstArg()) }
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            val values: MutableList<String> = ArrayList()
            for (origin in OriginManager.values()) {
                values.add(origin.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            }
            values
        }

        log(Level.SUCCESS, "Sylphia has finished reloading successfully!")
        log(Level.OUTLINE, "&m---------------------------------------------")
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
        optionLoader.loadOptions()
    }

    // TODO This shit???
    private fun loadLang() {
        lang = Lang(this)
        commandManager = PaperCommandManager(this)
        server.pluginManager.registerEvents(lang, this)
        lang.loadLang(commandManager)
    }

    private fun loadHooks() {
        placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")
        if (placeholderAPI) {
            PlaceholderAPIHook(this).register()
            log(Level.INFO, "Hooked PlaceholderAPI!")
        }
    }

    private fun loadAudienceManager() {
        audienceManager = BukkitAudiences.create(this)
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
    }

    private fun loadStorageManager() {
        playerManager = PlayerManager()
        storageProvider = YamlStorageProvider(this)
        log(Level.INFO, "Loaded Storage Provider")
    }

    private fun loadOriginsManager() {
        originManager = OriginManager(this)
        originHandler = OriginHandler(this)
    }

    private fun loadItemManager() {
        itemManager = ItemManager()
    }

    private fun registerCommands() {
        commandManager.enableUnstableAPI("help") // TODO Look into why this is deprecated
        commandManager.locales.defaultLocale = Locale.ENGLISH
        commandManager.usePerIssuerLocale(true, false) // TODO Look into if this is actually needed or not
        // Context? TODO I don't know what the fuck context is used for!!!
        commandManager.commandContexts.registerContext(Origin::class.java) // TODO Make this shit smaller again???
            { c: BukkitCommandExecutionContext -> OriginManager.valueOf(c.popFirstArg()) }
        // Completions
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            val values: MutableList<String> = ArrayList()
            for (origin in OriginManager.values()) {
                values.add(origin.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            }
            values
        }
        commandManager.commandConditions.addCondition(Int::class.java, "limits") { c, exec, value ->
            if (value == null) {
                return@addCondition
            }
            if (c.hasConfig("min") && c.getConfigValue("min", 0) > value) {
                throw ConditionFailedException("Min value must be " + c.getConfigValue("min", 0))
            }
            if (c.hasConfig("max") && c.getConfigValue("max", 3) < value) {
                throw ConditionFailedException("Max value must be " + c.getConfigValue("max", 3))
            }
        }
        // Command Registering
        commandManager.registerCommand(OriginCommand(this))
        commandManager.registerCommand(SylphiaCommand(this))
        SpecialCommands(this, commandManager)
    }

    private fun registerEvents() {
        val pm: PluginManager = server.pluginManager
        pm.registerEvents(PlayerMoveListener(originManager), this)
        pm.registerEvents(PlayerConsumeListener(this), this)
        pm.registerEvents(PlayerJoinLeaveListener(this), this)
        pm.registerEvents(PlayerRespawnListener(this), this)
        pm.registerEvents(PlayerDamageListener(this), this)
        pm.registerEvents(PlayerChangeWorldListener(this), this)
        pm.registerEvents(OriginEventListener(this), this)
        pm.registerEvents(RunnableListener(), this)
    }

    private fun registerRunnables() {
        val pm: PluginManager = server.pluginManager
        SunLightRunnable(pm).runTaskTimerAsynchronously(this, 0L, 20)
        RainRunnable(pm).runTaskTimerAsynchronously(this, 0L, 20)
        WaterRunnable(pm).runTaskTimerAsynchronously(this, 0L, 20)
    }

    private fun registerSoundManager() {
        soundManager = SoundManager(this)
    }
}