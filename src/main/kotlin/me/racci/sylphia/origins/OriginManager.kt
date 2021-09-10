@file:Suppress("unused")
@file:JvmName("OriginManager")
package me.racci.sylphia.origins


import me.racci.raccilib.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.enums.Special
import me.racci.sylphia.origins.OriginHandler.OriginsMap.originsMap
import me.racci.sylphia.utils.AttributeUtils
import me.racci.sylphia.utils.PotionUtils
import me.racci.sylphia.utils.getCondition
import org.apache.commons.collections4.ListUtils
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * Origin manager
 *
 * @property plugin
 * @constructor Create empty Origin manager
 */
class OriginManager(private val plugin: Sylphia) {
    private val playerManager = plugin.playerManager!!

    object Origins {
        private val originMap: MutableMap<String, Origin> = LinkedHashMap()
        fun addToMap(origin: Origin) {
            originMap.putIfAbsent(origin.name.uppercase(), origin)
        }

        fun valueOf(name: String): Origin {
            return originMap[name] ?: throw IllegalArgumentException("No Origins by the name $name found")
        }

        fun values(): Array<Origin> {
            return originMap.values.toTypedArray().clone()
        }
    }

    private fun getOriginName(player: Player): String? {
        val playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!
        return if (originsMap[playerData.origin] != null) playerData.origin!!.uppercase() else null
    }
    fun getOrigin(player: Player): Origin? {
        val originName = getOriginName(player)!!
        return if (originsMap.containsKey(originName)) originsMap[originName] else null
    }

    fun reset(player: Player) {
        for (potionEffect: PotionEffect in player.activePotionEffects) {
            if (PotionUtils.isOriginEffect(potionEffect)) {
                player.removePotionEffect(potionEffect.type)
            }
        }
        for (attribute: Attribute in AttributeUtils.getPlayerAttributes()) {
            val instance: AttributeInstance? = player.getAttribute(attribute)
            instance?.baseValue = AttributeUtils.getDefault(attribute)
        }
    }
    fun add(player: Player, origin: Origin = getOrigin(player)!!, condition: OriginValue = getCondition(player)) {
        origin.conditionAttributeMap[condition]?.forEach { var1x ->
            player.getAttribute(var1x!!.attribute)!!.baseValue = var1x.value
        }
        player.addPotionEffects(origin.conditionEffectMap[condition]!!)
    }
    fun refresh(player: Player, origin: Origin = getOrigin(player)!!, condition: OriginValue = getCondition(player)) {
        reset(player)
        add(player, origin, condition)
    }

    fun refreshSpecial(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        refreshNightVision(player, origin, playerData)
        refreshSlowFalling(player, origin, playerData)
        refreshJump(player, origin, playerData)
    }

    fun refreshNightVision(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val nightVision = playerData.getOriginSetting(Special.NIGHTVISION)
        if(origin.nightVision && nightVision > 0) {
            when(nightVision) {
                1 -> if(WorldTime.isNight(player)) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false)) else player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                2 -> if(!player.hasPotionEffect(PotionEffectType.NIGHT_VISION) || player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.hasIcon()!!) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                else -> TODO()
            }
        }
    }
    fun refreshJump(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val jumpBoost = playerData.getOriginSetting(Special.JUMPBOOST)
        if(origin.jumpBoost > 0 ) {
            if(jumpBoost > 0 && (!player.hasPotionEffect(PotionEffectType.JUMP) || player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, jumpBoost, true, false, false))
            } else if(jumpBoost == 0 && !player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.JUMP)
            }
        }
    }
    fun refreshSlowFalling(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val slowFalling = playerData.getOriginSetting(Special.SLOWFALLING)
        if(origin.slowFalling) {
            if(slowFalling == 1 && (!player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Int.MAX_VALUE, 0, true, false, false))
            } else if(slowFalling == 0 && !player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
    }

    fun refreshTime(player: Player,
        origin: Origin = getOrigin(player)!!) {
        if (player.world.environment == World.Environment.NORMAL) {
            var remove: ArrayList<PotionEffect?>? = null
            var add: ArrayList<PotionEffect?>? = null
            when (WorldTime.isDay(player)) {
                true -> {
                    remove = origin.effectMap[OriginValue.NIGHT]
                    add = origin.effectMap[OriginValue.DAY]
                }
                false -> {
                    remove = origin.effectMap[OriginValue.DAY]
                    add = origin.effectMap[OriginValue.NIGHT]
                }
            }
            remove?.forEach { var1x -> player.removePotionEffect(var1x!!.type) }
            player.addPotionEffects(add!!)
        }
    }
}


