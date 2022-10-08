package dev.racci.terix.core.origins

import dev.racci.minix.api.events.PlayerMoveXYZEvent
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

// TODO -> Walk on powdered snow.
// TODO -> Cake!
class BlizzOrigin(override val plugin: Terix) : Origin() {

    override val name = "Blizz"
    override val colour = TextColor.fromHexString("#7ac2ff")!!

    override suspend fun handleRegister() {
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
            Material.SNOWBALL += dslMutator<FoodPropertyBuilder> {
                canAlwaysEat = true
                nutrition = 3
                saturationModifier = 3f
                fastFood = true
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

    @OriginEventSelector(EventSelector.PLAYER)
    fun PlayerMoveXYZEvent.handle() {
        val belowBlock = player.location.block.getRelative(BlockFace.DOWN)
        if (belowBlock.type != Material.POWDER_SNOW) return
        logger.debug { "PlayerY: ${player.location.y} | BlockY: ${belowBlock.location.y}" }

        val keepAboveLocation = player.location.clone()
    }
}
