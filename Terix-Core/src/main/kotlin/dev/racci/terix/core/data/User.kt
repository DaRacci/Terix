package dev.racci.terix.core.data

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.services.OriginServiceImpl
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object User : UUIDTable("user") {

    val origin = text("origin").default(getKoin().get<OriginServiceImpl>().defaultOrigin.name.lowercase())
    val lastOrigin = text("last_origin").nullable().default(null)

    val lastChosenTime = timestamp("last_chosen_time").nullable().default(null)
    val usedChoices = integer("used_choices").default(0)

    val nightVision = enumeration("night_vision", Trigger::class).default(Trigger.NIGHT)
    val jumpBoost = enumeration("jump_boost", Trigger::class).default(Trigger.ON)
    val slowFall = enumeration("slow_falling", Trigger::class).default(Trigger.ON)
}
