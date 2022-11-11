package dev.racci.terix.api

import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.states.State
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.attribute.Attribute
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds

class TestOrigin : Origin() {

    override val plugin: MinixPlugin get() = null!!
    override val name = "TestOrigin"
    override val colour = NamedTextColor.AQUA!!
    override var fireImmunity = true
    override var waterBreathing = true
    override val becomeOriginTitle = TitleBuilder("<white><bold>Test".parse(), "<aqua><italic>Subtitle".parse())

    override suspend fun handleRegister() {
        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.5
            Attribute.GENERIC_MOVEMENT_SPEED /= 4
            Pair(State.TimeState.DAY, Attribute.GENERIC_ATTACK_DAMAGE) += 2.0
        }
        title {
            State.TimeState.DAY += {
                title = "<green>Title".parse()
                subtitle = "<green>Subtitle".parse()
            }
        }
        potions {
            State.WorldState.NETHER += dslMutator {
                type = PotionEffectType.REGENERATION
                duration = 5.seconds
                amplifier = 1
                ambient = true
            }
            State.BiomeState.COLD += dslMutator {
                type = PotionEffectType.FIRE_RESISTANCE
                duration = 5.seconds
                amplifier = 1
                ambient = true
            }
        }
        damage {
            State.LiquidState.WATER += 2.0
        }
    }
}
