package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.MaterialTags
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

class DragonOrigin(override val plugin: Terix) : Origin() {

    override val name = "Dragon"
    override val colour = TextColor.fromHexString("#9e33ff")!!

    override suspend fun hasPermission(player: Player): Boolean {
        return player.getAdvancementProgress(ADVANCEMENT).isDone
    }

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.hoglin.angry")
        sounds.deathSound = SoundEffect("entity.ravager.stunned")
        sounds.ambientSound = SoundEffect("entity.strider.ambient")

        food {
            listOf(MaterialTagsExtension.RAW_MEATS, MaterialTagsExtension.COOKED_MEATS) *= 2
            modifyFood(Material.EMERALD) {
                it.nutrition = 7
                it.saturationModifier = 3.5f
            }
            modifyFood(Material.DIAMOND) {
                it.nutrition = 5
                it.saturationModifier = 3f
            }
            modifyFood(Material.GOLD_INGOT) {
                it.nutrition = 3
                it.saturationModifier = 1.5f
            }
        }

        potions {
            State.CONSTANT += {
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
    }

    override suspend fun onDamageByBlock(event: EntityDamageByBlockEvent) {
        bedExplosion(event)
    }

    override suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) {
        bedDamage(event)
        shieldExplosion(event)
    }

    override suspend fun onBedEnter(event: PlayerBedEnterEvent) {
        event.cancel()
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

    companion object {
        private val ADVANCEMENT = run { Bukkit.getAdvancement(NamespacedKey.minecraft("end/kill_dragon"))!! }
    }
}
