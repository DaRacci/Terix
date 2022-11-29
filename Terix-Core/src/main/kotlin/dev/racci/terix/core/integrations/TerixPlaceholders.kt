package dev.racci.terix.core.integrations

import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.integrations.placeholders.PlaceholderIntegration
import dev.racci.minix.api.integrations.placeholders.PlaceholderManager
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.player.TerixPlayer

@MappedIntegration(
    "PlaceholderAPI",
    Terix::class,
    PlaceholderManager::class
)
public class TerixPlaceholders(override val plugin: MinixPlugin) : PlaceholderIntegration() {

    init {
        registerOnlinePlaceholder("origin_name") { TerixPlayer[this].origin.name }
        registerOnlinePlaceholder("origin_display") { TerixPlayer[this].origin.displayName }
        registerOnlinePlaceholder("origin_colour") { TerixPlayer[this].origin.colour }
    }
}
