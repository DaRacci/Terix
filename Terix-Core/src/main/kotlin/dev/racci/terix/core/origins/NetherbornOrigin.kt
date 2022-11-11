package dev.racci.terix.core.origins

import dev.racci.minix.api.utils.kotlin.ifNotEmpty
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.passives.FluidWalker
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent

// TODO -> Spawn in the nether.
// TODO -> Flame particles.
// TODO -> Eating raw items gives the same as cooked food since hes hot.
// TODO -> Eating Cooked food gives less since it becomes overcooked.
// TODO -> Cake.
// TODO -> holding food cooks it
public class NetherbornOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Netherborn"
    override val colour: TextColor = TextColor.fromHexString("#ff5936")!!

    override var fireImmunity: Boolean = true

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.ravager.hurt")
        sounds.deathSound = SoundEffect("entity.shulker.death")
        sounds.ambientSound = SoundEffect("entity.polar_bear.ambient")

        food {
            listOf(MaterialTagsExtension.VEGETABLES, MaterialTagsExtension.CARBS, MaterialTagsExtension.FRUITS) += dslMutator<FoodPropertyBuilder> {
                saturationModifier = 0.3f
                nutrition = 1
            }
            exchangeFoodProperties(Material.COOKED_BEEF, Material.BEEF)
            exchangeFoodProperties(Material.COOKED_CHICKEN, Material.CHICKEN)
            exchangeFoodProperties(Material.COOKED_MUTTON, Material.MUTTON)
            exchangeFoodProperties(Material.COOKED_PORKCHOP, Material.PORKCHOP)
            exchangeFoodProperties(Material.COOKED_RABBIT, Material.RABBIT)
            exchangeFoodProperties(Material.COOKED_SALMON, Material.SALMON)
            exchangeFoodProperties(Material.COOKED_COD, Material.COD)
            exchangeFoodProperties(Material.COOKED_SALMON, Material.SALMON)
        }
        damage {
            listOf(
                State.WeatherState.RAIN,
                State.LiquidState.WATER
            ) += 0.5
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.MELTING,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR
            ) *= 0.0
        }
        item {
            material = Material.LAVA_BUCKET
            lore = "<light_purple>A breath of fire that can be used to summon a dragon."
        }

        abilities {
            withPassive<FluidWalker> {
                this.fluidType = Material.LAVA
                this.replacement = Material.CRYING_OBSIDIAN
            }
        }
    }

    override suspend fun handleLoad(player: Player): Unit = ensureDrySponge(player)

    @OriginEventSelector(EventSelector.OFFENDER)
    public fun EntityDamageByEntityEvent.handle() {
        if (damager.fireTicks <= 0) return

        damage *= 1.2
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityPickupItemEvent.handle() {
        if (this.item.itemStack.type != Material.WET_SPONGE) return
        this.item.itemStack.type = Material.SPONGE
    }

    private fun ensureDrySponge(player: Player) {
        player.inventory
            .filterNotNull()
            .filter { item -> item.type == Material.WET_SPONGE }
            .onEach { item -> item.type = Material.SPONGE }
            .ifNotEmpty { player.updateInventory() }
    }
}
