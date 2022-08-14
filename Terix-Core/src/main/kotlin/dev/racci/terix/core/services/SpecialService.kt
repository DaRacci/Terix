package dev.racci.terix.core.services

// @MappedExtension(Terix::class, "Special Service", [OriginService::class])
// class SpecialService(override val plugin: Terix) : Extension<Terix>() {
//
//    val specialStates by lazy {
//        persistentListOf<State>(
//            State.CONSTANT,
//            State.TimeState.NIGHT,
//            State.WorldState.NETHER,
//            State.WorldState.END,
//            State.WorldState.OVERWORLD
//        )
//    }
//    val specialStatesFormatted by lazy { specialStates.map { formattedString(it.name) }.toTypedArray() }
//
//    fun isValidTrigger(state: State) = specialStates.contains(state)
//
//    /**
//     * Gets the opposite of the given trigger in the form of on or off.
//     *
//     * @param player The player to get the opposite trigger for.
//     * @param trigger The trigger to get the opposite of.
//     */
//    fun getToggle(
//        player: Player,
//        state: State
//    ) = when (state) {
//        State.CONSTANT -> null
//        State.TimeState.DAY ->
//        Trigger.NIGHT -> if (player.isNight) Trigger.ON else Trigger.OFF
//        Trigger.DAY -> if (player.isDay) Trigger.ON else Trigger.OFF
//        Trigger.DARKNESS -> if (player.location.block.lightLevel > 8) Trigger.ON else Trigger.OFF
//        Trigger.NETHER -> if (player.inNether) Trigger.OFF else Trigger.ON
//        Trigger.OVERWORLD -> if (player.inOverworld) Trigger.OFF else Trigger.ON
//        Trigger.THE_END -> if (player.inEnd) Trigger.OFF else Trigger.ON
//        else -> error { "Unknown special trigger: $trigger" }
//    }
//
//    private fun formattedString(string: String): String {
//        val split = string.lowercase().split("_").toMutableList()
//        return split.joinToString("_")
//    }
//
//    companion object : ExtensionCompanion<SpecialService>()
// }
