package me.racci.sylphia.lang

import co.aikar.commands.MessageKeys
import co.aikar.commands.MinecraftMessageKeys
import co.aikar.commands.PaperCommandManager
import me.racci.raccicore.Level
import me.racci.raccicore.log
import me.racci.raccicore.utils.LangDefaultFileException
import me.racci.raccicore.utils.LangLoadException
import me.racci.raccicore.utils.LangNoVersionException
import me.racci.raccicore.utils.LangUpdateFileException
import me.racci.raccicore.utils.strings.colour
import me.racci.raccicore.utils.strings.replace
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.configuration.Option
import me.racci.sylphia.data.configuration.OptionL
import me.racci.sylphia.lang.Lang.Messages.messagesMap
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class Lang(private val plugin: Sylphia): Listener {

    object Messages {
        internal val messagesMap: LinkedHashMap<MessageKey, String> = LinkedHashMap()
        fun get(key: MessageKey): String {
            return messagesMap[key] ?: ""
        }
    }

    private val lang: String = "lang.yml"
    private val fileVersion: String = "File_Version"

    /**
     * Well... I mean it's in the name
     */
    fun loadLang(commandManager: PaperCommandManager) {
        val startTime = System.currentTimeMillis()
        checkExistingFile()
        val defaultFile: YamlConfiguration = loadDefaultFile()
        loadLangFile(commandManager, defaultFile)
        val endTime = System.currentTimeMillis()
        log(Level.INFO, "Loaded lang in " + (endTime - startTime) + "ms")
    }
    /**
     * Checks if the Lang file exists and if not adds a new one
     * @throws [LangLoadException] If an error occurs
     */
    private fun checkExistingFile() {
        try {
            if (!File(plugin.dataFolder, lang).exists()) {
                plugin.saveResource(lang, false)
            }
        } catch (e: Exception) {
            throw LangLoadException("There was an error adding the default Lang file.")
        }
    }
    /**
     *  Loads the default lang configuration
     *  @throws [LangDefaultFileException] if an error occurs
     */
    private fun loadDefaultFile(): YamlConfiguration {
        try {
            return YamlConfiguration.loadConfiguration(
                InputStreamReader(
                plugin.getResource(lang)!!,
                StandardCharsets.UTF_8)
            )
        } catch (e: Exception) {
            throw LangDefaultFileException("There was an error loading the default lang configuration." +
                    "Please report this to Racci" + e.printStackTrace())
        }
    }
    /**
     * @param [commandManager] AikarCommandManager for changing the default messages.
     * @param [defaultFile] Default lang file configuration.
     * @throws [LangNoVersionException] if "File_Version" is not found.
     */
    private fun loadLangFile(commandManager: PaperCommandManager, defaultFile: YamlConfiguration) {
        val file = File(plugin.dataFolder, lang)
        val config: YamlConfiguration = updateLangFile(file, defaultFile, YamlConfiguration.loadConfiguration(file))
        if (config.contains(fileVersion)) {
            val prefixes: HashMap<MessageKey, String> = HashMap()
            Prefix.values().forEach { key: Prefix -> prefixes[key] = colour(config.getString(key.path), true)!! }
            val messages: HashMap<MessageKey, String> = messagesMap
            for (path: String in config.getKeys(true)) {
                if (!config.isConfigurationSection(path)) {
                    var key: MessageKey = Empty.EMPTY
                    MessageKey.values().forEach {
                        messageKey: MessageKey -> if(messageKey.path == path) { key = messageKey }
                    }
                    if(key == Empty.EMPTY) { key = CustomMessageKey(path) }
                    val message: String = config.getString(path) ?: ""
                    messages[key] = colour(replace(message,
                        "{Origins}", prefixes[Prefix.ORIGINS] ?: "",
                        "{Sylphia}", prefixes[Prefix.SYLPHIA] ?: "",
                        "{Error}", prefixes[Prefix.ERROR] ?: "",
                        "{token}", OptionL.getString(Option.ORIGIN_TOKEN_NAME)), true) ?: ""
                }
            }
            MessageKey.values().forEach { key: MessageKey -> if(config.getString(key.path) == null) log(Level.WARNING,
                "Message with path " + key.path + " wasn't found!") }
            ACFCoreMessage.values().forEach { message: ACFCoreMessage ->
                commandManager.locales.addMessage(Locale.ENGLISH,
                    MessageKeys.valueOf(message.name),
                    colour(replace(
                        config.getString(message.path) ?: MessageKeys.valueOf(message.name).messageKey.key,
                        "{Origins}", prefixes[Prefix.ORIGINS] ?: "",
                        "{Sylphia}", prefixes[Prefix.SYLPHIA] ?: "",
                        "{Error}", prefixes[Prefix.ERROR] ?: ""), true)
                )
            }
            ACFMinecraftMessage.values().forEach { message: ACFMinecraftMessage ->
                commandManager.locales.addMessage(Locale.ENGLISH, MinecraftMessageKeys.valueOf(message.name), colour(replace(
                    config.getString(message.path) ?: MessageKeys.valueOf(message.name).messageKey.key,
                    "{Origins}", prefixes[Prefix.ORIGINS] ?: "",
                    "{Sylphia}", prefixes[Prefix.SYLPHIA] ?: "",
                    "{Error}", prefixes[Prefix.ERROR] ?: ""), true)
                )
            }
            messagesMap.putAll(messages)
        } else {
            throw LangNoVersionException("Couldn't find the [File_Version] in lang file, please make sure it is present")
        }
    }
    /**
     * @param [file] The lang file path.
     * @param [defaultFile] The default Lang config.
     * @param [config] The current lang config.
     * @return [YamlConfiguration] The updated lang file.
     * @throws [LangUpdateFileException] if an error occurs.
     */
    private fun updateLangFile(file: File, defaultFile: YamlConfiguration, config: YamlConfiguration): YamlConfiguration {
        if(config.contains(fileVersion)) {
            val newestVersion: Int = defaultFile.getInt(fileVersion)
            if(config.getInt(fileVersion) != newestVersion) {
                try {
                    val configSection: ConfigurationSection = defaultFile.getConfigurationSection("")!!
                    var keysAdded = 0
                    for(key: String in configSection.getKeys(true)) {
                        if(!configSection.isConfigurationSection(key) && !config.contains(key)) {
                            config.set(key, defaultFile.get(key))
                            keysAdded++
                        }
                    }
                    config.set(fileVersion, newestVersion)
                    config.save(file)
                    log(Level.INFO, "$lang was updated to a new file version and had $keysAdded new keys added.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw LangUpdateFileException("There was an error updating your lang file to the latest configuration" + e.printStackTrace())
                }
            }
        }
        return YamlConfiguration.loadConfiguration(file)
    }
}