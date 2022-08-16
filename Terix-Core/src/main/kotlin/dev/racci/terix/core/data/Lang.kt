package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.IConfig
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.utils.safeCast
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.Terix
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Inserting
import net.kyori.adventure.text.minimessage.tag.PreProcess
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.command.CommandSender
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.kotlin.extensions.get
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.serialize.TypeSerializer
import java.lang.reflect.Type
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

// TODO -> Fix elixir gradient and make bold
@ConfigSerializable
@MappedConfig(
    Terix::class,
    "Lang.conf",
    [Lang.PartialComponent::class, Lang.PartialComponent.Serializer::class]
)
class Lang : IConfig(), WithPlugin<Terix>, KoinComponent {
    @delegate:Transient
    override val plugin: Terix by inject()

    private fun <T : Any> getNested(instance: T): List<Pair<Any, KProperty1<Any, PropertyFinder<PartialComponent>>>> {
        return instance::class.declaredMemberProperties
            .filter { it.returnType.isSubtypeOf(typeOf<PropertyFinder<*>>()) }
            .filterIsInstance<KProperty1<T, PropertyFinder<PartialComponent>>>()
            .map { instance to it }.unsafeCast()
    }

    override fun loadCallback() {
        val map = prefixes.mapKeys {
            if (!it.key.matches(prefixRegex)) "<prefix_${it.key}>" else it.key
        }

        val initialNested = getNested(this)
        val queue = ArrayDeque(initialNested)
        while (queue.isNotEmpty()) {
            val (instance, property) = queue.removeFirst()

            val propInstance = try {
                property.get(instance)
            } catch (e: ClassCastException) {
                continue
            }

            val nested = getNested(propInstance)
            if (nested.isNotEmpty()) {
                queue.addAll(nested)
            }

            propInstance::class.declaredMemberProperties
                .filterIsInstance<KProperty1<PropertyFinder<PartialComponent>, PartialComponent>>()
                .forEach {
                    try {
                        it.get(propInstance).formatRaw(map)
                    } catch (e: ClassCastException) {
                        return@forEach
                    }
                }
        }
    }

    val prefixes: Map<String, String> = mapOf(
        "<prefix_terix>" to "<light_purple>Terix</light_purple> » <aqua>",
        "<prefix_server>" to "<light_purple>Elixir</light_purple> » <aqua>",
        "<prefix_origins>" to "<gold>Origins</gold> » <aqua>"
    )

    var generic: Generic = Generic()

    var origin: Origin = Origin()

    var database: Database = Database()

    operator fun get(key: String, vararg placeholder: Pair<String, () -> Any>): Component {
        val keys = key.split(".")
        if (keys.size <= 1) return MiniMessage.miniMessage().deserialize("Invalid key: $key")

        val prop = Lang::class.declaredMemberProperties.find { it.name == keys[0] } ?: return MiniMessage.miniMessage().deserialize("No Property found for $key")
        val value = prop.get(this).safeCast<PropertyFinder<PartialComponent>>() ?: return MiniMessage.miniMessage().deserialize("$key's return type is not a property finder class.")

        return value[key.substringAfter('.')].get(*placeholder)
    }

    abstract class PropertyFinder<R> {
        @Transient
        private val propertyMap: PersistentMap<String, KProperty1<Any, R>>

        operator fun get(key: String): R = propertyMap[key]?.get(this) ?: throw IllegalArgumentException("No property found for $key")

        init {
            val properties = this::class.declaredMemberProperties.filterIsInstance<KProperty1<Any, R>>()
            propertyMap = properties.associateBy { property ->
                buildString {
                    for ((index, char) in property.name.withIndex()) {
                        if (index == 0 || char.isLowerCase()) {
                            append(char)
                            continue
                        }

                        append('.').append(char.lowercaseChar())
                    }
                }
            }.toPersistentMap()
        }
    }

    @ConfigSerializable
    class Generic : PropertyFinder<PartialComponent>() {

        var error: PartialComponent = PartialComponent.of("<dark_red>Error <white>» <red><message>")

        var reloadLang: PartialComponent = PartialComponent.of("<prefix_terix>Reloaded Language file.")
    }

    @ConfigSerializable
    class Origin : PropertyFinder<PartialComponent>() {

        var broadcast: PartialComponent = PartialComponent.of("<prefix_server><player> has become the <new_origin> origin!")

        var setSelf: PartialComponent = PartialComponent.of("<prefix_origins>You set your origin to <new_origin>.")

        var setOther: PartialComponent = PartialComponent.of("<prefix_origins>Set <player>'s origin to <new_origin>.")

        var setSameSelf: PartialComponent = PartialComponent.of("<prefix_origins>You are already the <origin> origin!")

        var setSameOther: PartialComponent = PartialComponent.of("<prefix_origins><player> is already the <origin> origin!")

        var getSelf: PartialComponent = PartialComponent.of("<prefix_origins>Your origin is <origin>.")

        var getOther: PartialComponent = PartialComponent.of("<prefix_origins><player>'s origin is <origin>.")

        var nightVision: PartialComponent = PartialComponent.of("<prefix_origins>Your night vision now triggers on: <new_nightvision>.")

        var onChangeCooldown: PartialComponent = PartialComponent.of("<prefix_origins>You can't change your origin for another <cooldown>.")

        var bee: Bee = Bee()

        @ConfigSerializable
        class Bee : PropertyFinder<PartialComponent>() {
            var potion: PartialComponent = PartialComponent.of("<prefix_origins>The potion is too strong for you, try a flower instead.")
        }
    }

    @ConfigSerializable
    class Database : PropertyFinder<PartialComponent>() {

        var choicesSelf: PartialComponent = PartialComponent.of("<prefix_origins>You <changed> have <choices> remaining choices.")

        var choicesOther: PartialComponent = PartialComponent.of("<prefix_origins><player><changed> has <choices> remaining choices.")
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
            var tmp = raw
            placeholders.forEach { (placeholder, prefix) ->
                tmp = tmp.replaceFirst(placeholder, prefix)
            }
            _value = tmp
            dirty = true
            cache = null
        }

        companion object {

            fun of(raw: String): PartialComponent {
                return PartialComponent(raw)
            }

            infix fun PartialComponent.message(recipient: CommandSender) = recipient.msg(this.get())
        }

        object Serializer : TypeSerializer<PartialComponent> {

            override fun deserialize(
                type: Type,
                node: ConfigurationNode
            ): PartialComponent = node.get<String>()?.let(PartialComponent::of) ?: throw SerializationException(type, "Null Partial Component: ${node.path()}")

            override fun serialize(
                type: Type,
                obj: PartialComponent?,
                node: ConfigurationNode
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
                    val str = it.second.toString()
                    TagResolver.resolver(
                        it.first,
                        when (str) {
                            in components -> LazyComponentReplacement { it.second().unsafeCast() }
                            else -> LazyStringReplacement { it.second().toString() }
                        }
                    )
                }
            )
        )

        class LazyStringReplacement(private val value: () -> String) : Tag, PreProcess {
            override fun value() = value.invoke()
        }

        class LazyComponentReplacement(private val value: () -> Component) : Tag, Inserting {
            override fun value() = value.invoke()

            override fun allowsChildren() = false
        }
    }
}
