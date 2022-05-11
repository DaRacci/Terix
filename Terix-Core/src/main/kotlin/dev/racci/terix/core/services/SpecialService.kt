package dev.racci.terix.core.services

import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.formatted
import dev.racci.minix.api.extensions.inEnd
import dev.racci.minix.api.extensions.inNether
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.extensions.isNight
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.enums.Trigger
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.entity.Player

@MappedExtension(Terix::class, "Special Service", [OriginService::class])
class SpecialService(override val plugin: Terix) : Extension<Terix>() {

    val specialStates by lazy {
        persistentListOf(
            Trigger.ON,
            Trigger.OFF,
            Trigger.NIGHT,
            Trigger.NETHER,
            Trigger.THE_END,
            Trigger.OVERWORLD,
        )
    }
    val specialStatesFormatted by lazy { specialStates.map { it.formatted("_", false) }.toTypedArray() }

    fun isValidTrigger(trigger: Trigger) = specialStates.contains(trigger)

    /**
     * Gets the opposite of the given trigger in the form of on or off.
     *
     * @param player The player to get the opposite trigger for.
     * @param trigger The trigger to get the opposite of.
     */
    fun getToggle(
        player: Player,
        trigger: Trigger
    ) = when (trigger) {
        Trigger.ON -> Trigger.OFF
        Trigger.OFF -> Trigger.ON
        Trigger.NIGHT -> if (player.isNight()) Trigger.ON else Trigger.OFF
        Trigger.DAY -> if (player.isDay()) Trigger.ON else Trigger.OFF
        Trigger.DARKNESS -> if (player.location.block.lightLevel > 8) Trigger.ON else Trigger.OFF
        Trigger.NETHER -> if (player.inNether()) Trigger.OFF else Trigger.ON
        Trigger.OVERWORLD -> if (player.inOverworld()) Trigger.OFF else Trigger.ON
        Trigger.THE_END -> if (player.inEnd()) Trigger.OFF else Trigger.ON
        else -> error { "Unknown special trigger: $trigger" }
    }

    companion object : ExtensionCompanion<SpecialService>()
}
