@file:Suppress("unused")
@file:JvmName("OriginUtils")
package me.racci.sylphia.utils

import me.racci.raccilib.utils.OriginConditionException
import me.racci.raccilib.utils.worlds.WorldTime
import me.racci.sylphia.extensions.PlayerExtension.isDay
import me.racci.sylphia.origins.AttributeModifier
import me.racci.sylphia.origins.BaseAttribute
import me.racci.sylphia.origins.OriginAttribute
import me.racci.sylphia.origins.OriginValue
import org.apache.commons.lang3.EnumUtils
import org.bukkit.World.Environment.*
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.Attribute.*
import org.bukkit.attribute.AttributeInstance
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

fun getCondition(player: Player): OriginValue {
    val dimensionCondition: String = when(player.world.environment) {
        NORMAL -> "O"
        NETHER -> "N"
        THE_END -> "E"
        else -> throw OriginConditionException("Unexpected value: " + player.world.environment)
    }
    val timeCondition: String = if(dimensionCondition == "O") when(WorldTime.isDay(player)) {
        true -> "D"
        false -> "N"
    } else ""
    val liquidCondition: String = if(player.isInWaterOrBubbleColumn) "W" else if(player.isInLava) "L" else ""
    return OriginValue.valueOf("$dimensionCondition$timeCondition$liquidCondition")
}
fun getConditionSet(player: Player): HashSet<OriginValue> {
    val condition = HashSet<OriginValue>()
    val var1 = player.world.environment
    condition.add(when(var1) {
        NORMAL -> OriginValue.OVERWORLD
        NETHER -> OriginValue.NETHER
        THE_END -> OriginValue.END
        else -> throw OriginConditionException("Unexpected value: " + player.world.environment)
    })
    if(var1 == NORMAL) {
        condition.add(when(player.isDay) {
            true -> OriginValue.DAY
            false -> OriginValue.NIGHT
        })
    }
    if(player.isInWaterOrBubbleColumn) condition.add(OriginValue.WATER)
        else if(player.isInLava) condition.add(OriginValue.LAVA)
    return condition
}

object PotionUtils {

    private val potionEffectTypes = HashSet<PotionEffectType>()

    fun createPotion(string: String): PotionEffect? {
        val potion = string.split(":".toRegex()).toTypedArray()
        if ((potion[1].toIntOrNull() ?: return null) !in 0..if(isValid(potion[0])) PrivatePotionEffectType.valueOf(potion[0]).maxLevel else return null) return null
        return PotionEffect(PotionEffectType.getByName(potion[0])!!, Int.MAX_VALUE, potion[1].toInt(), true, false, false)
    }

    private fun isValid(string: String): Boolean {
        return EnumUtils.isValidEnum(PrivatePotionEffectType::class.java, string)
    }

    fun isInfinite(potion: PotionEffect): Boolean {
        return potion.duration >= 86400
    }

    fun isOriginEffect(potion: PotionEffect): Boolean {
        return !potion.hasIcon() || potion.isAmbient || potion.duration >= 86400
    }

    val potionTypes: HashSet<PotionEffectType> = potionEffectTypes

    fun getHigherPotion(var1: PotionEffect, var2: PotionEffect): PotionEffect {
        return if (var1.amplifier > var2.amplifier) var1 else var2
    }

    fun getLowerPotion(var1: PotionEffect, var2: PotionEffect): PotionEffect {
        return if (var1.amplifier < var2.amplifier) var1 else var2
    }

    init {
        for (potion in PrivatePotionEffectType.values()) {
            potionEffectTypes.add(potion.potionEffectType)
        }
    }
}

object AttributeUtils {

    private val playerAttributes = ArrayList<Attribute>()

    fun createModifier(condition: OriginValue, string: String) : AttributeModifier? {
        val attribute = string.split(":".toRegex()).toTypedArray()
        val var1 = if(isValid(attribute[0])) PrivateAttribute.valueOf(attribute[0]) else return null
        if ((attribute[1].toDoubleOrNull() ?: return null) !in var1.minLevel..var1.maxLevel) return null
        return AttributeModifier(Attribute.valueOf(attribute[0]),
            org.bukkit.attribute.AttributeModifier(condition.toString(), attribute[1].toDouble(), org.bukkit.attribute.AttributeModifier.Operation.ADD_SCALAR))
    }

    fun createBaseAttribute(string: String) : BaseAttribute? {
        val attribute = string.split(":".toRegex()).toTypedArray()
        val var1 = if(isValid(attribute[0])) PrivateAttribute.valueOf(attribute[0]) else return null
        if ((attribute[1].toDoubleOrNull() ?: return null) !in var1.minLevel..var1.maxLevel) return null
        return BaseAttribute(Attribute.valueOf(attribute[0]), attribute[1].toDouble())
    }

    fun createAttribute(string: String): OriginAttribute? {
        val attribute = string.split(":".toRegex()).toTypedArray()
        val var1 = if(isValid(attribute[0])) PrivateAttribute.valueOf(attribute[0]) else return null
        if ((attribute[1].toDoubleOrNull() ?: return null) !in var1.minLevel..var1.maxLevel) return null
        return OriginAttribute(Attribute.valueOf(attribute[0]), attribute[1].toDouble())
    }

    private fun isValid(string: String?): Boolean {
        return EnumUtils.isValidEnum(Attribute::class.java, string)
    }

    fun isDefault(attribute: AttributeInstance): Boolean {
        return getDefault(attribute.attribute) == attribute.baseValue
    }

