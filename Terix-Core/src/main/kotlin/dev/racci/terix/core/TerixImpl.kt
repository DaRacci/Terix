package dev.racci.terix.core

import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.core.integrations.TempPlaceholderExpansion

@MappedPlugin(14443, Terix::class)
class TerixImpl : Terix() {
    override suspend fun handleLoad() {
        TempPlaceholderExpansion(this).register()
    }
}
