package dev.racci.terix.core.extension

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.core.services.OriginService
import org.bukkit.potion.PotionEffect

private val originService by getKoin().inject<OriginService>()

fun PotionEffect?.fromOrigin() = this?.key?.namespace == "origin"

fun PotionEffect.origin() = if (this.fromOrigin()) originService[this.key!!.key] else null
