package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

// TODO -> Raw fish raw fish raw fish raw fish, kelp no dried, cake.
// TODO -> More damage from being beaten with a fishing rod (Similar to sword damage)
class AxolotlOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Axolotl"
    override val colour = TextColor.fromHexString("#ff6ea8")!!

    override var waterBreathing = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.axolotl.hurt")
        sounds.deathSound = SoundEffect("entity.axolotl.death")
        sounds.ambientSound = SoundEffect("entity.axolotl.ambient")

        food {
            listOf(
                Material.SALMON,
                Material.COD,
                Material.PUFFERFISH,
                Material.TROPICAL_FISH,
                Material.KELP
            ) *= 2.0
        }

        item {
            material = Material.AXOLOTL_BUCKET
            lore = "<white>Little shit."
        }
    }
}
