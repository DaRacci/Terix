package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Arrow
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

// TODO: Cake
class FairyOrigin(override val plugin: Terix) : Origin() {

    override val name = "Fairy"
    override val colour = TextColor.fromHexString("#86ff93")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.puffer_fish.death")
        sounds.deathSound = SoundEffect("entity.glow_squid.squirt")

        damage {
            DamageCause.PROJECTILE += {
                if (it.entity !is Arrow) it.damage *= 0.5
            }
        }

        food {
            TOP_FOODS *= 2.25
            MID_FOODS *= 1.50
            FAT_FOODS += dslMutator<TimedAttributeBuilder> {
                attribute = Attribute.GENERIC_MOVEMENT_SPEED
                amount = 0.8
            }
        }

        item {
            material = Material.GLOWSTONE_DUST
            lore = "<yellow>A magical dust that can be used to create a fairy."
        }
    }

    companion object {
        val TOP_FOODS = listOf(Material.BEETROOT, Material.APPLE, Material.GOLDEN_APPLE, Material.ENCHANTED_GOLDEN_APPLE, Material.CARROT, Material.CHORUS_FRUIT)
        val MID_FOODS = listOf(Material.SWEET_BERRIES, Material.GLOW_BERRIES)
        val FAT_FOODS = listOf(Material.POTATO, Material.POISONOUS_POTATO)
    }
}
