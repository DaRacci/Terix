package dev.racci.terix.core.extensions

import arrow.core.Some
import arrow.core.getOrElse
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.extensions.allOriginPotions
import dev.racci.terix.api.extensions.originPassiveModifiers
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.jetbrains.exposed.sql.transactions.transaction

public fun Player.sanitise() {
    allOriginPotions.map(PotionEffect::getType).forEach(this::removePotionEffect)
    originPassiveModifiers.forEach { it.value.forEach(it.key::removeModifier) }
}

public fun Player.inDarkness(): Boolean = inventory.itemInMainHand.type != Material.TORCH &&
    inventory.itemInOffHand.type != Material.TORCH &&
    location.block.lightLevel < 5

public var Player.originTime: Instant
    get() = transaction { TerixPlayer[this@originTime].lastChosenTime.getOrElse { Instant.DISTANT_PAST } }
    set(instant) = transaction { TerixPlayer[this@originTime].lastChosenTime = Some(instant) }

public var Player.freeChanges: Int
    get() {
        return transaction { TerixPlayer[this@freeChanges].freeChanges }
    }
    set(value) {
        transaction { TerixPlayer[this@freeChanges].freeChanges = value }
    }

public infix fun Component.message(receiver: Collection<Player>) { for (audience in receiver) { audience.sendMessage(this) } }
