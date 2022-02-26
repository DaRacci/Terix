package dev.racci.terix.api.origins.enums

import org.bukkit.World
import org.bukkit.entity.Player

/**
 * Enums which represent the triggers for
 * a wide range of events within the origins.
 *
 * ## DO NOT CHANGE THE ORDER OF THESE ENUMS.
 */
enum class Trigger {
    /*Active at all times*/
    ON,

    /*Never active*/
    OFF,

    /*Activates at day*/
    DAY,

    /*Activates at night*/
    NIGHT,

    /*Activates within the overworld*/
    OVERWORLD,

    /*Activates within the nether*/
    NETHER,

    /*Activates within the end*/
    THE_END,

    /*Activates within water*/
    WATER,

    /*Activates within lava*/
    LAVA,

    /*Activated within lava or fire*/
    FLAMMABLE,

    /*Activated on fall damage*/
    FALL_DAMAGE,

    /*Activates when in Sunlight*/
    SUNLIGHT,

    /*Activates when in light under*/
    DARKNESS,

    /*Activates when in rain*/
    RAIN,

    /*Activates when in rain or water*/
    WET;

    val ordinalByte: Byte get() = ordinal.toByte()

    companion object {

        val values: Array<out String> by lazy { values().map { trigger -> trigger.name.lowercase().replaceFirstChar { char -> char.titlecase() } }.toTypedArray() }

        fun fromOrdinal(ordinal: Int): Trigger =
            values()[ordinal]

        fun fromOrdinal(ordinal: Byte): Trigger =
            fromOrdinal(ordinal.toInt())

        fun World.Environment.getTrigger(): Trigger =
            when (this) {
                World.Environment.NETHER -> NETHER
                World.Environment.THE_END -> THE_END
                else -> OVERWORLD
            }

        fun World.getTimeTrigger(): Trigger? =
            if (environment.ordinal != 0) {
                null
            } else if (isDayTime) DAY else NIGHT

        fun Player.getLiquidTrigger(): Collection<Trigger> {
            val list = mutableListOf<Trigger>()
            if (isInWaterOrBubbleColumn) {
                list.add(WET)
                list.add(WATER)
                if (isInRain) list.add(RAIN)
            } else if (isInLava) {
                list.add(LAVA)
                list.add(FLAMMABLE)
            }
            return list
        }
    }
}
