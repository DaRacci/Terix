package dev.racci.terix.core.extensions

import dev.racci.terix.api.OriginService
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.potion.PotionEffect

public fun PotionEffect?.fromOrigin(): Boolean {
    if (this == null) return false
    return PotionEffectBuilder.regex.matches(key.toString())
}

public fun PotionEffect.origin(): Origin? {
    val key = this.key ?: return null
    val match = PotionEffectBuilder.regex.matchEntire(key.key)?.groups ?: return null
    return OriginService.getOriginOrNull(match["origin"]?.value)
}
