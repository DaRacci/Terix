package dev.racci.terix.core.origins

import dev.racci.minix.api.collections.PlayerMap
import dev.racci.minix.api.events.player.PlayerMoveFullXYZEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.collections.clear
import dev.racci.minix.api.extensions.collections.computeAndRemove
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.playSound
import dev.racci.minix.api.extensions.toItemStack
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.abilities.keybind.Levitate
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.origin.cooldown
import dev.racci.terix.api.origins.origin.keybinding
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import dev.racci.terix.core.extensions.fromOrigin
import dev.racci.terix.core.origins.abilities.keybind.TargetOrSelf
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.sync.Mutex
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import ru.beykerykt.minecraft.lightapi.common.LightAPI
import ru.beykerykt.minecraft.lightapi.common.api.engine.EditPolicy
import ru.beykerykt.minecraft.lightapi.common.api.engine.LightFlag
import ru.beykerykt.minecraft.lightapi.common.api.engine.SendPolicy
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public class AethenOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Aethen"
    override val colour: TextColor = TextColor.fromHexString("#ffc757")!!

    private val lightMutex = Mutex(false)
    private val playerLocations = ConcurrentHashMap<Player, LightLocation>()
    private val missingPotion = PlayerMap<PotionEffect>()

    override val requirements: PersistentList<Pair<TextComponent, (Player) -> Boolean>> = persistentListOf(
        Component.text("Slay Lycer. (Currently Unimplemented)") to { _: Player -> true }
    )

    override suspend fun handleRegister() {
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

            newBuilder<TargetOrSelf>()
                .parameter(TargetOrSelf::lambda) { target -> sync { target.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 3, true)) } }
                .parameter(TargetOrSelf::searchDistance, 9)
                .keybinding(KeyBinding.DOUBLE_RIGHT_CLICK)
                .cooldown(45.seconds)
                .build()
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

                    sync {
                        player.location.clone().apply {
                            yaw = player.location.yaw - 180
                            pitch = 45f
                            add(0.0, -0.5, 0.0)
                        }.dropItem(item.toItemStack())
                    }

                    player.location.playSound(Sound.BLOCK_POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, 1f, 0.8f)
                }
            }
        }
    }

    override suspend fun handleUnload() {
        missingPotion.clear()
        playerLocations.clear { _, location -> resetLightLevel(location) } // Ensures that we don't have ghost lights
    }

    override suspend fun handleDeactivate(player: Player) {
        playerLocations.computeAndRemove(player) { resetLightLevel(this) }
    }

    override suspend fun handleChangeOrigin(player: Player) {
        playerLocations.computeAndRemove(player) { resetLightLevel(this) }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerQuitEvent.handle() {
        playerLocations.computeAndRemove(player) { resetLightLevel(this) }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerBedEnterEvent.handle() {
        if (this.bed.location.y > 91) return

        this.cancel()
        this.player.sendActionBar("<red>You need fresh air to sleep!".parse())
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerToggleSneakEvent.handle() {
        if (this.isSneaking) {
            missingPotion[this.player] = this.player.activePotionEffects.first { it.type == PotionEffectType.SLOW_FALLING && it.fromOrigin() }
            player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            return
        }

        missingPotion.computeAndRemove(player, player::addPotionEffect)
    }

    private val handler = LightAPI.get()

    @OriginEventSelector(EventSelector.PLAYER)
    public suspend fun PlayerMoveFullXYZEvent.handle() {
        if (OriginHelper.shouldIgnorePlayer(this.player)) return

        if (handler == null) {
            plugin.log.warn { "LightAPI is not installed, disabling Aethen's ability" }
            return
        }

        lightMutex.lock()

        sync {
            playerLocations.compute(player) { _, oldLocation ->
                val lightLocation = LightLocation(player.location)
                val (newX, newY, newZ, newWorld) = lightLocation

                if (oldLocation != null) resetLightLevel(oldLocation)

                handler.setLightLevel(
                    newWorld,
                    newX,
                    newY,
                    newZ,
                    EMISSION_LEVEL,
                    LightFlag.BLOCK_LIGHTING,
                    EditPolicy.DEFERRED,
                    SendPolicy.DEFERRED,
                    null
                )

                lightLocation
            }
        }

        lightMutex.unlock()
    }

    private fun resetLightLevel(location: LightLocation) {
        handler.setLightLevel(
            location.world,
            location.x,
            location.y,
            location.z,
            0,
            LightFlag.BLOCK_LIGHTING,
            EditPolicy.DEFERRED,
            SendPolicy.DEFERRED,
            null
        )
    }

    private data class LightLocation(
        val x: Int,
        val y: Int,
        val z: Int,
        val world: String
    ) {
        constructor(location: Location) : this(location.blockX, location.blockY, location.blockZ, location.world!!.name)
    }

    private companion object {
        const val EMISSION_LEVEL: Int = 9
    }
}
