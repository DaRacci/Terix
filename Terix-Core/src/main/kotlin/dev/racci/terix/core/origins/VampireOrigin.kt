package dev.racci.terix.core.origins

import dev.racci.minix.api.events.keybind.PlayerSneakSecondaryEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.parse
import dev.racci.minix.api.extensions.playSound
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.extensions.reflection.safeCast
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.minix.api.utils.now
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.abilities.Transform
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import kotlinx.datetime.Instant
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import net.kyori.adventure.text.format.TextColor
import net.minecraft.world.damagesource.DamageSource
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityDamageEvent.DamageCause
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.potion.PotionEffectType
import xyz.xenondevs.particle.ParticleBuilder
import xyz.xenondevs.particle.ParticleEffect
import java.awt.Color
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.seconds

// TODO -> Cake good.
public class VampireOrigin(override val plugin: Terix) : Origin() {
    private val cooldowns = mutableMapOf<Player, Instant>()
    private val potionWatch = mutableSetOf<Player>()

    override val name: String = "Vampire"
    override val colour: TextColor = TextColor.fromHexString("#ff1234")!!

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.bat.hurt")
        sounds.deathSound = SoundEffect("entity.bat.death")
        sounds.ambientSound = SoundEffect("entity.bat.ambient")

        food {
            listOf(Material.ROTTEN_FLESH, Material.SPIDER_EYE) += dslMutator<FoodPropertyBuilder> {
                saturationModifier *= 1.75f
                nutrition *= 2
            }
            listOf(MaterialTagsExtension.CARBS, MaterialTagsExtension.FRUITS, MaterialTagsExtension.VEGETABLES) += dslMutator<FoodPropertyBuilder> {
                saturationModifier = 0.3f
                nutrition = 1
            }
            exchangeFoodProperties(Material.COOKED_BEEF, Material.BEEF)
            exchangeFoodProperties(Material.COOKED_CHICKEN, Material.CHICKEN)
            exchangeFoodProperties(Material.COOKED_MUTTON, Material.MUTTON)
            exchangeFoodProperties(Material.COOKED_PORKCHOP, Material.PORKCHOP)
            exchangeFoodProperties(Material.COOKED_RABBIT, Material.RABBIT)
            exchangeFoodProperties(Material.COOKED_SALMON, Material.SALMON)
            exchangeFoodProperties(Material.COOKED_COD, Material.COD)
            exchangeFoodProperties(Material.COOKED_SALMON, Material.SALMON)
        }
        potions {
            listOf(State.TimeState.NIGHT, State.WorldState.NETHER) += dslMutator {
                type = PotionEffectType.NIGHT_VISION
                duration = INFINITE
                amplifier = 0
                ambient = true
            }
        }
        damage {
            State.LightState.SUNLIGHT += 300
        }
        title {
            State.LightState.SUNLIGHT += {
                subtitle = "<red>Return to the dark to regain your strength.".parse()
            }
        }
        potions {
            State.LightState.SUNLIGHT += dslMutator {
                type = PotionEffectType.WEAKNESS
                duration = INFINITE
                ambient = true
            }
            State.LightState.DARKNESS += dslMutator {
                type = PotionEffectType.INCREASE_DAMAGE
                duration = INFINITE
                ambient = true
            }
        }

        item {
            material = Material.BEETROOT_SOUP
            lore = """
                <dark_red>The Vampire is a type of vampire that is able to turn into a bat.
                <dark_red>This is a powerful ability that can be used to kill any player.
            """.trimIndent()
        }

        abilities {
            KeyBinding.DOUBLE_OFFHAND.add<Transform> {
                this.disguise = MobDisguise(DisguiseType.BAT)
            }
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun onDamage(event: EntityDamageEvent) {
        when (event.cause.ordinal) {
            DamageCause.WITHER.ordinal -> event.cancel()
            in 17..18 -> {
                event.cancel()
                OriginHelper.increaseHealth(event.entity.castOrThrow<Player>(), event.damage)
            }
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityRegainHealthEvent.onRegenHealth() {
        when (regainReason.ordinal) {
            in 4..5 -> {
                cancel()
                entity.castOrThrow<Player>().damage(amount)
            }
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityPotionEffectEvent.onPotionEffect() {
        if (action != EntityPotionEffectEvent.Action.ADDED) return
        if (cause != EntityPotionEffectEvent.Cause.FOOD) return

        if (potionWatch.remove(entity.castOrThrow<Player>())) cancel()
    }

    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerItemConsumeEvent.onItemConsume() {
        when (item.type) {
            Material.ROTTEN_FLESH, Material.SPIDER_EYE -> potionWatch += player
            else -> return
        }
    }

    // TODO: Sucking sound
    @OriginEventSelector(EventSelector.PLAYER)
    public fun PlayerSneakSecondaryEvent.onSneakRightClick() {
        val now = now()
        if (player in cooldowns) {
            val inst = cooldowns[player]!!

            if (inst + 1.seconds > now) return
            cooldowns -= player
        }

        val entity = entity as? LivingEntity ?: return

        val amountTaken = (player.maxHealth - player.health).coerceAtMost(2.0)
        val vampAmount = (entity.maxHealth / 8).coerceAtMost(entity.health.coerceAtMost(amountTaken))
        val killing = entity.maxHealth - vampAmount <= 0.0

        player.health += vampAmount
        player.sendHealthUpdate()
        ParticleBuilder(ParticleEffect.FALLING_DUST)
            .setColor(Color.RED)
            .setAmount(6)
            .setLocation(entity.location)
            .display()

        if (killing) {
            val nmsPlayer = player.toNMS()

            entity.killer = player
            nmsPlayer.health = 0.0f

            sync { nmsPlayer.die(DamageSource.playerAttack(player.toNMS())) }
            return
        }

        entity.location.playSound(Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.0f)
        entity.health = (entity.health - vampAmount)
        entity.safeCast<Player>()?.sendHealthUpdate()
        cooldowns[player] = now
    }
}
