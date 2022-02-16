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
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Inserting
import net.kyori.adventure.text.minimessage.tag.PreProcess
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import kotlin.properties.Delegates

const val LANG_DELIMITER = "."

class LangService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Lang Service"

    private val file by lazy { plugin.dataFolder.resolve("lang.yml") }

    private val yaml by lazy {
        Yaml(
            SerializersModule {},
            YamlConfiguration(
                encodeDefaults = true,
                strictMode = false,
                polymorphismStyle = PolymorphismStyle.Tag,
                polymorphismPropertyName = "type",
                encodingIndentationSize = 4,
                breakScalarsAt = 80,
                sequenceStyle = SequenceStyle.Flow
            )
        )
    }

    var lang by Delegates.notNull<Lang>(); private set

    override suspend fun handleEnable() {
        if (!file.exists()) {
            file.createNewFile()
            plugin.javaClass.getResourceAsStream("lang.yml")?.use { input ->
                file.outputStream().use(input::copyTo)
            }
        }
        lang = file.inputStream().use { input ->
            yaml.decodeFromStream(Lang.serializer(), input)
        }
    }

    override suspend fun handleUnload() {
        file.outputStream().use { output ->
            yaml.encodeToStream(Lang.serializer(), lang, output)
        }
    }

    operator fun get(key: String, vararg template: Pair<String, () -> Any>) = lang[key, template]

    @Serializable
    data class Lang(
        val prefixes: MutableMap<String, String>,
        val lang: MutableMap<String, String>,
        val originLang: MutableMap<String, String>,
    ) {

        val messageMap by lazy {
            val map = observableMapOf<String, String>()
            val prefixPlaceholders = persistentListOf<Pair<String, String>>()
            prefixes.forEach { (key, value) ->
                prefixPlaceholders.add("<prefix_$key>" to value)
            }
            lang.forEach { (key, value) ->
                map["lang$LANG_DELIMITER$key"] = value
            }
            originLang.forEach { (key, value) ->
                map["origin$LANG_DELIMITER$key"] = value
            }
            map.replaceAll { _, value ->
                prefixPlaceholders.fold(value) { acc, (placeholder, prefix) ->
                    acc.replace(placeholder, prefix, true)
                }
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
                    "lang" -> lang[split[1]] = entry.second
                    "origin" -> originLang[split[1]] = entry.second
                    "prefix" -> prefixes[split[1]] = entry.second
                }
            }
        }
    }

    companion object {
        private val components by lazy {
            val path = "net.kyori.adventure.text"
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
