package dev.racci.terix.core.integrations

import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchants.EcoEnchant
import com.willfp.ecoenchants.target.EnchantLookup.hasEnchantActiveInSlot
import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.pluginManager
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.integrations.Integration
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.OriginSunlightBurnEvent
import dev.racci.terix.core.TerixImpl
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import java.util.zip.ZipInputStream

@MappedIntegration(
    "EcoEnchants",
    Terix::class
)
public class EcoEnchantsIntegration(override val plugin: MinixPlugin) : Integration {
    override suspend fun handleLoad() {
        extractDefaultAssets()
        EnchantmentSunProtection(pluginManager.getPlugin("EcoEnchants").castOrThrow())
    }

    private fun extractDefaultAssets() {
        val src = TerixImpl::class.java.protectionDomain.codeSource
        val jarLoc = src.location
        val ecoEnchantsFolder = server.pluginsFolder.resolve("EcoEnchants")

        runCatching {
            logger.info { "Extracting default assets..." }
            ZipInputStream(jarLoc.openStream()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    val name = entry.name

                    if (entry.isDirectory || !name.startsWith("enchants/")) continue

                    val dest = ecoEnchantsFolder.resolve(name)
                    if (!dest.exists()) {
                        dest.parentFile.mkdirs()
                        dest.createNewFile()
                        logger.debug { "Extracting $name" }
                        plugin.getResource(name)!!.use { input -> dest.outputStream().use(input::copyTo) }
                    }
                }
            }
            logger.info { "Finished extracting default assets." }
        }.onFailure { err -> logger.error(err) { "Failed to extract default assets." } }
    }

    private class EnchantmentSunProtection(plugin: EcoEnchantsPlugin) : EcoEnchant(
        "sun_protection",
        plugin,
        false
    ) {
        override fun onInit() {
            this.registerListener(object : Listener {
                @EventHandler(priority = EventPriority.LOWEST)
                fun handle(event: OriginSunlightBurnEvent) {
                    if (!event.player.hasEnchantActiveInSlot(enchant, HELMET_SLOT)) return
                    event.cancel()
                }
            })
        }
    }

    private companion object {
        const val HELMET_SLOT = 103
    }
}
