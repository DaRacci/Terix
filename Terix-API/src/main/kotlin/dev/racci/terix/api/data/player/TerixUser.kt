package dev.racci.terix.api.data.player

import arrow.core.getOrElse
import arrow.core.toOption
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.now
import dev.racci.minix.core.services.DataServiceImpl
import dev.racci.minix.core.services.DataServiceImpl.DataHolder.Companion.memoizedTransform
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.koin.core.component.get

public object TerixUser : UUIDTable("user") {

    public val origin: ColumnWithTransform<String, Origin> = text("origin")
        .clientDefault { OriginService.defaultOrigin.name.lowercase() }
        .memoizedTransform({ origin -> origin.name.lowercase() }) { rawText ->
            OriginService.getOriginOrNull(rawText).toOption()
                .tapNone { DataServiceImpl.DataHolder.getKoin().get<Terix>().log.error { "Previous origin [$rawText] wasn't found, using default origin." } }
                .getOrElse(OriginService::defaultOrigin)
        }

    public val lastChosenTime: Column<Instant> = timestamp("last_chosen_time")
        .clientDefault { now() - DataService.get<TerixConfig>().intervalBeforeChange }

    public val freeChanges: Column<Int> = integer("free_changes")
        .clientDefault { DataService.get<TerixConfig>().freeChanges }
        .check { it greaterEq 0 }

    public val grants: ColumnWithTransform<String, MutableSet<String>> = text("explicit_grants")
        .default("")
        .memoizedTransform({ transformed -> transformed.joinToString(",") }) { rawText -> rawText.split(",").toMutableSet() }
}
