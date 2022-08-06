package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialSetTag
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration

// TODO: Explosion when shield is broken

class DragonOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Dragon"
    override val colour = TextColor.fromHexString("#9e33ff")!!

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
            Trigger.ON += {
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
}
