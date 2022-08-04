package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

// TODO -> Every meat gives increased saturation.
// TODO -> Always have hunger.
// TODO -> Diamond, Gold and Emerald are edible.
// TODO -> Take more damage from beds.
// TODO -> Bed explosion does more damage.
class DragonOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Dragon"
    override val colour = TextColor.fromHexString("#9e33ff")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.hoglin.angry")
        sounds.deathSound = SoundEffect("entity.ravager.stunned")
        sounds.ambientSound = SoundEffect("entity.strider.ambient")

        item {
            material = Material.LARGE_AMETHYST_BUD
            lore = "<light_purple>A breath of fire that can be used to summon a dragon."
        }
    }

    // TODO: Explosion when shield is broken
}
