package dev.racci.terix.core.extensions

import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.LiquidType.Companion.liquidType
import dev.racci.minix.api.extensions.inEnd
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.extensions.isNight
import dev.racci.terix.api.origins.enums.Trigger
import org.bukkit.entity.Player

fun Trigger.fulfilled(player: Player): Boolean {
    return when (this) {
        Trigger.ON -> true
        Trigger.OFF -> false
        Trigger.DAY -> player.isDay
        Trigger.NIGHT -> player.isNight
        Trigger.OVERWORLD -> player.inOverworld
        Trigger.NETHER -> player.isNight
        Trigger.THE_END -> player.inEnd
        Trigger.WATER -> player.location.block.liquidType == LiquidType.WATER
        Trigger.LAVA -> player.location.block.liquidType == LiquidType.LAVA
        Trigger.LAND -> player.location.block.liquidType == LiquidType.NON
        Trigger.FLAMMABLE -> player.fireTicks > 0
        Trigger.FALL_DAMAGE -> false
        Trigger.SUNLIGHT -> player.canSeeSky()
        Trigger.DARKNESS -> player.inDarkness()
        Trigger.RAIN -> player.isInRain
        Trigger.WET -> player.isInWaterOrRainOrBubbleColumn || player.location.block.isLiquid
    }
}
