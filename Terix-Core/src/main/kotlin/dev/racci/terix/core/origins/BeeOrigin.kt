package dev.racci.terix.core.origins

import dev.racci.minix.api.events.PlayerRightClickEvent
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.core.services.HookService
import me.angeschossen.lands.api.flags.Flags
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect

class BeeOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Bee"
    override val colour = TextColor.fromHexString("#fc9f2f")!!

    override suspend fun onRegister() {
        sounds.hurtSound = SoundEffect("entity.bee.hurt")
        sounds.deathSound = SoundEffect("entity.bee.death")
        sounds.ambientSound = SoundEffect("entity.bee.ambient")

        item {
            material = Material.HONEYCOMB
            lore = """
                <gold>A bee is a type of bee.
                <gold>It is a type of bee.
            """.trimIndent()
        }
    }

    override suspend fun onRightClick(event: PlayerRightClickEvent) {
        val flower = event.player.inventory.itemInMainHand.takeIf { it.type.isFlower } ?: run {
            val area = HookService.getService().get<HookService.LandsHook>()?.integration?.getAreaByLoc(event.player.location)
            if (area == null || area.hasFlag(event.player.uniqueId, Flags.BLOCK_BREAK)) return@run null
            event.blockData?.block?.takeIf { it.type.isFlower }
        } ?: return

//        val effect = TODO("Unique effects for each flower type")
//        TODO("Apply effect")

        if (flower is ItemStack) {
            flower.amount--
            return
        }

        flower.unsafeCast<Block>().type = Material.AIR
    }

    // TODO: Possible groups
    @Suppress("kotlin:S1151")
    private val Material.isFlower get() = when (this) {
        Material.DANDELION,
        Material.POPPY,
        Material.BLUE_ORCHID,
        Material.ALLIUM,
        Material.AZURE_BLUET,
        Material.RED_TULIP,
        Material.ORANGE_TULIP,
        Material.WHITE_TULIP,
        Material.PINK_TULIP,
        Material.OXEYE_DAISY,
        Material.CORNFLOWER,
        Material.LILY_OF_THE_VALLEY,
        Material.WITHER_ROSE,
        Material.LILAC,
        Material.ROSE_BUSH,
        Material.PEONY -> true
        else -> false
    }

    private fun Material.getEffect(): FlowerEffect? {
        if (!this.isFlower) return null

        return when (this) {
            Material.DANDELION -> TODO("Dandelion effect")
            Material.POPPY -> TODO("Poppy effect")
            Material.BLUE_ORCHID -> TODO("Blue Orchid effect")
            Material.ALLIUM -> TODO("Allium effect")
            Material.AZURE_BLUET -> TODO("Azure Bluet effect")
            Material.RED_TULIP -> TODO("Red Tulip effect")
            Material.ORANGE_TULIP -> TODO("Orange Tulip effect")
            Material.WHITE_TULIP -> TODO("White Tulip effect")
            Material.PINK_TULIP -> TODO("Pink Tulip effect")
            Material.OXEYE_DAISY -> TODO("Oxeye Daisy effect")
            Material.CORNFLOWER -> TODO("Cornflower effect")
            Material.LILY_OF_THE_VALLEY -> TODO("Lily Of The Valley effect")
            Material.WITHER_ROSE -> TODO("Wither Rose effect")
            Material.LILAC -> TODO("Lilac effect")
            Material.ROSE_BUSH -> TODO("Rose Bush effect")
            Material.PEONY -> TODO("Peony effect")
            else -> null
        }
    }

    private class FlowerEffect {
        var potionEffect: PotionEffect? = null
        var attribute: TimedAttributeBuilder? = null
        var title: TitleBuilder? = null

        fun apply(player: Player) {
            attribute?.invoke(player)
            potionEffect?.apply(player)
            title?.invoke(player)
        }
    }
}
