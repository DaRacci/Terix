package dev.racci.terix.core.extensions

import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.PlayerData
import dev.racci.terix.api.Terix
import kotlinx.datetime.Instant
import net.kyori.adventure.text.Component
import net.minecraft.core.BlockPos
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction

private val terix by getKoin().inject<Terix>()

fun Player.safelyAddPotion(potion: PotionEffect) {
    terix.launch { addPotionEffect(potion) }
}

fun Player.safelyRemovePotion(potionType: PotionEffectType) {
    terix.launch { removePotionEffect(potionType) }
}

fun Player.inDarkness(): Boolean = inventory.itemInMainHand.type != Material.TORCH &&
    inventory.itemInOffHand.type != Material.TORCH &&
    location.block.lightLevel < 5

fun Player.canSeeSky(): Boolean {
    val nms = toNMS()
    val pos = BlockPos(nms.x, nms.eyeY, nms.z)
    return world.toNMS().canSeeSky(pos)
}

var Player.originTime: Instant
    get() { return transaction { PlayerData[this@originTime].lastChosenTime ?: Instant.DISTANT_PAST } }
    set(instant) { transaction { PlayerData[this@originTime].lastChosenTime = instant } }

var Player.usedChoices: Int
    get() { return transaction { PlayerData[this@usedChoices].usedChoices } }
    set(value) { transaction { PlayerData[this@usedChoices].usedChoices = value } }

val Player.tickCache: PlayerData.PlayerTickCache get() = PlayerData.cachedTicks(this)

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
