package dev.racci.terix.core.storage

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.services.OriginService
import org.jetbrains.exposed.dao.id.UUIDTable

object User : UUIDTable("user") {

    val origin = text("origin").default(getKoin().get<OriginService>().defaultOrigin.name.lowercase())
    val lastOrigin = text("last_origin").nullable().default(null)

    val nightVision = enumeration("night_vision", Trigger::class).default(Trigger.NIGHT)
    val jumpBoost = enumeration("jump_boost", Trigger::class).default(Trigger.ON)
    val slowFall = enumeration("slow_falling", Trigger::class).default(Trigger.ON)
}
