package dev.racci.terix.core.origins

import dev.racci.minix.api.annotations.RunAsync
import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.reflection.safeCast
import dev.racci.minix.api.utils.adventure.PartialComponent.Companion.message
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.data.Lang
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityPotionEffectEvent.Cause
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.get
import kotlin.time.Duration.Companion.seconds

// TODO -> All normal food gives half a heart of damage, they can only eat flowers.
// TODO -> Some flowers are edible, some flowers are potion effects.
public class BeeOrigin(override val plugin: Terix) : Origin() {
    private val stingerInstant = PlayerMap<Instant>()
    private val lowerFood = mutableSetOf<Player>()

    override val name: String = "Bee"
    override val colour: TextColor = TextColor.fromHexString("#fc9f2f")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.bee.hurt")
        sounds.deathSound = SoundEffect("entity.bee.death")
        sounds.ambientSound = SoundEffect("entity.bee.ambient")

        item {
            material = Material.HONEYCOMB
            lore = """
                <gold>A bee is a type of bee.
                <gold>It is a type of bee.
            """.trimIndent()
        }

        food {
            listOf(
                Material.OXEYE_DAISY,
                Material.PINK_TULIP,
                Material.WHITE_TULIP,
                Material.ORANGE_TULIP,
                Material.RED_TULIP,
                Material.ALLIUM,
                Material.BLUE_ORCHID,
                Material.AZURE_BLUET,
                Material.CORNFLOWER,
                Material.LILY_OF_THE_VALLEY
            ) + 3.0

            listOf(
                Material.POPPY,
                Material.DANDELION
            ) + 2.0

            listOf(
                Material.PEONY,
                Material.LILAC
            ) + 4.0

            Material.DEAD_BUSH + 1.0

            Material.ORANGE_TULIP += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.FIRE_RESISTANCE
            }

            Material.RED_TULIP += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.INCREASE_DAMAGE
            }

            Material.BLUE_ORCHID += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.WATER_BREATHING
            }

            Material.DEAD_BUSH += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.HUNGER
            }

            Material.ROSE_BUSH += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.HUNGER
                amplifier = 3
            } // TODO -> Half heard of damage, 4 hunger and instant health??

            Material.ROSE_BUSH += { player: Player -> player.damage(0.5) }

            Material.ROSE_BUSH += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.HEAL
                amplifier = 2
            }

            Material.CORNFLOWER += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.SPEED
            }

            Material.LILY_OF_THE_VALLEY += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.INVISIBILITY
            }

            Material.LILAC += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.ABSORPTION
            }

            Material.PEONY += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.FAST_DIGGING
            }

            Material.WITHER_ROSE += { player: Player ->
                player.health = 0.0
            }

            Material.SUNFLOWER += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.NIGHT_VISION
            }

            Material.SUNFLOWER += dslMutator<PotionEffectBuilder> {
                type = PotionEffectType.HUNGER
                amplifier = 2
            }
        }
    }

    @RunAsync
    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageByEntityEvent.handle() {
        val attacker = damager.safeCast<Player>() ?: return
        stingerAttack(damager.castOrThrow(), attacker)
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityPotionEffectEvent.handle() {
        if (action != EntityPotionEffectEvent.Action.ADDED) return
        if (cause in BANNED_POTION_CAUSES) cancel()
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun FoodLevelChangeEvent.handle() {
        if (lowerFood.remove(entity.castOrThrow())) cancel()
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerItemConsumeEvent.handle() {
        if (antiPotion(this)) return
        lowerFood.add(player)
    }

    private fun stingerAttack(
        attacker: Player,
        victim: LivingEntity
    ) {
        if (attacker.inventory.itemInMainHand.type != Material.AIR) return

        val now = now()
        val current = stingerInstant.compute(attacker) { _, value ->
            if (value != null && value + STINGER_COOLDOWN >= now) value else now
        }

        if (now === current) return

        sync {
            attacker.damage(0.5)
            victim.damage(0.5)
            victim.addPotionEffect(PotionEffect(PotionEffectType.POISON, 60, 0))
        }
    }

    private fun antiPotion(event: PlayerItemConsumeEvent): Boolean {
        val potion = event.item
        if (potion.itemMeta is PotionMeta) {
            event.cancel()
            get<Lang>().origin.bee["potion"] message event.player
            return true
        }

        return false
    }

    private companion object {
        val STINGER_COOLDOWN = 10.seconds
        val BANNED_POTION_CAUSES = arrayOf(
            Cause.AREA_EFFECT_CLOUD,
            Cause.BEACON,
            Cause.CONDUIT,
            Cause.POTION_DRINK,
            Cause.POTION_SPLASH
        )
    }
}
