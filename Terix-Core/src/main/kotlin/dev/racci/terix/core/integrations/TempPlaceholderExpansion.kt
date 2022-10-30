package dev.racci.terix.core.integrations

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.collections.get
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import kotlinx.collections.immutable.persistentMapOf
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

/** For until the minix api is updated. */
class TempPlaceholderExpansion(override val plugin: Terix) : PlaceholderExpansion(), WithPlugin<Terix> {
    private val placeholders = persistentMapOf<String, (Player) -> Any>(
        "origin_name" to { TerixPlayer.cachedOrigin(it).name },
        "origin_display" to { TerixPlayer.cachedOrigin(it).displayName },
        "origin_colour" to { TerixPlayer.cachedOrigin(it).colour }
    )

    override fun persist(): Boolean = true
    override fun getIdentifier(): String = "terix"
    override fun getVersion(): String = plugin.description.version
    override fun getAuthor(): String = "Racci"

    override fun onPlaceholderRequest(
        player: Player,
        params: String
    ): String? {
        logger.debug { placeholders }
        logger.debug { "Placeholder request: $params" }
        val placeholder = placeholders.get(params, true).orNull() ?: return null

        val result = asString(placeholder(player))
        logger.debug { "Placeholder result: $result" }

        return result
    }

    private fun asString(value: Any?): String? = when (value) {
        is String -> value
        is Component -> LegacyComponentSerializer.legacySection().serialize(value)
        null -> null
        else -> {
            warning("Placeholder value is not a string or component: ${value::class.qualifiedName}")
            null
        }
    }
}
