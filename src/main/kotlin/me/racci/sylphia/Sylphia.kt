package me.racci.sylphia

import co.aikar.commands.BaseCommand
import co.aikar.commands.BukkitCommandExecutionContext
import co.aikar.commands.PaperCommandManager
import me.racci.raccicore.Level
import me.racci.raccicore.RacciPlugin
import me.racci.raccicore.log
import me.racci.raccicore.utils.pm
import me.racci.sylphia.commands.OriginCommand
import me.racci.sylphia.commands.SpecialCommands
import me.racci.sylphia.commands.SylphiaCommand
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.data.PlayerManager
import me.racci.sylphia.data.configuration.OptionL
import me.racci.sylphia.data.storage.StorageProvider
import me.racci.sylphia.data.storage.YamlStorageProvider
import me.racci.sylphia.factories.*
import me.racci.sylphia.hook.PlaceholderAPIHook
import me.racci.sylphia.hook.perms.LuckPermsHook
import me.racci.sylphia.hook.perms.PermManager
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.listeners.*
import me.racci.sylphia.managers.ItemManager
import me.racci.sylphia.managers.SoundManager
import me.racci.sylphia.managers.WorldManager
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.origins.abilityevents.OffhandListener
import me.racci.sylphia.runnables.RainRunnable
import me.racci.sylphia.runnables.SunLightRunnable
import me.racci.sylphia.runnables.WaterRunnable
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import java.io.InputStreamReader
import java.util.*


internal lateinit var plugin            : Sylphia               ; private set
internal lateinit var lang              : Lang                  ; private set

internal lateinit var itemFactory       : ItemFactory           ; private set
internal lateinit var soundFactory      : SoundFactory          ; private set
internal lateinit var potionFactory     : PotionFactory         ; private set
internal lateinit var originFactory     : OriginFactory         ; private set

internal lateinit var permManager       : PermManager           ; private set
internal lateinit var itemManager       : ItemManager           ; private set
internal lateinit var soundManager      : SoundManager          ; private set
internal lateinit var worldManager      : WorldManager          ; private set
internal lateinit var originManager     : OriginManager         ; private set
internal lateinit var playerManager     : PlayerManager         ; private set
internal lateinit var storageManager    : StorageProvider       ; private set
internal lateinit var audienceManager   : BukkitAudiences       ; private set




class Sylphia: RacciPlugin() {

    // Configs
    private lateinit var optionLoader: OptionL

    // Plugin Integrations
    private var luckPerms: Boolean = false
    private var placeholderAPI: Boolean = false

    override fun handleEnable() {

        load()

    }

    private fun load() {
        plugin = this
        lang = Lang(this)

        // Config
        config.options().copyDefaults(true)
        saveDefaultConfig()
        val inputStream = getResource("config.yml")!!
        val defaultConfig = YamlConfiguration.loadConfiguration(InputStreamReader(inputStream))
        for(key: String in config.getKeys(true)) {
            if(!config.contains(key)) {
                config.set(key, defaultConfig.get(key))
            }
        }
        saveConfig()
        optionLoader = OptionL(this)
        optionLoader.loadOptions()

        // Lang
        pm.registerEvents(lang, this)
        lang.loadLang(commandManager)

        // Instancing
        itemFactory     = ItemFactory()
        soundFactory    = SoundFactory()
        potionFactory   = PotionFactory()
        originFactory   = OriginFactory()

        itemManager     = ItemManager()
        soundManager    = SoundManager()
        worldManager    = WorldManager()
        originManager   = OriginManager()
        playerManager   = PlayerManager()
        storageManager  = YamlStorageProvider()
        audienceManager = BukkitAudiences.create(this)

        // Hooks
        placeholderAPI = pm.isPluginEnabled("PlaceholderAPI")
        if (placeholderAPI) {
            PlaceholderAPIHook(this).register()
            log(Level.INFO, "Hooked PlaceholderAPI!")
        }
        try {
            if (Bukkit.getServicesManager().getRegistration(LuckPerms::class.java) != null) {
                permManager = LuckPermsHook()
                luckPerms = true
                log(Level.INFO, "Hooked LuckPerms!")
            }
        } catch (e: NoClassDefFoundError) {
            log(Level.INFO, "No permission manager found, please make sure one is installed!")
        }

        // Commands
        commandManager.commandContexts.registerContext(Origin::class.java) {
            OriginManager.valueOf(it.popFirstArg())}
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            OriginManager.values().map{Origin::identity.name}}
        listOf(SylphiaCommand(), OriginCommand(), SpecialCommands()).forEach {commandManager.registerCommand(it)}

        // Events
        registerListeners(
            PlayerConsumeListener(),
            PlayerMoveListener(),
            PlayerJoinLeaveListener(),
            PlayerRespawnListener(),
            PlayerDamageListener(),
            PlayerChangeWorldListener(),
            OriginEventListener(),
            RunnableListener(),
            OffhandListener(),
        )

        // Runnables
        registerRunnables(
            SunLightRunnable(),
            RainRunnable(),
            WaterRunnable(),
            async = true,
            repeating = true,
            delay = 0L,
            period = 20L,
        )
    }

    fun doReload() {
        unload()
        placeholderAPI = false
        luckPerms = false
        registerRunnables()

    }

    private fun unload() {
        audienceManager.close()
        playerManager.playerDataMap.values.map(PlayerData::player).forEach(storageManager::save)
        playerManager.playerDataMap.clear()
        Lang.Messages.messagesMap.clear()
        reloadConfig() ; saveConfig()
    }

    override fun onDisable() {

        unload()

    }

    override fun handleReload() {
        log(Level.OUTLINE, "&m---------------------------------------------")
        log(Level.INFO, "Sylphia has started reloading!")

        optionLoader.loadOptions()
        lang.loadLang(commandManager)
        audienceManager.close()
        audienceManager = BukkitAudiences.create(this)
        worldManager.loadWorlds(this)
        originManager.refresh()
        Bukkit.getOnlinePlayers().forEach { player1x ->
            if(originManager.getOrigin(player1x.uniqueId) == null) return@forEach
            originManager.removeAll(player1x)
        }
        itemManager.loadItems()
        commandManager.commandContexts.registerContext(Origin::class.java)
        { c: BukkitCommandExecutionContext -> OriginManager.valueOf(c.popFirstArg()) }
        commandManager.commandCompletions.registerAsyncCompletion("origins") {
            val values: MutableList<String> = ArrayList()
            for (origin in OriginManager.values()) {
                values.add(origin.identity.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
            }
            values
        }

        log(Level.SUCCESS, "Sylphia has finished reloading successfully!")
        log(Level.OUTLINE, "&m---------------------------------------------")
    }
}