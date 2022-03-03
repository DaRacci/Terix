package dev.racci.terix.core.services

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.SequenceStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.utils.collections.ObservableAction
import dev.racci.minix.api.utils.collections.observableMapOf
import dev.racci.terix.api.Terix
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Inserting
import net.kyori.adventure.text.minimessage.tag.PreProcess
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import kotlin.properties.Delegates
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

const val LANG_DELIMITER = "."

class LangService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Lang Service"

    private val file by lazy { plugin.dataFolder.resolve("Lang.yml") }

    private val yaml by lazy {
        Yaml(
            SerializersModule {},
            YamlConfiguration(
                encodeDefaults = true,
                strictMode = false,
                polymorphismStyle = PolymorphismStyle.Tag,
                polymorphismPropertyName = "type",
                encodingIndentationSize = 4,
                breakScalarsAt = 160,
                sequenceStyle = SequenceStyle.Flow
            )
        )
    }

    var lang by Delegates.notNull<Lang>(); private set

    override suspend fun handleEnable() {
        withContext(Dispatchers.IO) {
            val clf = plugin::class.memberFunctions.first { it.name == "getClassLoader" }
            clf.isAccessible = true
            val cl = clf.call(plugin) as ClassLoader
            val defaultResource = cl.getResource("lang.yml")!!
            val defaultInput = defaultResource.openStream().use { it.readBytes() }
            if (!file.exists()) {
                log.info { "Creating new lang file." }
                file.createNewFile()
                file.outputStream().use { it.write(defaultInput) }
                lang = yaml.decodeFromString(Lang.serializer(), defaultInput.decodeToString())
                return@withContext
            }
            val defaultLang = yaml.decodeFromString(Lang.serializer(), defaultInput.decodeToString())
            val presentLang = file.inputStream().use { yaml.decodeFromStream(Lang.serializer(), it) }

            lang = if (presentLang.version != defaultLang.version) {
                log.info { "Lang file is outdated. Updating from ${presentLang.version} to ${defaultLang.version}." }
                for ((key, value) in presentLang.generic.entries) {
                    if (defaultLang.generic[key] != null) {
                        log.debug { "Updating value from existing lang file: $key." }
                        defaultLang.generic[key] = value
                    } else log.debug { "Dropping value as it is no longer used: $key." }
                }
                for ((key, value) in presentLang.origins.entries) {
                    if (defaultLang.origins[key] != null) {
                        log.debug { "Updating value from existing lang file: $key." }
                        defaultLang.origins[key] = value
                    } else log.debug { "Dropping value as it is no longer used: $key." }
                }
                for ((key, value) in presentLang.prefix) {
                    log.debug { "Updating value from existing lang file: $key." }
                    defaultLang.prefix[key] = value
                }
                withContext(Dispatchers.IO) {
                    file.outputStream().use { yaml.encodeToStream(Lang.serializer(), defaultLang, it) }
                }
                defaultLang
            } else presentLang
        }
    }

    override suspend fun handleUnload() {
        file.outputStream().use { output ->
            yaml.encodeToStream(Lang.serializer(), lang, output)
        }
    }

    fun handleReload() {
        lang = file.inputStream().use { yaml.decodeFromStream(Lang.serializer(), it) }
    }

    operator fun get(key: String, vararg template: Pair<String, () -> Any>) = lang[key, template]

    @Serializable
    data class Lang(
        val prefix: MutableMap<String, String>,
        val generic: MutableMap<String, String>,
        val origins: MutableMap<String, String>,
        @SerialName("lang_version") val version: Int,
    ) {

        val messageMap by lazy {
            val map = observableMapOf<String, String>()
            val prefixPlaceholders = mutableMapOf<String, String>()
            prefix.forEach { (key, value) ->
                prefixPlaceholders["<prefix_$key>"] = value
            }
            generic.forEach { (key, value) ->
                map["generic$LANG_DELIMITER$key"] = value
            }
            origins.forEach { (key, value) ->
                map["origin$LANG_DELIMITER$key"] = value
            }
            map.replaceAll { _, value ->
                var string = value
                prefixPlaceholders.forEach { (placeholder, prefix) ->
                    string = string.replace(placeholder, prefix)
                }
                string
            }
            map
        }

        operator fun get(key: String, template: Array<out Pair<String, () -> Any>>): Component {
            val relevantString = messageMap[key] ?: error("Invalid key: $key")
            return MiniMessage.miniMessage().lazyPlaceholder(relevantString, template)
        }

        init {
            messageMap.observe(ObservableAction.REPLACE) { entry, _ ->
                val split = entry.first.split(LANG_DELIMITER)
                when (split[0]) {
                    "generic" -> generic[split[1]] = entry.second
                    "origin" -> origins[split[1]] = entry.second
                    "prefix" -> prefix[split[1]] = entry.second
                }
            }
        }
    }

    companion object {
        private val components by lazy {
            // This is so we don't have to worry about shading etc.
            val parent = Component::class.qualifiedName!!.split(".").dropLast(1).joinToString(".")
            val path = "() -> $parent"
            persistentListOf(
                "$path.Component",
                "$path.MiniMessage",
                "$path.TextComponent",
                "$path.KeybindComponent",
                "$path.TranslatableComponent"
            )
        }

        fun MiniMessage.lazyPlaceholder(
            input: String,
            template: Array<out Pair<String, () -> Any>>
        ) = deserialize(
            input,
            TagResolver.resolver(
                template.map {
                    TagResolver.resolver(
                        it.first,
                        if (it.second.toString() in components) {
                            LazyComponentReplacement { it.second() as Component }
                        } else LazyStringReplacement { it.second().toString() }
                    )
                }
            )
        )

        @Suppress("NonExtendableApiUsage")
        class LazyStringReplacement(private val value: () -> String) : Tag, PreProcess {
            override fun value() = value.invoke()
        }
        class LazyComponentReplacement(private val value: () -> Component) : Tag, Inserting {
            override fun value() = value.invoke()
        }
    }
}
