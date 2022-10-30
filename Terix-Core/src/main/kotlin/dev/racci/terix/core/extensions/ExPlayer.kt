package dev.racci.terix.core.extensions

import dev.racci.terix.api.TerixPlayer
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction

fun Player.inDarkness(): Boolean = inventory.itemInMainHand.type != Material.TORCH &&
    inventory.itemInOffHand.type != Material.TORCH &&
    location.block.lightLevel < 5

var Player.originTime: Instant
    get() {
        return transaction { TerixPlayer[this@originTime].lastChosenTime ?: Instant.DISTANT_PAST }
    }
    set(instant) {
        transaction { TerixPlayer[this@originTime].lastChosenTime = instant }
    }

var Player.freeChanges: Int
    get() {
        return transaction { TerixPlayer[this@freeChanges].freeChanges }
    }
    set(value) {
        transaction { TerixPlayer[this@freeChanges].freeChanges = value }
    }

val Player.tickCache: TerixPlayer.PlayerTickCache get() = TerixPlayer.cachedTicks(this)

var Player.wasInSunlight
    get() = tickCache.wasInSunlight
    set(bool) { tickCache.wasInSunlight = bool }

var Player.wasInDarkness
    get() = tickCache.wasInDarkness
    set(bool) { tickCache.wasInDarkness = bool }

var Player.wasInWater
    get() = tickCache.wasInWater
    set(bool) { tickCache.wasInWater = bool }

var Player.wasInRain
    get() = tickCache.wasInRain
    set(bool) { tickCache.wasInRain = bool }

var Player.inSunlight
    get() = tickCache.inSunlight
    set(bool) { tickCache.inSunlight = bool }

var Player.inDarkness
    get() = tickCache.inDarkness
    set(bool) { tickCache.inDarkness = bool }

var Player.inWater
    get() = tickCache.inWater
    set(bool) { tickCache.inWater = bool }

var Player.inRain
    get() = tickCache.inRain
    set(bool) { tickCache.inRain = bool }

infix fun Component.message(receiver: Collection<Player>) { for (audience in receiver) { audience.sendMessage(this) } }
