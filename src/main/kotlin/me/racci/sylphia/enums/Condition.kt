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

    fun toNamespacedkey() =
        Sylphia.namespacedKey("condition_${name.lowercase()}")

}