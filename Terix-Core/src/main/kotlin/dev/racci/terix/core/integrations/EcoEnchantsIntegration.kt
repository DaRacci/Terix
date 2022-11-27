package dev.racci.terix.core.integrations

import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchants.EcoEnchant
import com.willfp.ecoenchants.target.EnchantLookup.hasEnchantActiveInSlot
import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.pluginManager
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.OriginSunlightBurnEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

@MappedIntegration(
    "EcoEnchants",
    Terix::class
)
public class EcoEnchantsIntegration(override val plugin: MinixPlugin) : FileExtractorIntegration() {
    override suspend fun handleLoad() {
        extractDefaultAssets()
        EnchantmentSunProtection(pluginManager.getPlugin("EcoEnchants").castOrThrow())
    }

    override fun filterResource(name: String): Boolean = name.startsWith("enchants/")

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