class Origin(nameMap: Map<OriginValue, String>,
             soundMap: Map<OriginValue, Sound>,
             timeMessageMap: HashMap<OriginValue, String?>,
             permissionMap: Map<OriginValue, ArrayList<String?>?>,
             effectEnableMap: Map<OriginValue, Boolean>,
             specialMap: Map<OriginValue, Int>,
             effectMap: Map<OriginValue, List<PotionEffect?>?>,
             attributeMap: Map<OriginValue, List<OriginAttribute?>?>,
             damageEnableMap: Map<OriginValue, Boolean>,
             damageAmountMap: Map<OriginValue, Int>,
             guiItem: Map<OriginValue, Any>) {

    val name: String = nameMap[OriginValue.NAME] ?: ""
    val colour: String = nameMap[OriginValue.COLOUR] ?: ""
    val displayName: String = nameMap[OriginValue.DISPLAY_NAME] ?: ""
    val bracketName: String = nameMap[OriginValue.BRACKET_NAME] ?: ""
    val hurtSound: Sound = soundMap[OriginValue.HURT] ?: Sound.ENTITY_PLAYER_HURT
    val deathSound: Sound = soundMap[OriginValue.DEATH] ?: Sound.ENTITY_PLAYER_DEATH
    val dayTitle: String = timeMessageMap[OriginValue.DAY_TITLE] ?: "" // TODO Make component
    val daySubtitle: String = timeMessageMap[OriginValue.DAY_SUBTITLE] ?: "" // TODO Make component
    val nightTitle: String = timeMessageMap[OriginValue.NIGHT_TITLE] ?: "" // TODO Make component
    val nightSubtitle: String = timeMessageMap[OriginValue.NIGHT_SUBTITLE] ?: "" // TODO Make component
    val requiredPermissions = permissionMap[OriginValue.PERMISSION_REQUIRED] ?: emptyList()
    val givenPermissions = permissionMap[OriginValue.PERMISSION_GIVEN] ?: emptyList()
    val enableEffects: Boolean = effectEnableMap[OriginValue.GENERAL] ?: false
    val enableTime: Boolean = effectEnableMap[OriginValue.TIME] ?: false
    val enableLiquid: Boolean = effectEnableMap[OriginValue.LIQUID] ?: false
    val enableDimension: Boolean = effectEnableMap[OriginValue.DIMENSION] ?: false
    val nightVision: Boolean = (specialMap[OriginValue.SPECIAL_NIGHTVISION] ?: 0) == 1
    val slowFalling: Boolean = (specialMap[OriginValue.SPECIAL_SLOWFALLING] ?: 0) == 1
    val jumpBoost: Int = specialMap[OriginValue.SPECIAL_JUMPBOOST] ?: 0
    val enableDamage: Boolean = damageEnableMap[OriginValue.DAMAGE] ?: false
    val enableSun: Boolean = damageEnableMap[OriginValue.SUN] ?: false
    val enableFall: Boolean = damageEnableMap[OriginValue.FALL] ?: false
    val enableRain: Boolean = damageEnableMap[OriginValue.RAIN] ?: false
    val enableWater: Boolean = damageEnableMap[OriginValue.WATER] ?: false
    val enableLava: Boolean = damageEnableMap[OriginValue.LAVA] ?: false
    val sunAmount: Int = damageAmountMap[OriginValue.SUN] ?: 0
    val fallAmount: Int = damageAmountMap[OriginValue.SUN] ?: 100
    val rainAmount: Int = damageAmountMap[OriginValue.SUN] ?: 0
    val waterAmount: Int = damageAmountMap[OriginValue.SUN] ?: 0
    val lavaAmount: Int = damageAmountMap[OriginValue.SUN] ?: 100
    val item: ItemStack = guiItem[OriginValue.ITEM] as ItemStack
    val slot: Int = guiItem[OriginValue.SLOT] as Int
    val effectMap = LinkedHashMap<OriginValue, ArrayList<PotionEffect?>?>()
    val conditionEffectMap = LinkedHashMap<OriginValue, ArrayList<PotionEffect?>?>()
    val attributeMap = LinkedHashMap<OriginValue, ArrayList<OriginAttribute?>?>()
    val conditionAttributeMap = LinkedHashMap<OriginValue, ArrayList<OriginAttribute?>?>()

    init {
        val mapTypes: List<OriginValue> = arrayListOf(OriginValue.OD,
            OriginValue.ON,
            OriginValue.ODW,
            OriginValue.ODL,
            OriginValue.ONW,
            OriginValue.ONL,
            OriginValue.N,
            OriginValue.NL,
            OriginValue.E,
            OriginValue.EW,
            OriginValue.EL)

        for(originValue: OriginValue in mapTypes) {
            val value: String = originValue.toString()
            var dimension: OriginValue? = null
            var time: OriginValue? = null
            var liquid: OriginValue? = null
            if(value.isNotEmpty()) {
                dimension = when (value[0].toString()) {
                    "O" -> OriginValue.OVERWORLD
                    "N" -> OriginValue.NETHER
                    "E" -> OriginValue.END
                    else -> null
                }
            }
            if(value.length > 1) {
                time = when (value[1].toString()) {
                    "D" -> OriginValue.DAY
                    "N" -> OriginValue.NIGHT
                    else -> null
                }
            }
            if(value.length > 2) {
                liquid = when (value[2].toString()) {
                    "W" -> OriginValue.WATER
                    "L" -> OriginValue.LAVA
                    else -> null
                }
            }
            constructPotionMap(originValue, effectMap[OriginValue.GENERAL], effectMap[dimension], effectMap[time], effectMap[liquid])
            constructAttributeMap(originValue, attributeMap[OriginValue.GENERAL], attributeMap[dimension], attributeMap[time], attributeMap[liquid])
        }

        OriginManager.Origins.addToMap(this)
    }

    private fun constructPotionMap(originValue: OriginValue,
        general: List<PotionEffect?>?,
        world: List<PotionEffect?>?,
        time: List<PotionEffect?>?,
        liquid: List<PotionEffect?>?) {

        val list: ArrayList<PotionEffect?> = ArrayList()
        for (potion in PotionEffectType.values()) {
            var finalPotion = PotionEffect(potion, 1, -1)
            finalPotion = potionLoop(potion, general, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, world, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, time, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, liquid, finalPotion) ?: finalPotion
            if (!(finalPotion.duration == 1 && finalPotion.amplifier == -1)) {
                list.add(finalPotion)
            }
        }
        conditionEffectMap[originValue] = list
    }

    private fun constructAttributeMap(
        originValue: OriginValue,
        general: List<OriginAttribute?>?,
        world: List<OriginAttribute?>?,
        time: List<OriginAttribute?>?,
        liquid: List<OriginAttribute?>?) {

        val list: ArrayList<OriginAttribute?> = ArrayList()
        for (attribute in Attribute.values()) {
            var finalAttribute: OriginAttribute? =
                OriginAttribute(Attribute.GENERIC_FLYING_SPEED, 0.842)
            finalAttribute = attributeLoop(attribute, general, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, world, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, time, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, liquid, finalAttribute) ?: finalAttribute
            if (!(finalAttribute?.attribute == Attribute.GENERIC_FLYING_SPEED && finalAttribute.value == 0.842)) {
                list.add(finalAttribute)
            }
        }
        conditionAttributeMap[originValue] = list
    }

    private fun potionLoop(
        potionType: PotionEffectType?,
        potionEffects: List<PotionEffect?>?,
        finalPotion: PotionEffect?): PotionEffect? {

        for (potion in ListUtils.emptyIfNull(potionEffects)) {
            if (potion != null && potion.type === potionType && finalPotion!!.amplifier < potion.amplifier) {
                return potion
            }
        }
        return null
    }

    private fun attributeLoop(
        attribute: Attribute?,
        attributes: List<OriginAttribute?>?,
        finalAttribute: OriginAttribute?): OriginAttribute? {

        for (var1: OriginAttribute? in ListUtils.emptyIfNull(attributes)) {
            if (var1 != null && var1.attribute == attribute && finalAttribute!!.value < var1.value) {
                return var1
            }
        }
        return null
    }


}

enum class OriginValue {
    NAME,
    COLOUR,
    DISPLAY_NAME,
    BRACKET_NAME,
    HURT, DEATH,
    DAY_TITLE,
    DAY_SUBTITLE,
    NIGHT_TITLE,
    NIGHT_SUBTITLE,
    PERMISSION_REQUIRED,
    PERMISSION_GIVEN,
    GENERAL,
    TIME,
    LIQUID,
    DIMENSION,
    DAY,
    NIGHT,
    WATER,
    LAVA,
    OVERWORLD,
    NETHER,
    END,
    SPECIAL_SLOWFALLING,
    SPECIAL_NIGHTVISION,
    SPECIAL_JUMPBOOST,
    DAMAGE,
    SUN,
    FALL,
    RAIN,
    ITEM,
    SLOT,
    OD,
    ON,
    ODW,
    ODL,
    ONW,
    ONL,
    N,
    NL,
    E,
    EW,
    EL;
}