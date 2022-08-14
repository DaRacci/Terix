package dev.racci.terix.core.data

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.core.services.OriginServiceImpl
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object User : UUIDTable("user") {

    val origin = text("origin").default(getKoin().get<OriginServiceImpl>().defaultOrigin.name.lowercase())
    val lastOrigin = text("last_origin").nullable().default(null)

    val lastChosenTime = timestamp("last_chosen_time").nullable().default(null)
    val usedChoices = integer("used_choices").default(0)

    /*val nightVision = text("night_vision").nullable().default(State.TimeState.NIGHT.name)
    val jumpBoost = text("jump_boost").default(State.CONSTANT.name)
    val slowFall = text("slow_falling").default(State.CONSTANT.name)*/
}
