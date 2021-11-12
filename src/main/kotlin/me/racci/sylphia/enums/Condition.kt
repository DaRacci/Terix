package me.racci.sylphia.enums

import me.racci.sylphia.Sylphia

enum class Condition {

    /**
     * Origin Passive Conditions
     */
    PARENT,
    DAY,
    NIGHT,
    WATER,
    LAVA,
    OVERWORLD,
    NETHER,
    END,
    /**
     * Origin Ability Conditions
     */
    LEVITATION,

    /**
     * Other stuff
     */
    NULL;

    private val keyArray = arrayOf(
        Sylphia.namespacedKey("condition_parent"),
        Sylphia.namespacedKey("condition_day"),
        Sylphia.namespacedKey("condition_night"),
        Sylphia.namespacedKey("condition_water"),
        Sylphia.namespacedKey("condition_lava"),
        Sylphia.namespacedKey("condition_overworld"),
        Sylphia.namespacedKey("condition_nether"),
        Sylphia.namespacedKey("condition_end"),
        Sylphia.namespacedKey("condition_levitation"),
        Sylphia.namespacedKey("condition_null"),
    )

    val key = keyArray[this.ordinal]
}