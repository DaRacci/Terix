package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class FairyOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Fairy"
    override val colour = NamedTextColor.YELLOW!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.puffer_fish.death")
        sounds.deathSound = SoundEffect("entity.glow_squid.squirt")

        item {
            material = Material.GLOWSTONE_DUST
            lore = "<yellow>A magical dust that can be used to create a fairy."
        }
    }
}
