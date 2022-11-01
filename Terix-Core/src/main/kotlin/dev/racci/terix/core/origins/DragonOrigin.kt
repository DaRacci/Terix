package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.MaterialTags
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.pdc
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.FoodPropertyBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.abilities.DragonBreath
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import kotlinx.collections.immutable.persistentListOf
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry.ADVANCEMENT
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

// TODO -> Dragons breaths, don't damage the dragon, Not collectable in bottles
public class DragonOrigin(override val plugin: Terix) : Origin() {

    override val name = "Dragon"
    override val colour = TextColor.fromHexString("#9e33ff")!!

    override val requirements = persistentListOf(
        Component.text("Slay the Ender Dragon.") to { player: Player -> player.getAdvancementProgress(ADVANCEMENT).isDone },
        Component.text("Cradle Jean's egg") to { player: Player -> player.pdc.getOrDefault(CRADLE_KEY, PersistentDataType.BYTE, 0) == 1.toByte() },
        Component.text("Kill 5 players in Overworld, Nether and End") to { player: Player -> arrayOf(KILL_OVERWORLD_KEY, KILL_NETHER_KEY, KILL_END_KEY).all { key -> player.pdc.getOrDefault(key, PersistentDataType.INTEGER, 0) >= KILL_TOTAL } }

    )

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.hoglin.angry")
        sounds.deathSound = SoundEffect("entity.ravager.stunned")
        sounds.ambientSound = SoundEffect("entity.strider.ambient")

        food {
            listOf(MaterialTagsExtension.RAW_MEATS, MaterialTagsExtension.COOKED_MEATS) *= 2
            Material.EMERALD += dslMutator<FoodPropertyBuilder> {
                nutrition = 7
                saturationModifier = 3.5f
            }
            Material.DIAMOND += dslMutator<FoodPropertyBuilder> {
                nutrition = 5
                saturationModifier = 3f
            }
            Material.GOLD_INGOT += dslMutator<FoodPropertyBuilder> {
                nutrition = 3
                saturationModifier = 1.5f
            }
        }

        potions {
            State.CONSTANT += dslMutator {
                type = PotionEffectType.HUNGER
                duration = Duration.INFINITE
                amplifier = 1
                ambient = true
            }
        }

        item {
            material = Material.LARGE_AMETHYST_BUD
            lore = "<light_purple>A breath of fire that can be used to summon a dragon."
        }

        abilities {
            KeyBinding.SNEAK_RIGHT_CLICK.add<DragonBreath>()
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    fun EntityDamageByBlockEvent.handle() {
        bedExplosion(this)
    }

    @OriginEventSelector(EventSelector.ENTITY)
    fun EntityDamageByEntityEvent.handle() {
        bedDamage(this)
        shieldExplosion(this)
    }

    @OriginEventSelector(EventSelector.PLAYER)
    fun PlayerBedEnterEvent.handle() {
        cancel()
    }

    private fun bedExplosion(event: EntityDamageByBlockEvent) {
        if (event.cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return
        if (!MaterialSetTag.BEDS.isTagged(event.damager?.type ?: return)) return

        event.damage *= 2
    }

    private fun bedDamage(event: EntityDamageByEntityEvent) {
        val attacker = event.entity as? LivingEntity ?: return
        val item = attacker.equipment?.getItem(EquipmentSlot.HAND) ?: return
        if (!MaterialSetTag.BEDS.isTagged(item.type)) return

        event.damage *= 2
    }

    private fun shieldExplosion(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? LivingEntity ?: return
        val victim = event.entity as Player

        if (!victim.isBlocking || !MaterialTags.AXES.isTagged(attacker.equipment?.itemInMainHand?.type ?: return) || victim.shieldBlockingDelay <= 0) return

        victim.location.createExplosion(victim, 3.5f, false, false)
    }

    public companion object {
        private const val KILL_TOTAL: Int = 5
        private val ADVANCEMENT by lazy { Bukkit.getAdvancement(NamespacedKey.minecraft("end/kill_dragon"))!! }
        internal val CRADLE_KEY by lazy { NamespacedKey("terix", "dragon/cradle_egg") }
        internal val KILL_OVERWORLD_KEY by lazy { NamespacedKey("terix", "dragon/player_kills_overworld") }
        internal val KILL_NETHER_KEY by lazy { NamespacedKey("terix", "dragon/player_kills_nether") }
        internal val KILL_END_KEY by lazy { NamespacedKey("terix", "dragon/player_kills_end") }
    }
}
