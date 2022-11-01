package dev.racci.terix.api

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.data.TerixConfig
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.UUID

public class TerixPlayer(public val uuid: EntityID<UUID>) : UUIDEntity(uuid) {

    private var _origin: String by User.origin
    public var lastOrigin: String? by User.lastOrigin
    public var lastChosenTime: Instant? by User.lastChosenTime
    public var freeChanges: Int by User.freeChanges

    public var origin: Origin
        get() = originCache[uuid.value]
        set(value) {
            lastOrigin = _origin
            _origin = value.name.lowercase()
            originCache.put(uuid.value, value)
        }

    public interface Cache {
        public operator fun get(player: Player): TerixPlayer
        public fun cachedOrigin(player: Player): Origin
        public fun cachedTicks(player: Player): PlayerTickCache
    }

    public class PlayerTickCache {

        public var wasInSunlight: Boolean = false
        public var wasInDarkness: Boolean = false
        public var wasInWater: Boolean = false
        public var wasInRain: Boolean = false
        public var inSunlight: Boolean = false
        public var inDarkness: Boolean = false
        public var inWater: Boolean = false
        public var inRain: Boolean = false
    }

    public companion object : UUIDEntityClass<TerixPlayer>(User), Cache {
        override operator fun get(player: Player): TerixPlayer = get(player.uniqueId)

        override fun cachedTicks(player: Player): PlayerTickCache = tickCache[player.uniqueId]

        override fun cachedOrigin(player: Player): Origin = originCache[player.uniqueId]

        // TODO: Implement defaulting to the default origin
        // Actually, is this needed?
        // does jetbrains exposed already have a cache?
        private val originCache: LoadingCache<UUID, Origin> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15)) // We don't want offline players
            .refreshAfterWrite(Duration.ofSeconds(15)) // To ensure we are 100% up to date
            .build { uuid: UUID ->
                val koin = getKoin()
                val origin = transaction(koin.getProperty("terix:database")) { this@Companion[uuid]._origin }
                val originService = koin.get<OriginService>()
                val originInstance = originService.getOriginOrNull(origin)

                if (originInstance == null) {
                    koin.get<Terix>().log.warn { "Player $uuid had an invalid origin $origin, setting to default." }
                }

                originInstance ?: originService.defaultOrigin
            }

        private val tickCache: LoadingCache<UUID, PlayerTickCache> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15))
            .build { PlayerTickCache() }
    }

    object User : UUIDTable("user") {

        val origin = text("origin").default(getKoin().get<OriginService>().defaultOrigin.name.lowercase())
        val lastOrigin = text("last_origin").nullable().default(null)

        val lastChosenTime = timestamp("last_chosen_time").nullable().default(null)
        val freeChanges = integer("free_changes").default(DataService.getService().get<TerixConfig>().freeChanges)
    }
}
