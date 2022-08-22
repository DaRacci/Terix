package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

// TODO -> Walk on powdered snow.
// TODO -> Cake!
class BlizzOrigin(override val plugin: Terix) : Origin() {

    override val name = "Blizz"
    override val colour = TextColor.fromHexString("#7ac2ff")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.panda.bite")
        sounds.deathSound = SoundEffect("entity.squid.death")
        sounds.ambientSound = SoundEffect("entity.skeleton_horse.ambient")

        damage {
            listOf(
                DamageCause.FIRE,
                DamageCause.LAVA,
                DamageCause.FIRE_TICK,
                DamageCause.HOT_FLOOR
            ) *= 2.5

            DamageCause.FREEZE /= 3
        }

        food {
            Material.SNOWBALL += { builder: FoodPropertyBuilder ->
                builder.canAlwaysEat = true
                builder.nutrition = 3
                builder.saturationModifier = 3f
                builder.fastFood = true
            }
            listOf(Material.TROPICAL_FISH, Material.PUFFERFISH, Material.SALMON, Material.COD) *= 3
            listOf(Material.COOKED_SALMON, Material.COOKED_COD) /= 3
        }

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
