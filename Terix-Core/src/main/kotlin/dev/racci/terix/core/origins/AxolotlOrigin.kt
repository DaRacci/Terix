package dev.racci.terix.core.origins

import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent

class AxolotlOrigin(override val plugin: Terix) : Origin() {

    override val name = "Axolotl"
    override val colour = TextColor.fromHexString("#ff6ea8")!!

    override var waterBreathing = true

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.axolotl.hurt")
        sounds.deathSound = SoundEffect("entity.axolotl.death")
        sounds.ambientSound = SoundEffect("entity.axolotl.ambient")

        food {
            listOf(
                Material.SALMON,
                Material.COD,
                Material.PUFFERFISH,
                Material.TROPICAL_FISH,
                Material.KELP
            ) *= 2.0
        }

        item {
            material = Material.AXOLOTL_BUCKET
            lore = "<white>Little shit."
        }
    }

    override suspend fun onDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? LivingEntity ?: return
        if (damager.equipment?.itemInMainHand?.type != Material.FISHING_ROD) return
        event.damage += 5
    }
}
