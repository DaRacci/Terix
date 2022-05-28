package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.abilities.Levitate
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.event.entity.EntityDamageEvent

class AethenOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Aethen"
    override val colour = TextColor.fromHexString("#fff6cc")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.silverfish.hurt")
        sounds.deathSound = SoundEffect("entity.turtle.death_baby")
        sounds.ambientSound = SoundEffect("entity.vex.ambient")

        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.80
            Pair(Trigger.NETHER, Attribute.GENERIC_MAX_HEALTH) *= 0.50
        }
        damage {
            EntityDamageEvent.DamageCause.FALL *= 0.0
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.LIGHTNING,
            ) *= 2.0
        }
        item {
            material = Material.FEATHER
            lore = "<yellow>The Angel of the sky!"
        }
        abilities {
            KeyBinding.DOUBLE_OFFHAND.add<Levitate>()
        }
    }
}
