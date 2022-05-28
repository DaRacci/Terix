package dev.racci.terix.api

import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds

class Origin : AbstractOrigin() {

    override val plugin: MinixPlugin get() = null!!
    override val name = "TestOrigin"
    override val colour = NamedTextColor.AQUA!!
    override val fireImmune = true
    override val nightVision = true
    override val waterBreathing = true
    override val permission = "test.permission"
    override val becomeOriginTitle = TitleBuilder("<white><bold>Test".parse(), "<aqua><italic>Subtitle".parse())

    override suspend fun onRegister() {
        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 2.0
            Attribute.GENERIC_MOVEMENT_SPEED /= 4
            Pair(Trigger.DAY, Attribute.GENERIC_ATTACK_DAMAGE) += 2.0
        }
        title {
            Trigger.DAY += {
                title = "<green>Title".parse()
                subtitle = "<green>Subtitle".parse()
            }
        }
        potions {
            Trigger.NETHER += {
                type = PotionEffectType.REGENERATION
                duration = 5.seconds
                amplifier = 1
                ambient = true
            }
            Trigger.FLAMMABLE += {
                type = PotionEffectType.FIRE_RESISTANCE
                duration = 5.seconds
                amplifier = 1
                ambient = true
            }
        }
        damage {
            Trigger.WATER += 2.0
        }
    }
}
