package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageEvent.DamageCause

class DragonOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Dragon"
    override val colour = NamedTextColor.LIGHT_PURPLE!!
    override val fireImmune = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.hoglin.angry")
        sounds.deathSound = SoundEffect("entity.ravager.stunned")
        sounds.ambientSound = SoundEffect("entity.strider.ambient")

        damage {
            Trigger.WET += 2.0
            listOf(
                DamageCause.LAVA,
                DamageCause.FIRE,
                DamageCause.MELTING,
                DamageCause.FIRE_TICK,
                DamageCause.HOT_FLOOR
            ) *= 0.0
        }
        item {
            material = Material.LARGE_AMETHYST_BUD
            lore = "<light_purple>A breath of fire that can be used to summon a dragon."
        }
    }

    // TODO: Explosion when shield is broken
}
