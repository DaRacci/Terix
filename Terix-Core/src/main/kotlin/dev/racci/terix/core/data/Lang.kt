package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.IConfig
import dev.racci.minix.api.utils.safeCast
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.Terix
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Inserting
import net.kyori.adventure.text.minimessage.tag.PreProcess
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

@ConfigSerializable
@MappedConfig(
    Terix::class, "Lang.conf",
    [Lang.PartialComponent::class, Lang.PartialComponent.Serializer::class]
)
class Lang : IConfig() {

    @Transient override val loadCallback = {
        val map = prefixes.mapKeys {
            if (!it.key.matches(prefixRegex)) "<prefix_${it.key}" else it.key
        }
        generic::class.memberProperties
            .filterIsInstance<PartialComponent>()
            .forEach {
                it.formatRaw(map)
            }
    }

    val prefixes: PersistentMap<String, String> = persistentMapOf(
        "<prefix_terix>" to "<light_purple>Terix</light_purple> » <aqua>",
        "<prefix_server>" to "<light_purple>Elixir</light_purple> » <aqua>",
        "<prefix_origins>" to "<gold>Origins</gold> » <aqua>",
    )

    var generic: Generic = Generic()

    var origin: Origin = Origin()

    operator fun get(key: String, vararg placeholder: Pair<String, () -> Any>): Component {
        val keys = key.split(".")
        if (keys.size <= 1) return MiniMessage.miniMessage().deserialize("Invalid key: $key")

        val prop = Lang::class.memberProperties.find { it.name == keys[0] } ?: return MiniMessage.miniMessage().deserialize("No Property found for $key")
        val value = prop.get(this).safeCast<PropertyFinder<*, PartialComponent>>() ?: return MiniMessage.miniMessage().deserialize("$key's return type is not a property finder class.")
        return value.get(key.substringAfter('.')).get(*placeholder)
    }

    sealed class PropertyFinder <C : PropertyFinder<C, R>, R : Any?> {
        @Transient
        private val map: PersistentMap<String, KProperty1<C, R>> = persistentMapOf(
            *Lang::class.memberProperties
                .filterIsInstance<KProperty1<C, R>>()
                .map {
                    val builder = StringBuilder()
                    for ((index, char) in it.name.withIndex()) {
                        if (index == 0 || char.isLowerCase()) {
                            builder.append(char)
                            continue
                        }
                        builder.append(".${char.lowercase()}")
                    }
                    builder.toString() to it
                }.toTypedArray()
        )

        fun get(key: String): R = map[key]?.get(this@PropertyFinder.unsafeCast()) ?: throw IllegalArgumentException("No property found for $key")
    }

    @ConfigSerializable
    class Generic : PropertyFinder<Generic, PartialComponent>() {

        var error: PartialComponent = PartialComponent.of("<dark_red>Error <white>» <red><message>")

        var reloadLang: PartialComponent = PartialComponent.of("<prefix_terix>Reloaded Language file.")
    }

    @ConfigSerializable
    class Origin : PropertyFinder<Generic, PartialComponent>() {

        var broadcast: PartialComponent = PartialComponent.of("<prefix_server><player> has become the <new_origin> origin!")

        var setSelf: PartialComponent = PartialComponent.of("<prefix_origins>You set your origin to <new_origin>.")

        var setOther: PartialComponent = PartialComponent.of("<prefix_origins>Set <target>'s origin to <new_origin>.")

        var setSameSelf: PartialComponent = PartialComponent.of("<prefix_origins>You are already the <origin> origin!")

        var setSameOther: PartialComponent = PartialComponent.of("<prefix_origins><player> is already the <origin> origin!")

        var getSelf: PartialComponent = PartialComponent.of("<prefix_origins>Your origin is <origin>.")

        var getOther: PartialComponent = PartialComponent.of("<prefix_origins><target>'s origin is <origin>.")

        var nightVision: PartialComponent = PartialComponent.of("<prefix_origins>Your night vision now triggers on: <new_nightvision>.")
    }

    class PartialComponent private constructor(private var raw: String) {
        private var _value = raw
        private var dirty = true
        private var cache: Component? = null

        val value: Component
            get() {
                if (dirty) {
                    cache = MiniMessage.miniMessage().deserialize(_value)
                    dirty = false
                }
                return cache!!
            }

        operator fun get(vararg placeholder: Pair<String, () -> Any>): Component = if (placeholder.isEmpty()) {
            value
        } else MiniMessage.miniMessage().lazyPlaceholder(_value, placeholder)

        fun formatRaw(placeholders: Map<String, String>) {
            placeholders.forEach { (placeholder, prefix) ->
                _value = raw.replace(placeholder, prefix) // We use raw incase a prefix was removed.
            }
            dirty = true
            cache = null
        }

        companion object {

            fun of(raw: String): PartialComponent {
                return PartialComponent(raw)
            }
        }

        object Serializer : TypeSerializer<PartialComponent> {

            override fun deserialize(
                type: Type,
                node: ConfigurationNode,
            ): PartialComponent = node.get<String>()?.let(PartialComponent::of) ?: throw SerializationException(type, "Null Partial Component: ${node.path()}")

            override fun serialize(
                type: Type,
                obj: PartialComponent?,
                node: ConfigurationNode,
            ) {
                if (obj == null) { node.raw(null); return }
                node.set(obj.raw)
            }
        }
    }

    companion object {
        private val prefixRegex = Regex("<prefix_(.*)>")

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

        private fun MiniMessage.lazyPlaceholder(
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

        class LazyStringReplacement(private val value: () -> String) : Tag, PreProcess {
            override fun value() = value.invoke()
        }

        class LazyComponentReplacement(private val value: () -> Component) : Tag, Inserting {
            override fun value() = value.invoke()
        }
    }
}
