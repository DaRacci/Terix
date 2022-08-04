package dev.racci.terix.core.data

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.terix.api.PlayerData
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.api.origins.origin.AbstractOrigin
import dev.racci.terix.core.services.OriginServiceImpl
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.util.UUID

class PlayerData(uuid: EntityID<UUID>) : PlayerData(uuid) {
    private var _origin: String by User.origin
    override var lastOrigin: String? by User.lastOrigin

    override var origin: AbstractOrigin
        get() = originCache[uuid.value]
        set(value) {
            lastOrigin = _origin
            _origin = value.name.lowercase()
            originCache.put(uuid.value, value)
        }

    override var lastChosenTime: Instant? by User.lastChosenTime
    override var usedChoices: Int by User.usedChoices
    override var nightVision: Trigger by User.nightVision
    override var jumpBoost: Trigger by User.jumpBoost
    override var slowFalling: Trigger by User.slowFall

    companion object : UUIDEntityClass<PlayerData>(User), Cache {

        override operator fun get(player: Player): PlayerData = get(player.uniqueId)

        override fun cachedTicks(player: Player): PlayerTickCache = tickCache[player.uniqueId]

        override fun cachedOrigin(player: Player): AbstractOrigin = originCache[player.uniqueId]

        // TODO: Implement defaulting to the default origin
        // Actually, is this needed? does jetbrains exposed already have a cache?
        private val originCache: LoadingCache<UUID, AbstractOrigin> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15)) // We don't want offline players
            .refreshAfterWrite(Duration.ofSeconds(15)) // To ensure we are 100% up to date
            .build { uuid: UUID ->
                val origin = transaction { this@Companion[uuid].origin.name.lowercase() }
                OriginServiceImpl.getService().getOriginOrNull(origin) ?: OriginServiceImpl.getService().defaultOrigin
            }

        private val tickCache: LoadingCache<UUID, PlayerTickCache> = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofSeconds(15))
            .build { PlayerTickCache() }
    }
}
