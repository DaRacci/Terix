package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.potion.PotionEffectType

// TODO -> Better trades with villagers
// TODO -> Tames animals faster
// TODO -> Agriculture
// TODO -> Get sick from eating raw meat / fish and then they shit themselves.
// TODO -> Increase cake.
class HumanOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Human"
    override val colour = TextColor.fromHexString("#ff3838")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.player.hurt")
        sounds.deathSound = SoundEffect("entity.player.death")
        sounds.ambientSound = SoundEffect("entity.player.burp")

        potions {
            Trigger.ON += {
                type = PotionEffectType.DAMAGE_RESISTANCE
                durationInt = Int.MAX_VALUE
                amplifier = 1
                ambient = true
            }
        }
        title {
            Trigger.DAY += {
                title = "<blue>Day".parse()
                subtitle = "<aqua>haha its bright blue now".parse()
            }
        }
        item {
            material = Material.APPLE
            lore = "This is a <blue>blue"
        }
    }
}
