package dev.racci.terix.core.extensions

import dev.racci.terix.api.OriginService
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.AbstractOrigin
import org.bukkit.potion.PotionEffect

fun PotionEffect?.fromOrigin(): Boolean {
    if (this == null) return false
    return PotionEffectBuilder.regex.matches(key.toString())
}

fun PotionEffect.origin(): AbstractOrigin? {
    val key = this.key ?: return null
    val match = PotionEffectBuilder.regex.matchEntire(key.key)?.groups ?: return null
    return OriginService.getService().getOriginOrNull(match["origin"]?.value)
}
