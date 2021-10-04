package me.racci.sylphia.origins


import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.racci.raccicore.Level
import me.racci.raccicore.log
import me.racci.raccicore.skedule.BukkitDispatcher
import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.enums.Condition
import me.racci.sylphia.enums.Special
import me.racci.sylphia.factories.Origin
import me.racci.sylphia.handlers.AttributeHandler
import me.racci.sylphia.handlers.PotionHandler
import me.racci.sylphia.originFactory
import me.racci.sylphia.playerManager
import me.racci.sylphia.plugin
import me.racci.sylphia.utils.getConditions
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.io.File
import java.util.*


/**
 * Origin manager
 *
 * @property plugin
 * @constructor Create empty Origin manager
 */
class OriginManager {

    val origins = LinkedHashMap<String, Origin>()

    init {
        refresh()
    }

    fun refresh() {
        origins.clear()
        val configs = getConfigs()
        if(configs.isNotEmpty()) {
            for(entry in configs.entries) {
                if(!origins.containsKey(entry.key)) {
                    origins[entry.key] = originFactory.generate(entry.value)
                }
            }
        } else {
            log(Level.WARNING, "No origins found!")
        }
    }

    private fun getConfigs() : Map<String, YamlConfiguration> {
        val var1 = HashMap<String, YamlConfiguration>()
        val var2 = File("${plugin.dataFolder.absolutePath}/Origins").listFiles()
        if(var2 != null) {
            for(fvar1 in var2) {
                if(!fvar1.isDirectory) {
                    var1[fvar1.name.uppercase().replace(".YML", "")] = YamlConfiguration.loadConfiguration(fvar1)
                }
            }
        }
        return var1
    }

    companion object {
        private val originMap: MutableMap<String, Origin> = LinkedHashMap()
        fun addToMap(origin: Origin) {
            originMap.putIfAbsent(origin.identity.name.uppercase(), origin)
        }

        fun valueOf(name: String): Origin {
            return originMap[name.uppercase()] ?: throw IllegalArgumentException("No Origins by the name $name found")
        }

        fun values(): Array<Origin> {
            return originMap.values.toTypedArray().clone()
        }
        fun contains(name: String): Boolean {
            return originMap.contains(name.uppercase())
        }
    }

    private fun getOriginName(player: Player): String {
        return playerManager.getPlayerData(player.uniqueId)?.origin?.uppercase() ?: "LOST"
    }
    private fun getOriginName(uuid: UUID) : String {
        return playerManager.getPlayerData(uuid)?.origin?.uppercase() ?: "LOST"
    }
    fun getOrigin(player: Player): Origin? {
        return origins[getOriginName(player)]
    }
    fun getOrigin(uuid: UUID) : Origin? {
        return origins[getOriginName(uuid)]
    }

    fun refreshAll(player: Player, origin: Origin? = getOrigin(player)) = runBlocking {
        AttributeHandler.reset(player)
        launch(BukkitDispatcher(plugin, false)) { PotionHandler.reset(player) }
        AttributeHandler.setBase(player, origin)
        launch(BukkitDispatcher(plugin, false)) { PotionHandler.setBase(player, origin) }
        getConditions(player).apply {
            AttributeHandler::setCondition
            launch(BukkitDispatcher(plugin, false)) {
                ::forEach { PotionHandler::setCondition }
            }
        }
    }

    fun removeAll(player: Player) = runBlocking {
        AttributeHandler.reset(player)
        launch(BukkitDispatcher(plugin, false)) {
            PotionHandler.reset(player)
        }
    }

    fun addCondition(player: Player, condition: Condition, origin: Origin? = getOrigin(player)) = runBlocking {
        if(origin == null) return@runBlocking
        AttributeHandler.setCondition(player, condition, origin)
        launch(BukkitDispatcher(plugin, false)) {
            PotionHandler.setCondition(player, condition, origin)
        }
    }

    fun removeCondition(player: Player, condition: Condition, origin: Origin? = getOrigin(player)) = runBlocking {
        if(origin == null) return@runBlocking
        AttributeHandler.removeCondition(player, condition, origin)
        launch(BukkitDispatcher(plugin, false)) {
            PotionHandler.removeCondition(player, condition, origin)
        }
    }

    private fun refreshNightVision(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val nightVision = playerData.getOriginSetting(Special.NIGHTVISION)
        if(origin.special.nightVision && nightVision > 0) {
            when(nightVision) {
                1 -> if(WorldTime.isNight(player)) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false)) else player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                2 -> if(!player.hasPotionEffect(PotionEffectType.NIGHT_VISION) || player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.hasIcon()!!) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                else -> TODO()
            }
        }
    }
    private fun refreshJump(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val jumpBoost = playerData.getOriginSetting(Special.JUMPBOOST)
        if(origin.special.jumpBoost > 0 ) {
            if(jumpBoost > 0 && (!player.hasPotionEffect(PotionEffectType.JUMP) || player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, jumpBoost, true, false, false))
            } else if(jumpBoost == 0 && !player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.JUMP)
            }
        }
    }
    private fun refreshSlowFalling(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val slowFalling = playerData.getOriginSetting(Special.SLOWFALLING)
        if(origin.special.slowFalling) {
            if(slowFalling == 1 && (!player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Int.MAX_VALUE, 0, true, false, false))
            } else if(slowFalling == 0 && !player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
    }
}