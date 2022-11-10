package dev.racci.terix.core

import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.abilities.passives.RayCastingSupplier
import dev.racci.terix.core.integrations.TempPlaceholderExpansion
import org.incendo.interfaces.paper.PaperInterfaceListeners

@MappedPlugin(14443, Terix::class)
public class TerixImpl : Terix() {
    override suspend fun handleLoad() {
        TempPlaceholderExpansion(this).register()
    }

    override suspend fun handleEnable() {
        PaperInterfaceListeners.install(this)
    }
}
