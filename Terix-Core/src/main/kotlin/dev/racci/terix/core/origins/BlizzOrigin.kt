package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.passive.FluidWalker
import dev.racci.terix.api.origins.abilities.passive.TrailPassive
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.cooldown
import dev.racci.terix.api.origins.origin.keybinding
import dev.racci.terix.api.origins.origin.placementDuration
import dev.racci.terix.api.origins.origin.placementProvider
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.origins.abilities.keybind.AOEFreeze
import dev.racci.terix.core.origins.abilities.keybind.WallErector
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import kotlin.time.Duration.Companion.seconds

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

            DamageCause.FREEZE /= 3.0
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
            newBuilder<TrailPassive>()
                .parameter(TrailPassive::trailLength, 3)
                .parameter(TrailPassive::trailDuration, 1.seconds)
                .placementProvider(Material.SNOW)
                .build()

            newBuilder<FluidWalker>()
                .parameter(FluidWalker::replaceableMaterials, setOf(Material.WATER))
                .placementProvider(Material.FROSTED_ICE)
                .build()

            newBuilder<AOEFreeze>()
                .parameter(AOEFreeze::radius, 7.0)
                .parameter(AOEFreeze::freezeDuration, 3.seconds)
                .keybinding(KeyBinding.SNEAK_DOUBLE_OFFHAND)
                .cooldown(25.seconds)
                .build()

            newBuilder<WallErector>()
                .placementProvider(Material.PACKED_ICE)
                .placementDuration(5.seconds)
                .keybinding(KeyBinding.SNEAK_LEFT_CLICK)
                .cooldown(10.seconds)
                .build()
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
