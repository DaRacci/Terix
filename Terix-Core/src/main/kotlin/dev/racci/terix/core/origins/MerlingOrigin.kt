package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.utils.minecraft.MaterialTagsExtension
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityAirChangeEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import kotlin.time.Duration

// TODO -> Raw fish, kelp and glow berries.
// TODO -> Make cake soggy.
// TODO -> Nothing in the ocean targets the merling (minus elder guardian).
// TODO -> More fire / lava damage.
class MerlingOrigin(override val plugin: Terix) : Origin() {

    override val name = "Merling"
    override val colour = TextColor.fromHexString("#47d7ff")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.salmon.hurt")
        sounds.deathSound = SoundEffect("entity.salmon.death")
        sounds.ambientSound = SoundEffect("entity.salmon.ambient")

        attributes {
            Pair(State.LiquidState.WATER, Attribute.GENERIC_MOVEMENT_SPEED) *= 1.2
            Pair(State.LiquidState.LAND, Attribute.GENERIC_MOVEMENT_SPEED) *= 0.9
            Pair(State.LiquidState.WATER, Attribute.GENERIC_ATTACK_DAMAGE) *= 1.2
            Pair(State.LiquidState.WATER, Attribute.GENERIC_KNOCKBACK_RESISTANCE) *= 0.80
            Pair(State.LiquidState.WATER, Attribute.GENERIC_ATTACK_SPEED) *= 5.0
            Pair(State.LiquidState.LAND, Attribute.GENERIC_ATTACK_SPEED) *= 0.80
        }

        potions {
            State.LiquidState.WATER += {
                type = PotionEffectType.NIGHT_VISION
                duration = Duration.INFINITE
                amplifier = 0
                ambient = true
            }
        }

        food {
            MaterialTags.RAW_FISH *= 4.0
            MaterialTagsExtension.COOKED_MEATS *= 0.5
        }

        item {
            material = Material.TRIDENT
            lore = """
                <aqua>A mysterious origin.
                <aqua>It's not clear what it is.
            """.trimIndent()
        }
    }

    override suspend fun onBecomeOrigin(event: PlayerOriginChangeEvent) {
        event.player.isReverseOxygen = true
    }

    override suspend fun onChangeOrigin(event: PlayerOriginChangeEvent) {
        event.player.isReverseOxygen = false
    }

    override suspend fun onRiptide(event: PlayerRiptideEvent) {
        event.player.velocity = event.player.velocity.multiply(1.5)
    }

    override suspend fun onItemConsume(event: PlayerItemConsumeEvent) {
        if ((event.item.itemMeta as? PotionMeta)?.basePotionData?.type != PotionType.WATER) return

        event.player.remainingAir = (event.player.remainingAir + 2).coerceAtMost(event.player.maximumAir)
    }

    // TODO -> Move into tentacles then remove here
    override suspend fun onAirChange(event: EntityAirChangeEvent) {
        val player = event.entity as? Player ?: return

        if (player.hasPotionEffect(PotionEffectType.WATER_BREATHING)) return event.cancel()

        val helmet = player.inventory.helmet ?: return
        if (helmet.type == Material.TURTLE_HELMET || helmet.enchantments.containsKey(Enchantment.OXYGEN)) return event.cancel()
    }

    // TODO -> Fix inside tentacles then remove here
//    override suspend fun onInteract(event: PlayerInteractEvent) {
//        val player = event.player.toNMS()
//
//        if (event.action == Action.LEFT_CLICK_BLOCK) {
//            val packet = ClientboundBlockDestructionPacket(
//                event.player.entityId,
//                BlockPos(
//                    event.clickedBlock!!.x,
//                    event.clickedBlock!!.y,
//                    event.clickedBlock!!.z
//                ),
//                3
//            )
//            try {
//                val field: Field = packet.getClass()
//                    .getDeclaredField("a")
//                field.setAccessible(true) // allows us to access the field
//                field.setInt(packet, 123) // sets the field to an integer
//                field.setAccessible(!field.isAccessible()) // we want to stop accessing this now
//            } catch (x: Exception) {
//                x.printStackTrace()
//            }
//            p.getHandle().playerConnection.sendPacket(packet)
//        }
//    }
}
