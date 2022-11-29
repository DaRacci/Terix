package dev.racci.terix.core.integrations

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.collections.get
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer
import kotlinx.collections.immutable.persistentMapOf
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.entity.Player

/** For until the minix api is updated. */
public class TempPlaceholderExpansion(override val plugin: Terix) : PlaceholderExpansion(), WithPlugin<Terix> {
    private val placeholders = persistentMapOf<String, (Player) -> Any>(
        "origin_name" to { TerixPlayer[it].origin.name },
        "origin_display" to { TerixPlayer[it].origin.displayName },
        "origin_colour" to { TerixPlayer[it].origin.colour }
    )

    override fun persist(): Boolean = true
    override fun getIdentifier(): String = "terix"
    override fun getVersion(): String = plugin.description.version
    override fun getAuthor(): String = "Racci"

    override fun onPlaceholderRequest(
        player: Player,
        params: String
    ): String? = asString(placeholders.get(params, true).orNull()?.invoke(player))

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
