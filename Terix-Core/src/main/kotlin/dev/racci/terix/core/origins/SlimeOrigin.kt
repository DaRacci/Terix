package dev.racci.terix.core.origins

import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.isSword
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.data.ItemMatcher
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.extensions.handle
import dev.racci.terix.api.origins.abilities.keybind.Leap
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.cooldown
import dev.racci.terix.api.origins.origin.keybinding
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.origins.abilities.passive.WaterHealthBonus
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO -> CAke.
public class SlimeOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Slime"
    override val colour: TextColor = TextColor.fromHexString("#61f45a")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.slime.hurt")
        sounds.deathSound = SoundEffect("entity.slime.death")
        sounds.ambientSound = SoundEffect("entity.slime.ambient")

        potions {
            State.CONSTANT += dslMutator {
                type = PotionEffectType.JUMP
                amplifier = 4
                duration = kotlin.time.Duration.INFINITE
                ambient = true
            }
        }
        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.8
        }
        damage {
            EntityDamageEvent.DamageCause.FALL += { cause ->
                if (cause.entity.castOrThrow<Player>().handle.random.nextBoolean()) {
                    cause.cancel()
                }
            }
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR
            ) *= 1.5
        }
        item {
            material = Material.SLIME_BALL
            lore = "<green>Slime lol"
        }
        food {
            ItemMatcher {
                this.type == Material.POTION && (this.itemMeta as PotionMeta).basePotionData.type == PotionType.WATER
            }.foodProperty {
                this.nutrition = 6
                this.saturationModifier = 0.2f
            }
            listOf(
                Material.MELON_SLICE,
                Material.HONEY_BOTTLE,
                Material.BEETROOT_SOUP,
                Material.MUSHROOM_STEW,
                Material.SUSPICIOUS_STEW
            ) += dslMutator<FoodPropertyBuilder> {
                this.nutrition = (this.nutrition * 1.5).toInt()
                this.saturationModifier *= 1.15f
            }
        }

        abilities {
            newBuilder<Leap>()
                .keybinding(KeyBinding.SINGLE_SNEAK)
                .cooldown(Duration.ZERO)
                .parameter(Leap::chargeTime, 1.5.seconds)
                .build()

            withPassive<WaterHealthBonus>()
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageByEntityEvent.handle() {
        with(this.damager as? LivingEntity ?: return) {
            if (this.equipment == null) return
            if (this.equipment!!.itemInMainHand.type.isSword) {
                this@handle.damage *= 1.5
            }
        }
    }
}
