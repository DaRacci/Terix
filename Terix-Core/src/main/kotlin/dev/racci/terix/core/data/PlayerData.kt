package dev.racci.terix.core.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.services.OriginService
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.time.Duration
import java.util.UUID

class PlayerData(private val uuid: EntityID<UUID>) : UUIDEntity(uuid) {
    companion object : UUIDEntityClass<PlayerData>(User), KoinComponent {

        operator fun get(player: Player): PlayerData = get(player.uniqueId)

        fun tickCache(player: Player): PlayerTickCache = tickCache[player.uniqueId]

        internal val originCache: LoadingCache<UUID, AbstractOrigin> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15)) // We don't want offline players
            .refreshAfterWrite(Duration.ofSeconds(15)) // To ensure we are 100% up to date
            .build { uuid: UUID -> get<OriginService>()[transaction { this@Companion[uuid]._origin }]!! }

        private val tickCache: LoadingCache<UUID, PlayerTickCache> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15))
            .build { PlayerTickCache() }

        class PlayerTickCache {
            var wasInSunlight: Boolean = false
            var wasInDarkness: Boolean = false
            var wasInWater: Boolean = false
            var wasInRain: Boolean = false
            var inSunlight: Boolean = false
            var inDarkness: Boolean = false
            var inWater: Boolean = false
            var inRain: Boolean = false
        }
    }

    private var _origin: String by User.origin
    var lastOrigin: String? by User.lastOrigin; private set

    var origin: AbstractOrigin
        get() = originCache[uuid.value]
        set(value) {
            lastOrigin = _origin
            _origin = value.name.lowercase()
            originCache.put(uuid.value, value)
        }

    var lastChosenTime: Instant? by User.lastChosenTime

    var nightVision: Trigger by User.nightVision
    var jumpBoost: Trigger by User.jumpBoost
    var slowFalling: Trigger by User.slowFall
}