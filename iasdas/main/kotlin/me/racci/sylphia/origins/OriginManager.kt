package me.racci.terix.origins

import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.racci.raccicore.skedule.BukkitDispatcher
import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.terix.Terix
import me.racci.terix.data.PlayerData
import me.racci.terix.data.PlayerManager
import me.racci.terix.enums.Condition
import me.racci.terix.enums.Special
import me.racci.terix.factories.Origin
import me.racci.terix.factories.OriginFactory
import me.racci.terix.handlers.AttributeHandler
import me.racci.terix.handlers.PotionHandler
import me.racci.terix.utils.getConditions
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.UUID

internal object OriginManager {

    private val origins = LinkedHashMap<String, Origin>()

    fun init() {
        reload()
    }

    fun close() {
        origins.clear()
    }

    fun reload() {
        origins.clear()
        val configs = HashMap<String, YamlConfiguration>()
        val files = File("${Terix.instance.dataFolder.absolutePath}/Origins").listFiles() ?: emptyArray()
        files.filterNot { it.isDirectory }
            .forEach {
                configs[it.name.uppercase().replace(".YML", "")] =
                    YamlConfiguration.loadConfiguration(it)
            }

        if (configs.isNotEmpty()) {
            configs.entries.filterNot { origins.containsKey(it.key) }
                .forEach { origins[it.key] = OriginFactory.generate(it.value) }
        } else Terix.log.warning("No origins found!")
    }

    private fun getOriginName(player: Player): String {
        return PlayerManager[player.uniqueId].origin?.uppercase() ?: "LOST"
    }
    private fun getOriginName(uuid: UUID): String {
        return PlayerManager[uuid].origin?.uppercase() ?: "LOST"
    }
    fun getOrigin(player: Player): Origin? {
        return origins[getOriginName(player)]
    }
    fun getOrigin(uuid: UUID): Origin? {
        return origins[getOriginName(uuid)]
    }

    fun refreshAll(player: Player, origin: Origin? = getOrigin(player)) = runBlocking {
        AttributeHandler.reset(player)
        launch(BukkitDispatcher(Terix.instance, false)) { PotionHandler.reset(player) }
        AttributeHandler.setBase(player, origin)
        launch(BukkitDispatcher(Terix.instance, false)) { PotionHandler.setBase(player, origin) }
        getConditions(player).apply {
            AttributeHandler::setCondition
            launch(BukkitDispatcher(Terix.instance, false)) {
                ::forEach { PotionHandler::setCondition }
            }
        }
    }

    suspend fun removeAll(player: Player) = coroutineScope() {
        AttributeHandler.reset(player)
        launch(BukkitDispatcher(Terix.instance, false)) {
            PotionHandler.reset(player)
        }
    }

    suspend fun addCondition(player: Player, condition: Condition, origin: Origin? = getOrigin(player)) = coroutineScope() {
        if (origin == null) cancel()
        AttributeHandler.setCondition(player, condition, origin)
        launch(BukkitDispatcher(Terix.instance, false)) {
            PotionHandler.setCondition(player, condition, origin)
        }
    }

    suspend fun removeCondition(player: Player, condition: Condition, origin: Origin? = getOrigin(player)) = coroutineScope() {
        if (origin == null) cancel()
        AttributeHandler.removeCondition(player, condition, origin)
        launch(BukkitDispatcher(Terix.instance, false)) {
            PotionHandler.removeCondition(player, condition, origin)
        }
    }

    fun refreshNightVision(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = PlayerManager[player.uniqueId]) {
        val nightVision = playerData[Special.NIGHTVISION]
        if (origin.special.nightVision && nightVision > 0) {
            when (nightVision) {
                1 -> if (WorldTime.isNight(player)) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false)) else player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                2 -> if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION) || player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.hasIcon()!!) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                else -> TODO()
            }
        }
    }
    fun refreshJump(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = PlayerManager[player.uniqueId]) {
        val jumpBoost = playerData[Special.JUMPBOOST]
        if (origin.special.jumpBoost > 0) {
            if (jumpBoost > 0 && (!player.hasPotionEffect(PotionEffectType.JUMP) || player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, jumpBoost, true, false, false))
            } else if (jumpBoost == 0 && !player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.JUMP)
            }
        }
    }
    fun refreshSlowFalling(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = PlayerManager[player.uniqueId]) {
        val slowFalling = playerData[Special.SLOWFALLING]
        if (origin.special.slowFalling) {
            if (slowFalling == 1 && (!player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Int.MAX_VALUE, 0, true, false, false))
            } else if (slowFalling == 0 && !player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
    }

    fun addToMap(origin: Origin) {
        origins.putIfAbsent(origin.identity.name.uppercase(), origin)
    }

    fun valueOf(name: String) =
        origins[name.uppercase()] ?: throw IllegalArgumentException("No Origin by the name $name found")

    fun values() =
        origins.values.toTypedArray().clone()

    fun contains(name: String) =
        origins.contains(name.uppercase())
}
