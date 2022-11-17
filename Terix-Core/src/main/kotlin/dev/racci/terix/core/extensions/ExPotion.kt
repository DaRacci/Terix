package dev.racci.terix.core.extensions

import org.bukkit.potion.PotionEffect

public fun PotionEffect?.fromOrigin(): Boolean {
    if (this == null) return false
    return key.toString().startsWith("terix:origin__")
}
