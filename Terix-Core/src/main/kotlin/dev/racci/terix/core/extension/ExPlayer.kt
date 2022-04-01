package dev.racci.terix.core.extension

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import dev.racci.terix.core.data.PlayerData
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import net.minecraft.core.BlockPos
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction

private val terix by getKoin().inject<Terix>()

fun Player.origin(): AbstractOrigin = PlayerData.originCache[uniqueId]

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

fun Player.safelyAddPotion(potion: PotionEffect) {
    terix.launch {
        addPotionEffect(potion)
    }
}

fun Player.safelyRemovePotion(potionType: PotionEffectType) {
    terix.launch {
        removePotionEffect(potionType)
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

fun Player.validToBurn(): Boolean = !with(toNMS()) { isInWaterRainOrBubble || isInPowderSnow || wasInPowderSnow }

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

val Player.tickCache: PlayerData.Companion.PlayerTickCache get() = PlayerData.tickCache(this)

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
