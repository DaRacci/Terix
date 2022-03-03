package dev.racci.terix.core.services

import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.inEnd
import dev.racci.minix.api.extensions.inNether
import dev.racci.minix.api.extensions.inOverworld
import dev.racci.minix.api.extensions.isDay
import dev.racci.minix.api.extensions.isNight
import dev.racci.minix.api.utils.kotlin.ifInitialized
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.extension.formatted
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.entity.Player

class SpecialService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Special Service"
    override val dependencies = persistentListOf(OriginService::class)

    val specialStates = lazy {
        persistentListOf(
            Trigger.ON,
            Trigger.OFF,
            Trigger.NIGHT,
            Trigger.NETHER,
            Trigger.THE_END,
            Trigger.OVERWORLD,
        )
    }
    val specialStatesFormatted by lazy { specialStates.value.map { it.formatted("_", false) }.toTypedArray() }

    override suspend fun handleEnable() { }

    override suspend fun doUnload() {
        specialStates.ifInitialized { clear() }
    }

    fun isValidTrigger(trigger: Trigger) = specialStates.value.contains(trigger)

    fun getToggle(player: Player, trigger: Trigger) = when (trigger) {
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
}
