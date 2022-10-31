package dev.racci.terix.core.integrations

import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.integrations.placeholders.PlaceholderIntegration
import dev.racci.minix.api.integrations.placeholders.PlaceholderManager
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer

@MappedIntegration(
    "PlaceholderAPI",
    Terix::class,
    PlaceholderManager::class
)
public class TerixPlaceholders(override val plugin: MinixPlugin) : PlaceholderIntegration() {

    init {
        registerOnlinePlaceholder("origin_name") { TerixPlayer.cachedOrigin(this).name }
        registerOnlinePlaceholder("origin_display") { TerixPlayer.cachedOrigin(this).displayName }
        registerOnlinePlaceholder("origin_colour") { TerixPlayer.cachedOrigin(this).colour }
    }
}
