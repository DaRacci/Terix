package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.tentacles.Tentacles
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.services.ListenerService
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import net.minecraft.tags.FluidTags
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityTargetEvent.TargetReason
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import kotlin.time.Duration

// TODO -> Make cake soggy.
// TODO -> Mine faster underwater
// TODO -> Repulsion effect when in water
public class MerlingOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Merling"
    override val colour: TextColor = TextColor.fromHexString("#47d7ff")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.salmon.hurt")
        sounds.deathSound = SoundEffect("entity.salmon.death")
        sounds.ambientSound = SoundEffect("entity.salmon.ambient")

        attributes {
            State.LiquidState.LAND to Attribute.GENERIC_MOVEMENT_SPEED *= 0.75
            State.LiquidState.WATER to Attribute.GENERIC_ATTACK_DAMAGE *= 1.2
            State.LiquidState.WATER to Attribute.GENERIC_KNOCKBACK_RESISTANCE *= 0.80
            State.LiquidState.WATER to Attribute.GENERIC_ATTACK_SPEED *= 5.0
            State.LiquidState.LAND to Attribute.GENERIC_ATTACK_SPEED *= 0.80
        }

        damage {
            listOf(
                DamageCause.LAVA,
                DamageCause.FIRE,
                DamageCause.MELTING,
                DamageCause.FIRE_TICK,
                DamageCause.HOT_FLOOR
            ) *= 2.0
        }

        potions {
            State.LiquidState.WATER += dslMutator {
                type = PotionEffectType.NIGHT_VISION
                duration = Duration.INFINITE
                amplifier = -1
                ambient = true
            }
            State.LiquidState.WATER += dslMutator {
                type = PotionEffectType.DOLPHINS_GRACE
                duration = Duration.INFINITE
                amplifier = -1
                ambient = true
            }
        }

        food {
            listOf(*MaterialTags.RAW_FISH.values.toTypedArray(), Material.DRIED_KELP, Material.GLOW_BERRIES) += dslMutator<FoodPropertyBuilder> {
                nutrition *= 3
                saturationModifier = 0.6f
            }
            MaterialTagsExtension.COOKED_MEATS.values + MaterialTags.COOKED_FISH.values += dslMutator<FoodPropertyBuilder> {
                nutrition /= 2
                saturationModifier = 0.2f
            }
        }

        item {
            material = Material.TRIDENT
            lore = """
                <aqua>A mysterious origin.
                <aqua>It's not clear what it is.
            """.trimIndent()
        }

        Tentacles.addGlobalMiningModifier(NamespacedKey(plugin, "merling_mining")) { player, _ ->
            if (!player.toNMS().isEyeInFluid(FluidTags.WATER) || TerixPlayer.cachedOrigin(player) !== this@MerlingOrigin) return@addGlobalMiningModifier null

            if (player.isOnGround) {
                5F
            } else 25F
        }
    }

    override suspend fun handleBecomeOrigin(event: PlayerOriginChangeEvent) {
        event.player.isReverseOxygen = true
    }

    override suspend fun handleChangeOrigin(event: PlayerOriginChangeEvent) {
        event.player.isReverseOxygen = false
    }

    @OriginEventSelector(EventSelector.TARGET)
    public fun EntityTargetLivingEntityEvent.handle() {
        if (entity.type !in MARINE_FRIENDS) return
        if (reason !in TARGET_REASONS) return
        cancel()
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageByEntityEvent.handle() {
        if (damager is Projectile) {
            val projectile = damager as Projectile
            plugin.log.debug { "projectile fire: ${projectile.fireTicks}" }
            if (projectile.fireTicks > 0) damage *= 2.0
            val bow = ListenerService.getService().bowTracker[projectile.shooter] ?: return
            return
        }

        val damager = damager as? LivingEntity ?: return
        val weapon = damager.equipment?.itemInMainHand ?: return

        if (weapon.enchantments.contains(Enchantment.FIRE_ASPECT)) damage *= 2.0
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerRiptideEvent.handle() {
        player.velocity = player.velocity.multiply(3.0)
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerItemConsumeEvent.handle() {
        if ((item.itemMeta as? PotionMeta)?.basePotionData?.type != PotionType.WATER) return

        player.remainingAir = (player.remainingAir + 20).coerceAtMost(player.maximumAir)
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun onAirChange(event: EntityAirChangeEvent) {
        val player = event.entity as? Player ?: return

        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) return event.cancel()

        val helmet = player.inventory.helmet ?: return
        if (helmet.type == Material.TURTLE_HELMET || helmet.enchantments.containsKey(Enchantment.OXYGEN)) return event.cancel()
    }

    private companion object {
        val MARINE_FRIENDS = arrayOf(EntityType.DROWNED, EntityType.GUARDIAN, EntityType.PUFFERFISH)
        val TARGET_REASONS = arrayOf(TargetReason.CLOSEST_ENTITY, TargetReason.CLOSEST_PLAYER, TargetReason.COLLISION, TargetReason.RANDOM_TARGET)
    }
}
