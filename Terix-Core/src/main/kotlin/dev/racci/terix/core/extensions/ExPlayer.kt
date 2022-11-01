package dev.racci.terix.core.extensions

import dev.racci.terix.api.TerixPlayer
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

public fun Player.inDarkness(): Boolean = inventory.itemInMainHand.type != Material.TORCH &&
    inventory.itemInOffHand.type != Material.TORCH &&
    location.block.lightLevel < 5

public var Player.originTime: Instant
    get() {
        return transaction { TerixPlayer[this@originTime].lastChosenTime ?: Instant.DISTANT_PAST }
    }
    set(instant) {
        transaction { TerixPlayer[this@originTime].lastChosenTime = instant }
    }

public var Player.freeChanges: Int
    get() {
        return transaction { TerixPlayer[this@freeChanges].freeChanges }
    }
    set(value) {
        transaction { TerixPlayer[this@freeChanges].freeChanges = value }
    }

public val Player.tickCache: TerixPlayer.PlayerTickCache get() = TerixPlayer.cachedTicks(this)

public var Player.wasInSunlight: Boolean
    get() = tickCache.wasInSunlight
    set(bool) { tickCache.wasInSunlight = bool }

public var Player.wasInDarkness: Boolean
    get() = tickCache.wasInDarkness
    set(bool) { tickCache.wasInDarkness = bool }

public var Player.wasInWater: Boolean
    get() = tickCache.wasInWater
    set(bool) { tickCache.wasInWater = bool }

public var Player.wasInRain: Boolean
    get() = tickCache.wasInRain
    set(bool) { tickCache.wasInRain = bool }

public var Player.inSunlight: Boolean
    get() = tickCache.inSunlight
    set(bool) { tickCache.inSunlight = bool }

public var Player.inDarkness: Boolean
    get() = tickCache.inDarkness
    set(bool) { tickCache.inDarkness = bool }

public var Player.inWater: Boolean
    get() = tickCache.inWater
    set(bool) { tickCache.inWater = bool }

public var Player.inRain: Boolean
    get() = tickCache.inRain
    set(bool) { tickCache.inRain = bool }

public infix fun Component.message(receiver: Collection<Player>) { for (audience in receiver) { audience.sendMessage(this) } }
