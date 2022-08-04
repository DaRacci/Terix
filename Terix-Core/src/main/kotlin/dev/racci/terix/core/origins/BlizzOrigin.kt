package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material

// TODO -> Edible snowballs are the top food.
// TODO -> Raw fish are better than cooked fish.
// TODO -> Cake!
// TODO -> More fire / lava damage
class BlizzOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Blizz"
    override val colour = TextColor.fromHexString("#7ac2ff")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.panda.bite")
        sounds.deathSound = SoundEffect("entity.squid.death")
        sounds.ambientSound = SoundEffect("entity.skeleton_horse.ambient")

        item {
            material = Material.POWDER_SNOW_BUCKET
            lore = """
                <gold>A magical snowball that will 
                <green>freeze</green> any player that
                <red>touches it
            """.trimIndent()
        }
    }
}
