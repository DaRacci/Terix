package dev.racci.terix.core.integrations

import com.willfp.ecoenchants.EcoEnchantsPlugin
import com.willfp.ecoenchants.enchants.EcoEnchant
import com.willfp.ecoenchants.enchants.EcoEnchants
import com.willfp.ecoenchants.target.EnchantLookup.hasEnchantInSlot
import dev.racci.minix.api.annotations.MappedIntegration
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.pluginManager
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.integrations.Integration
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.OriginSunlightBurnEvent

@MappedIntegration(
    "EcoEnchants",
    Terix::class
)
class EcoEnchantsIntegration(override val plugin: MinixPlugin) : Integration {
    private val sunProtection = SunProtectionEnchant(pluginManager.getPlugin("EcoEnchants").castOrThrow())

    override suspend fun handleLoad() {
        EcoEnchants.register(sunProtection)
    }

    override suspend fun handleEnable() {
        event<OriginSunlightBurnEvent> {
            if (!this.player.hasEnchantInSlot(sunProtection, HELMET_SLOT)) return@event
            this.cancel()
        }
    }

    private class SunProtectionEnchant(plugin: EcoEnchantsPlugin) : EcoEnchant(
        "sun_protection",
        plugin,
        force = true
    )

    private companion object {
        const val HELMET_SLOT = 103
    }
}
