package dev.racci.terix.core.origins

import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.destructors.component1
import dev.racci.minix.api.destructors.component2
import dev.racci.minix.api.destructors.component3
import dev.racci.minix.api.destructors.component4
import dev.racci.minix.api.events.PlayerDoubleRightClickEvent
import dev.racci.minix.api.events.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.playSound
import dev.racci.minix.api.extensions.sync
import dev.racci.minix.api.extensions.toItemStack
import dev.racci.minix.api.utils.collections.CollectionUtils.computeAndRemove
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.abilities.Levitate
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.fromOrigin
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
import ru.beykerykt.minecraft.lightapi.common.LightAPI
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class AethenOrigin(override val plugin: Terix) : Origin() {

    override val name = "Aethen"
    override val colour = TextColor.fromHexString("#ffc757")!!

    private val playerLocations = PlayerMap<Location>()
    private val missingPotion = PlayerMap<PotionEffect>()
    private val regenPotion = PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true)
    private val regenCache = mutableMapOf<UUID, Instant>()
    private val regenCooldown = 3.minutes

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
            State.CONSTANT += dslMutator {
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

    override suspend fun handleChangeOrigin(event: PlayerOriginChangeEvent) {
        playerLocations.computeAndRemove(event.player) { resetLightLevel(this) }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    fun PlayerBedEnterEvent.handle() {
        if (this.bed.location.y > 91) return

        this.cancel()
        this.player.sendActionBar("<red>You need fresh air to sleep!".parse())
    }

    override suspend fun onToggleSneak(event: PlayerToggleSneakEvent) {
        if (event.isSneaking) {
            missingPotion[event.player] = event.player.activePotionEffects.first { it.type == PotionEffectType.SLOW_FALLING && it.fromOrigin() }
            event.player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            return
        }

        missingPotion.computeAndRemove(event.player, event.player::addPotionEffect)
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

    private val handler = LightAPI.get()

    @RunAsync
    @OriginEventSelector(EventSelector.PLAYER)
    fun PlayerMoveFullXYZEvent.handle() {
        if (handler == null) {
            plugin.log.warn { "LightAPI is not installed, disabling Aethen's ability" }
            return
        }

        playerLocations.compute(this.player) { _, oldLocation ->
            val (newX, newY, newZ, newWorld) = this.player.location

            if (oldLocation != null) resetLightLevel(oldLocation)

            handler.setLightLevel(
                newWorld!!.name,
                newX.toInt(),
                newY.toInt(),
                newZ.toInt(),
                9,
                LightFlag.BLOCK_LIGHTING,
                EditPolicy.DEFERRED,
                SendPolicy.DEFERRED,
                null
            )

            this.player.location
        }
    }

    private fun resetLightLevel(location: Location) {
        handler.setLightLevel(
            location.world!!.name,
            location.x.toInt(),
            location.y.toInt(),
            location.z.toInt(),
            0,
            LightFlag.BLOCK_LIGHTING,
            EditPolicy.DEFERRED,
            SendPolicy.DEFERRED,
            null
        )
    }
}
