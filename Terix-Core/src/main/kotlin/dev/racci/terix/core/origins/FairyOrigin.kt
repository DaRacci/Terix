package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

// TODO -> Sweet berries, cake and glow berries are top.
// TODO -> Beetroot, apple, carrots, fruits.
// TODO -> If they eat potatoes they get fat.
// TODO -> Take more damage from arrows.
class FairyOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Fairy"
    override val colour = TextColor.fromHexString("#86ff93")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.puffer_fish.death")
        sounds.deathSound = SoundEffect("entity.glow_squid.squirt")

        item {
            material = Material.GLOWSTONE_DUST
            lore = "<yellow>A magical dust that can be used to create a fairy."
        }
    }
}
