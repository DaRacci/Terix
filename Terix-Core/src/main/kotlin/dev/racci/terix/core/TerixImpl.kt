package dev.racci.terix.core

import dev.racci.minix.api.annotations.MappedPlugin
import dev.racci.terix.api.Terix

@MappedPlugin(14443, Terix::class)
class TerixImpl : Terix() {

    override suspend fun handleLoad() {
        if (!dataFolder.exists()) {
            dataFolder.mkdir()
        }
        // TODO: Fix this
//        try {
//            if (exists("com.willfp.ecoenchants.EcoEnchantsPlugin")) SunResistance()
//        } catch (e: NullPointerException) {
//            log.error(e) { "Failed to load EcoEnchants" }
//        }
    }
}