    fun isDefault(attribute: OriginAttribute): Boolean {
        return getDefault(attribute.attribute) == attribute.value
    }

    fun getDefault(attribute: Attribute): Double {
        return when(attribute) {
            GENERIC_MAX_HEALTH -> 20.0
            GENERIC_MOVEMENT_SPEED -> 0.1
            GENERIC_ATTACK_DAMAGE -> 2.0
            GENERIC_ATTACK_SPEED -> 4.0
            else -> 0.0
        }
    }

    fun getPlayerAttributes(): List<Attribute> {
        return playerAttributes
    }

    fun getHigherAttribute(var1: AttributeInstance, var2: AttributeInstance): AttributeInstance {
        return if (var1.baseValue > var2.baseValue) var1 else var2
    }

    fun getLowerAttribute(var1: AttributeInstance, var2: AttributeInstance): AttributeInstance {
        return if (var1.baseValue < var2.baseValue) var1 else var2
    }

    init {
        for (attribute: PrivateAttribute in PrivateAttribute.values()) {
            playerAttributes.add(attribute.attribute)
        }
    }
}

internal enum class PrivatePotionEffectType(
    val cleanName: String,
    val maxLevel: Int,
    val potionEffectType: PotionEffectType
) {
    ABSORPTION("Absorption", 100, PotionEffectType.ABSORPTION),
    BAD_OMEN("Bad Omen", 1, PotionEffectType.BAD_OMEN),
    BLINDNESS("Blindness", 6, PotionEffectType.BLINDNESS),
    CONDUIT_POWER("Conduit Power", 6, PotionEffectType.CONDUIT_POWER),
    CONFUSION("Nausea", 1, PotionEffectType.CONFUSION),
    DAMAGE_RESISTANCE("Resistance", 6, PotionEffectType.DAMAGE_RESISTANCE),
    DOLPHINS_GRACE("Dolphins Grace", 1, PotionEffectType.DOLPHINS_GRACE),
    FAST_DIGGING("Haste", 6, PotionEffectType.FAST_DIGGING),
    FIRE_RESISTANCE("Fire Resistance", 1, PotionEffectType.FIRE_RESISTANCE),
    GLOWING("Glowing", 1, PotionEffectType.GLOWING),
    HARM("Instant Damage", 4, PotionEffectType.HARM),
    HEAL("Instant Health", 4, PotionEffectType.HEAL),
    HEALTH_BOOST("Health Boost", 100, PotionEffectType.HEALTH_BOOST),
    HERO_OF_THE_VILLAGE("Hero of the Village", 4, PotionEffectType.HERO_OF_THE_VILLAGE),
    HUNGER("Hunger", 4, PotionEffectType.HUNGER),
    INCREASE_DAMAGE("Strength", 12, PotionEffectType.INCREASE_DAMAGE),
    INVISIBILITY("Invisibility", 1, PotionEffectType.INVISIBILITY),
    JUMP("Jump Boost", 12, PotionEffectType.JUMP),
    LEVITATION("Levitation", 1, PotionEffectType.LEVITATION),
    LUCK("Luck", 100, PotionEffectType.LUCK),
    NIGHT_VISION("Night Vision", 1, PotionEffectType.NIGHT_VISION),
    POISON("Poison", 12, PotionEffectType.POISON),
    REGENERATION("Regeneration", 12, PotionEffectType.REGENERATION),
    SATURATION("Saturation", 4, PotionEffectType.SATURATION),
    SLOW("Slowness", 4, PotionEffectType.SLOW),
    SLOW_DIGGING("Mining Fatigue", 4, PotionEffectType.SLOW_DIGGING),
    SLOW_FALLING("Slow Falling", 1, PotionEffectType.SLOW_FALLING),
    SPEED("Speed", 12, PotionEffectType.SPEED),
    UNLUCK("????", 100, PotionEffectType.UNLUCK),
    WATER_BREATHING("Water Breathing", 1, PotionEffectType.WATER_BREATHING),
    WEAKNESS("Weakness", 12, PotionEffectType.WEAKNESS),
    WITHER("Wither", 12, PotionEffectType.WITHER);
}

internal enum class PrivateAttribute(
    val defaultLevel: Double,
    val minLevel: Double,
    val maxLevel: Double,
    val attribute: Attribute
) {
    GENERIC_MAX_HEALTH(20.0, 0.0, 1024.0, Attribute.GENERIC_MAX_HEALTH),
    GENERIC_MOVEMENT_SPEED(0.1, 0.0, 1024.0, Attribute.GENERIC_MOVEMENT_SPEED),
    GENERIC_ATTACK_DAMAGE(2.0, 0.0, 2048.0, Attribute.GENERIC_ATTACK_DAMAGE),
    GENERIC_ATTACK_SPEED(4.0,0.0, 1024.0,Attribute.GENERIC_ATTACK_SPEED),
    GENERIC_KNOCKBACK_RESISTANCE(0.0, 0.0, 1.0, Attribute.GENERIC_KNOCKBACK_RESISTANCE),
    GENERIC_ATTACK_KNOCKBACK(0.0,0.0,5.0,Attribute.GENERIC_ATTACK_KNOCKBACK),
    GENERIC_ARMOR_TOUGHNESS(0.0, 0.0, 20.0, Attribute.GENERIC_ARMOR_TOUGHNESS),
    GENERIC_ARMOR(0.0,0.0,30.0,Attribute.GENERIC_ARMOR),
    GENERIC_LUCK(0.0, -1024.0, 1024.0, Attribute.GENERIC_LUCK);
}