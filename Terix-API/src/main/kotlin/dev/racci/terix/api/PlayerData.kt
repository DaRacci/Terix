package dev.racci.terix.api

import dev.racci.minix.api.utils.getKoin
import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.koin.core.component.KoinComponent
import java.util.UUID

abstract class PlayerData(val uuid: EntityID<UUID>) : UUIDEntity(uuid) {

    abstract var origin: Origin
    abstract var lastOrigin: String?
    abstract var lastChosenTime: Instant?
    abstract var usedChoices: Int

    interface Cache : KoinComponent {
        operator fun get(player: Player): PlayerData
        fun cachedOrigin(player: Player): Origin
        fun cachedTicks(player: Player): PlayerTickCache
    }

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

    companion object : Cache by getKoin().get()
}
