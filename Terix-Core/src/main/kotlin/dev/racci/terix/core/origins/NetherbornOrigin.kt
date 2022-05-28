package dev.racci.terix.core.origins

import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.event.entity.EntityDamageEvent

class NetherbornOrigin(override val plugin: MinixPlugin) : AbstractOrigin() {

    override val name = "Netherborn"
    override val colour = NamedTextColor.LIGHT_PURPLE!!

    override val fireImmune = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.ravager.hurt")
        sounds.deathSound = SoundEffect("entity.shulker.death")
        sounds.ambientSound = SoundEffect("entity.polar_bear.ambient")

        damage {
            Trigger.WET += 2.0
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
    }
}
