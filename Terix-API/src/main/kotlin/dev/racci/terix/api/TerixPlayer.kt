package dev.racci.terix.api

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.toOption
import arrow.fx.coroutines.Atomic
import dev.racci.minix.api.extensions.onlinePlayer
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import java.util.UUID

// TODO -> Maybe mark all as immutable and make a new class when origin changes?
public class TerixPlayer private constructor(
    id: EntityID<UUID>,
    public val backingPlayer: CraftPlayer = onlinePlayer(id.value) as? CraftPlayer ?: error("Player with UUID $id is not online")
) : UUIDEntity(id), Player by backingPlayer {
    public val ticks: TickCache = TickCache()

    public var lastChosenTime: Option<Instant> by User.lastChosenTime
    public var freeChanges: Int by User.freeChanges
    public val grants: MutableSet<String> by User.grants
    public var origin: Origin by User.origin

    public val handle: ServerPlayer get() = backingPlayer.handle

    public data class TickCache internal constructor(
        public val sunlight: TwoStateCache = TwoStateCache(),
        public val darkness: TwoStateCache = TwoStateCache(),
        public val water: TwoStateCache = TwoStateCache(),
        public val rain: TwoStateCache = TwoStateCache()
    ) {
        public open class TwoStateCache internal constructor() {
            protected open val last: Atomic<Boolean> = Atomic.unsafe(false)
            protected open val current: Atomic<Boolean> = Atomic.unsafe(false)

            public suspend fun update(f: (value: Boolean) -> Boolean) {
                val newLast = current.getAndUpdate(f)
                last.set(newLast)
            }

            public suspend fun last(): Boolean = last.get()

            public suspend fun current(): Boolean = current.get()
        }
    }

    public companion object : UUIDEntityClass<TerixPlayer>(User, TerixPlayer::class.java, ::TerixPlayer) {
        public operator fun get(player: Player): TerixPlayer = get(player.uniqueId)

        @Deprecated("Use class methods instead", ReplaceWith("TerixPlayer[player].ticks"))
        public fun cachedTicks(player: Player): TerixPlayer.TickCache = get(player).ticks

        @JvmName("cachedOriginNotNull")
        @Suppress("INAPPLICABLE_JVM_NAME")
        @Deprecated("Use class methods instead", ReplaceWith("TerixPlayer[player].origin"))
        public fun cachedOrigin(player: Player): Origin = this[player].origin

        @JvmName("cachedOriginNullable")
        @Suppress("INAPPLICABLE_JVM_NAME")
        @Deprecated("Use class methods instead", ReplaceWith("player?.let { TerixPlayer[player].origin }"))
        public fun cachedOrigin(player: Player?): Origin? = when (player) {
            null -> null
            else -> this[player].origin
        }
    }

    public object User : UUIDTable("user") {

        public val origin: ColumnWithTransform<String, Origin> = text("origin", eagerLoading = true)
            .clientDefault { OriginService.defaultOrigin.name.lowercase() }
            .memoizedTransform({ origin -> origin.name.lowercase() }) { rawText ->
                OriginService.getOriginOrNull(rawText).toOption()
                    .tapNone { getKoin().get<Terix>().log.error { "Previous origin [$rawText] wasn't found, using default origin." } }
                    .getOrElse(OriginService::defaultOrigin)
            }

        public val lastChosenTime: ColumnWithTransform<Instant?, Option<Instant>> = timestamp("last_chosen_time").nullable()
            .default(null)
            .transform({ option -> option.orNull() }) { rawInstant -> Option.fromNullable(rawInstant) }

        public val freeChanges: Column<Int> = integer("free_changes").default(DataService.getService().get<TerixConfig>().freeChanges)

        public val grants: ColumnWithTransform<String, MutableSet<String>> = text("explicit_grants", eagerLoading = true)
            .default("")
            .memoizedTransform({ transformed -> transformed.joinToString(",") }) { rawText -> rawText.split(",").toMutableSet() }
    }
}
