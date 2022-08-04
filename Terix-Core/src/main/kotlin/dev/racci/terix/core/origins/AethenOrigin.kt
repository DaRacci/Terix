package dev.racci.terix.core.origins

import dev.racci.minix.api.events.PlayerDoubleRightClickEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.playSound
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.extensions.toItemStack
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.abilities.Levitate
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

// TODO -> Aethens emit a constant light level while moving. Waiting until LightAPI is updated to 1.19 to implement this.
class AethenOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Aethen"
    override val colour = TextColor.fromHexString("#ffc757")!!

    private val regenPotion = PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true)
    private val regenCache = mutableMapOf<UUID, Instant>()
    private val regenCooldown = 3.minutes
    private val lastLocation = mutableMapOf<UUID, Location>()

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.silverfish.hurt")
        sounds.deathSound = SoundEffect("entity.turtle.death_baby")
        sounds.ambientSound = SoundEffect("entity.vex.ambient")

        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.80
        }
        damage {
            EntityDamageEvent.DamageCause.FALL *= 0.5
            listOf(
                EntityDamageEvent.DamageCause.LAVA,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.LIGHTNING
            ) *= 2.0
        }
        potions {
            Trigger.ON += {
                type = PotionEffectType.SLOW_FALLING
                duration = Duration.INFINITE
                amplifier = 0
                ambient = true
            }
        }
        item {
            material = Material.FEATHER
            lore = "<yellow>Halal mode enabled"
        }
        abilities {
            KeyBinding.DOUBLE_OFFHAND.add<Levitate>()
        }
        food {
            listOf<Material>(
                Material.GLOW_BERRIES,
                *MaterialTagsExtension.CARBS.values.toTypedArray(),
                *MaterialTagsExtension.FRUITS.values.toTypedArray(),
                *MaterialTagsExtension.VEGETABLES.values.toTypedArray()
            ) *= 2
            listOf(
                *MaterialTagsExtension.RAW_MEATS.values.toTypedArray(),
                *MaterialTagsExtension.COOKED_MEATS.values.toTypedArray()
            ).apply {
                this *= 0.15
                this += { player: Player ->
                    val item = if (player.toNMS().random.nextBoolean()) {
                        Material.BROWN_DYE
                    } else Material.COCOA_BEANS

                    player.location.clone().apply {
                        yaw = player.location.yaw - 180
                        pitch = 45f
                        add(0.0, -0.5, 0.0)
                    }.dropItem(item.toItemStack())

                    player.location.playSound(Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, 1f, 0.8f)
                }
            }
        }
    }

    override suspend fun onBedEnter(event: PlayerBedEnterEvent) {
        if (event.bed.location.y > 91) return
        event.cancel()
        event.player.sendActionBar("<red>You need fresh air to sleep!".parse())
    }

    override suspend fun onToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            event.player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            return
        }

        if (!OriginHelper.getAllPotions(event.player, Trigger.ON).contains(PotionEffectType.SLOW_FALLING)) {
            event.player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 0))
        }
    }

    override suspend fun onDoubleRightClick(event: PlayerDoubleRightClickEvent) {
        val lastTime = regenCache[event.player.uniqueId]
        val now = now()

        if (lastTime != null && lastTime.plus(regenCooldown) > now) {
            event.player.sendActionBar("<red>You must wait ${((lastTime + regenCooldown) - now).inWholeSeconds} seconds before using this again!".parse())
            return
        }

        val target = event.player.getTargetEntity(10) as? LivingEntity ?: run {
            return@run if (event.player.getTargetBlock(3) != null) {
                event.player
            } else null
        }

        if (target == null || target.isDead) return
        regenCache[event.player.uniqueId] = now
        sync { target.addPotionEffect(regenPotion) }
    }

    /* // Possibly move to on move?
    override suspend fun onTick(player: Player) {
        val last = lastLocation[player.uniqueId]
        val current = player.location

        lastLocation[player.uniqueId] = current

        if (last == null || last.block != current.block) {
            if (last?.block != null) {
                last.block.type
            }
        }
    } */
}
