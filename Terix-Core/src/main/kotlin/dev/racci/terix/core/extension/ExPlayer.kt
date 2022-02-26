package dev.racci.terix.core.extension

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.storage.PlayerData
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import net.minecraft.core.BlockPos
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction

private val terix by getKoin().inject<Terix>()

fun Player.origin(): AbstractOrigin = transaction { PlayerData[this@origin].origin }

fun Player.lastOrigin(): String? = transaction { PlayerData[this@lastOrigin].lastOrigin }

fun Player.shouldBeEffected(): Boolean =
    gameMode.ordinal != 0 &&
        gameMode.ordinal != 3

fun Player.safelyAddPotions(potions: Collection<PotionEffect>) {
    terix.launch {
        for (potion in potions) {
            addPotionEffect(potion)
        }
    }
}

fun Player.safelyRemovePotions(potionTypes: Collection<PotionEffectType>) {
    terix.launch {
        for (type in potionTypes) {
            removePotionEffect(type)
        }
    }
}

fun Player.safelySwapPotions(
    oldPotionTypes: Collection<PotionEffectType>?,
    newPotions: Collection<PotionEffect>?
) {
    terix.launch {
        oldPotionTypes?.let {
            for (type in it) {
                removePotionEffect(type)
            }
        }
        newPotions?.let {
            for (potion in it) {
                addPotionEffect(potion)
            }
        }
    }
}

fun Player.validToBurn(): Boolean = with(toNMS()) { isInWaterRainOrBubble || isInPowderSnow || wasInPowderSnow }

fun Player.inDarkness(): Boolean = eyeLocation.block.lightLevel <= 4

fun Player.canSeeSky(): Boolean {
    val nms = toNMS()
    val pos = BlockPos(nms.x, nms.eyeY, nms.z)
    return world.toNMS().canSeeSky(pos)
}

fun Player.getLightOrDarkTrigger(): Trigger? =
    if (canSeeSky()) Trigger.SUNLIGHT else if (inDarkness()) Trigger.DARKNESS else null

var Player.originTime: Instant
    get() { return transaction { PlayerData[this@originTime].lastChosenTime ?: Instant.DISTANT_PAST } }
    set(instant) { transaction { PlayerData[this@originTime].lastChosenTime = instant } }

infix fun Component.message(receiver: Collection<Player>) { for (audience in receiver) { audience.sendMessage(this) } }
