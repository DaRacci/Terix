package dev.racci.terix.core.extension

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.core.services.OriginServiceImpl
import org.bukkit.potion.PotionEffect

private val originService by getKoin().inject<OriginServiceImpl>()

fun PotionEffect?.fromOrigin() = this?.key?.namespace == "origin"

fun PotionEffect.origin() = if (this.fromOrigin()) originService.getOrigin(this.key!!.key) else null
