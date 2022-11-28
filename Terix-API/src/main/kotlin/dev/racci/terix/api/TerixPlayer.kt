package dev.racci.terix.api

import arrow.core.getOrElse
import arrow.core.toOption
import arrow.fx.coroutines.Atomic
import com.github.benmanes.caffeine.cache.Caffeine
import dev.racci.minix.api.extensions.onlinePlayer
import dev.racci.minix.api.services.DataService
import dev.racci.minix.core.services.DataServiceImpl.DataHolder.Companion.getKoin
import dev.racci.minix.core.services.DataServiceImpl.DataHolder.Companion.memoizedTransform
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.StorageService
import kotlinx.datetime.Instant
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.kyori.adventure.title.TitlePart
import net.minecraft.server.level.ServerPlayer
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.dao.DaoEntityID
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.koin.core.component.get
import java.util.UUID

// TODO -> Maybe mark all as immutable and make a new class when origin changes?
// TODO -> Passing this player instance can cause issues as bukkit just casts to CraftPlayer.
public class TerixPlayer private constructor(
    private val id: DaoEntityID<UUID>,
    public val backingPlayer: CraftPlayer = onlinePlayer(id.value) as? CraftPlayer ?: error("Player with UUID $id is not online"),
    public val ticks: TickCache = TickCache()
) : Player by backingPlayer, OverrideFixer {
    public val databaseEntity: TerixPlayerEntity get() {
        return if (TransactionManager.currentOrNull() != null) {
            TerixPlayerEntity[id]
        } else StorageService.transaction { TerixPlayerEntity[this@TerixPlayer.id] } // Maybe readonly for this?
    }

    public var origin: Origin = databaseEntity.origin
        set(value) {
            field = value
            databaseEntity.origin = value
        }

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

    public class TerixPlayerEntity(
        id: EntityID<UUID>
    ) : UUIDEntity(id) {
        public var lastChosenTime: Instant by User.lastChosenTime
        public var freeChanges: Int by User.freeChanges
        public val grants: MutableSet<String> by User.grants
        public var origin: Origin by User.origin

        public companion object : UUIDEntityClass<TerixPlayerEntity>(User, TerixPlayerEntity::class.java, ::TerixPlayerEntity)
    }

    public companion object {
        private val playerCache = Caffeine.newBuilder().weakKeys()
            .build<UUID, TerixPlayer>()

        public operator fun get(player: Player): TerixPlayer {
            if (player is TerixPlayer) return player

            return playerCache.get(player.uniqueId) {
                TerixPlayer(DaoEntityID(player.uniqueId, User))
            }
        }

        public operator fun get(uuid: UUID): TerixPlayer {
            return playerCache.get(uuid) {
                TerixPlayer(DaoEntityID(uuid, User))
            }
        }

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

        public val lastChosenTime: Column<Instant> = timestamp("last_chosen_time").default(Instant.DISTANT_PAST)

        public val freeChanges: Column<Int> = integer("free_changes")
            .clientDefault { DataService.get<TerixConfig>().freeChanges }
            .check { it greaterEq 0 }

        public val grants: ColumnWithTransform<String, MutableSet<String>> = text("explicit_grants")
            .default("")
            .memoizedTransform({ transformed -> transformed.joinToString(",") }) { rawText -> rawText.split(",").toMutableSet() }
    }

    override fun sendActionBar(message: Component) {
        backingPlayer.sendActionBar(message)
    }

    override fun sendPlayerListHeader(header: Component) {
        backingPlayer.sendPlayerListHeader(header)
    }

    override fun sendPlayerListFooter(footer: Component) {
        backingPlayer.sendPlayerListFooter(footer)
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
        backingPlayer.sendPlayerListHeaderAndFooter(header, footer)
    }

    override fun showTitle(title: Title) {
        backingPlayer.showTitle(title)
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
        backingPlayer.sendTitlePart(part, value)
    }

    override fun clearTitle() {
        backingPlayer.clearTitle()
    }

    override fun showBossBar(bar: BossBar) {
        backingPlayer.showBossBar(bar)
    }

    override fun hideBossBar(bar: BossBar) {
        backingPlayer.hideBossBar(bar)
    }

    override fun playSound(sound: Sound) {
        backingPlayer.playSound(sound)
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
        backingPlayer.playSound(sound, x, y, z)
    }

    override fun playSound(sound: Sound, emitter: Sound.Emitter) {
        backingPlayer.playSound(sound, emitter)
    }

    override fun stopSound(stop: SoundStop) {
        backingPlayer.stopSound(stop)
    }

    override fun openBook(book: Book) {
        backingPlayer.openBook(book)
    }
}

/** A group of overrides, which are overridden within [CraftPlayer] but not [Player] and need manual overrides. */
public interface OverrideFixer : Audience {

    override fun sendActionBar(message: Component)
    override fun sendPlayerListHeader(header: Component)
    override fun sendPlayerListFooter(footer: Component)
    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component)
    override fun showTitle(title: Title)
    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T)
    override fun clearTitle()
    override fun resetTitle()
    override fun showBossBar(bar: BossBar)
    override fun hideBossBar(bar: BossBar)
    override fun playSound(sound: Sound)
    override fun playSound(sound: Sound, x: Double, y: Double, z: Double)
    override fun playSound(sound: Sound, emitter: Sound.Emitter)
    override fun stopSound(stop: SoundStop)
    override fun openBook(book: Book)
}
