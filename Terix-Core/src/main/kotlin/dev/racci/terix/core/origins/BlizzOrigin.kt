package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.passive.FluidWalker
import dev.racci.terix.api.origins.abilities.passive.TrailPassive
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

// TODO -> Walk on powdered snow.
// TODO -> Cake!
// TODO -> Freeze ability
public class BlizzOrigin(override val plugin: Terix) : Origin() {
    override val name: String = "Blizz"
    override val colour: TextColor = TextColor.fromHexString("#7ac2ff")!!

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
            MaterialTags.RAW_FISH *= 3
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

        abilities {
            withPassive<TrailPassive> {
                this.material = Material.SNOW
            }

            withPassive<FluidWalker> {
                this.fluidType = Material.WATER
                this.replacement = Material.FROSTED_ICE
            }
        }
    }

    override suspend fun handleLoad(player: Player) {
        player.freezeTicks = 0
        player.lockFreezeTicks(true)
    }

    override suspend fun handleChangeOrigin(player: Player) {
        player.lockFreezeTicks(false)
    }

    @OriginEventSelector(EventSelector.OFFENDER)
    public fun EntityDamageByEntityEvent.handle() {
        entity.freezeTicks += 20
    }
}
