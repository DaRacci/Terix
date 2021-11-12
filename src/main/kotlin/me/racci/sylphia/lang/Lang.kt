package me.racci.sylphia.lang

import co.aikar.commands.MessageKeys
import me.racci.raccicore.utils.LangDefaultFileException
import me.racci.raccicore.utils.LangLoadException
import me.racci.raccicore.utils.LangNoVersionException
import me.racci.raccicore.utils.LangUpdateFileException
import me.racci.raccicore.utils.catch
import me.racci.sylphia.Sylphia
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale

object Lang {

    private val messageMap  = HashMap<MessageKey, String>()
    private val prefixes    = HashMap<MessageKey, Component>()
    operator fun get(key: MessageKey) = messageMap[key]!!
    operator fun get(key: MessageKey, unit: String.() -> Unit = {}) {
        MiniMessage.builder().placeholderResolver {
            unit(it)
            prefixes[MessageKey.valueOf(it.lowercase())]
        }.build().parse(messageMap[key]!!)
    }

    fun init() {
        val startTime   = System.currentTimeMillis()
        val plugin      = Sylphia.instance

        catch<Exception>({throw LangLoadException("There was an error adding the default Lang file. $it")}) {
            if (!File("${plugin.dataFolder}/lang.yaml").exists()) {
                plugin.saveResource("lang.yaml", false)
            }
        }

        var defaultConfig = YamlConfiguration()
        catch<Exception>({throw LangDefaultFileException("There was an error loading the default lang configuration. $it")}) {
            defaultConfig = YamlConfiguration.loadConfiguration(
                InputStreamReader(
                    plugin.getResource("lang.yaml")!!,
                    StandardCharsets.UTF_8)
            )
        }
        val file = File("${plugin.dataFolder}/lang.yaml")
        val config = YamlConfiguration.loadConfiguration(file)
        if(config.contains("File_Version")) {
            val newestVersion = defaultConfig.getInt("File_Version")
            if(config.getInt("File_Version") < newestVersion) {
                catch<Exception>({throw LangUpdateFileException("There was an error updating your lang file to the latest configuration. $it")}) {
                    val section = defaultConfig.getConfigurationSection("")!!
                    var added = 0
                    section.getKeys(true).filterNot{section.isConfigurationSection(it) && config.contains(it)}
                        .forEach{config[it] = defaultConfig[it] ; added++}
                    config["File_Version"] = newestVersion
                    config.save(file)
                    plugin.log.info("Your Lang file was updated to a new version and had $added new keys added.")
                }
            }
            Prefix.values().forEach{prefixes[it] = MiniMessage.get().parse(config.getString(it.path) ?: defaultConfig.getString(it.path)!!)}
            config.getKeys(true).filterNot(config::isConfigurationSection)
                    .forEach { p ->
                        var key: MessageKey = Empty.EMPTY
                        MessageKey.values.forEach { if (it.path == p) key = it }
                        if (key == Empty.EMPTY) key = CustomMessageKey(p)
                        messageMap[key] = config.getString(p) ?: defaultConfig.getString(p)!!
                    }
            MessageKey.values.filter{config.getString(it.path) == null}.forEach{plugin.log.warning("Message with path ${it.path} wasn't found!")}
            ACFCoreMessage.values().forEach{ acm ->
                plugin.commandManager.locales.addMessage(
                Locale.ENGLISH,
                MessageKeys.valueOf(acm.name),
                LegacyComponentSerializer.legacyAmpersand().serialize(MiniMessage.builder().placeholderResolver {
                    prefixes[MessageKey.valueOf(it)]
                }.build().parse(config.getString(acm.path) ?: defaultConfig.getString(acm.path)!!))
            )}
            ACFMinecraftMessage.values().forEach{ amm ->
                plugin.commandManager.locales.addMessage(
                    Locale.ENGLISH,
                    MessageKeys.valueOf(amm.name),
                    LegacyComponentSerializer.legacyAmpersand().serialize(MiniMessage.builder().placeholderResolver {
                        prefixes[MessageKey.valueOf(it)]
                    }.build().parse(config.getString(amm.path) ?: defaultConfig.getString(amm.path)!!))
                )}

        } else throw LangNoVersionException("Couldn't find the [File_Version] in lang file, please make sure it is present")


        val endTime = System.currentTimeMillis()
        plugin.log.success("Loaded lang in ${(endTime - startTime)}ms")
    }

    fun close() {
        messageMap.clear()
        prefixes.clear()
    }
}