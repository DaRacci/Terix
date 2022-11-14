package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.entity.LivingEntity
import org.bukkit.event.entity.EntityDamageByEntityEvent

public class AxolotlOrigin(override val plugin: Terix) : Origin() {

    override val name: String = "Axolotl"
    override val colour: TextColor = TextColor.fromHexString("#ff6ea8")!!

    override var waterBreathing: Boolean = true

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.axolotl.hurt")
        sounds.deathSound = SoundEffect("entity.axolotl.death")
        sounds.ambientSound = SoundEffect("entity.axolotl.ambient")

        food {
            listOf(
                *MaterialTags.RAW_FISH.values.toTypedArray(),
                Material.KELP
            ) *= 2.0
        }

        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.65
        }

        item {
            material = Material.AXOLOTL_BUCKET
            lore = "<white>Little shit."
        }
    }

    @OriginEventSelector(EventSelector.ENTITY)
    public fun EntityDamageByEntityEvent.handle() {
        val damager = damager as? LivingEntity ?: return
        if (damager.equipment?.itemInMainHand?.type != Material.FISHING_ROD) return
        damage += 5
    }
}
