package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material

class AxolotlOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Axolotl"
    override val colour = NamedTextColor.WHITE!!

    override val waterBreathing by lazy { true }

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.axolotl.hurt")
        sounds.deathSound = SoundEffect("entity.axolotl.death")
        sounds.ambientSound = SoundEffect("entity.axolotl.ambient")

        item {
            material = Material.AXOLOTL_BUCKET
            lore = "<white>Little shit."
        }
    }
}
