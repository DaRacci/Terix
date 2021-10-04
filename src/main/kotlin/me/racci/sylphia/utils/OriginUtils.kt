package me.racci.sylphia.utils

import me.racci.raccicore.utils.worlds.WorldTime.isDay
import me.racci.sylphia.enums.Condition
import me.racci.sylphia.enums.Condition.*
import org.apache.commons.lang3.EnumUtils
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.Attribute.*
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType


fun getConditions(player: Player) : Array<Condition> {
    return arrayOf(
        when(player.world.environment.ordinal) {2 -> NETHER;3 -> END;else -> OVERWORLD},
        if(player.world.environment.ordinal == 1) when(isDay(player)) {true -> DAY;false -> NIGHT} else NULL,
        if(player.isInWaterOrBubbleColumn) WATER else if(player.isInLava) LAVA else NULL
    )
}

object PotionUtils {

    private val potionEffectTypes = HashSet<PotionEffectType>()

    fun isValid(string: String): Boolean {
        return EnumUtils.isValidEnum(PrivatePotionEffectType::class.java, string)
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

    fun isValid(string: String?): Boolean {
        return EnumUtils.isValidEnum(Attribute::class.java, string)
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