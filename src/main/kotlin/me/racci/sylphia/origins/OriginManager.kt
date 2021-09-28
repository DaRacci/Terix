package me.racci.sylphia.origins


import me.racci.raccicore.skedule.SynchronizationContext
import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.skedule.skeduleSync
import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.enums.Special
import me.racci.sylphia.origins.OriginHandler.OriginsMap.origins
import me.racci.sylphia.origins.OriginManager.Companion.addToMap
import me.racci.sylphia.origins.OriginValue.*
import me.racci.sylphia.utils.AttributeUtils
import me.racci.sylphia.utils.AttributeUtils.getPlayerAttributes
import me.racci.sylphia.utils.PotionUtils
import me.racci.sylphia.utils.getCondition
import me.racci.sylphia.utils.getConditionSet
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.World.Environment.NORMAL
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.*


/**
 * Origin manager
 *
 * @property plugin
 * @constructor Create empty Origin manager
 */
class OriginManager(private val plugin: Sylphia) {

    private val playerManager = plugin.playerManager
    private val worldManager = plugin.worldManager

    companion object {
        private val originMap: MutableMap<String, Origin> = LinkedHashMap()
        fun addToMap(origin: Origin) {
            originMap.putIfAbsent(origin.name.uppercase(), origin)
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


    /**
     * Resets all the players attributes and removes all modifiers,
     * Then sets the base attributes for the players origin
     *
     * Thread Safe
     *
     * @param player The Target Player
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun setBaseAttributes(player: Player,
                          origin: Origin? = getOrigin(player.uniqueId)
                          ) {
        for(attribute in getPlayerAttributes()) {
            val instance = player.getAttribute(attribute)
            instance?.modifiers?.forEach {instance.removeModifier(it)}
            instance?.baseValue = AttributeUtils.getDefault(attribute)
        }
        for(attribute in origin?.baseAttributes!!) {
            player.getAttribute(attribute.attribute)?.baseValue = attribute.double
        }
    }

    /**
     * Adds the attribute modifiers for the given modifier condition
     * If there is none this will do nothing.
     *
     * Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to apply from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun addAttributeModifiers(player: Player,
                              modifier: OriginValue,
                              origin: Origin? = getOrigin(player.uniqueId),
                             ) {
        for(attribute in origin?.attributeModifiers?.get(modifier)!!) {
            player.getAttribute(attribute.attribute)?.addModifier(attribute.modifier)
        }
    }
    /**
     * Removes the attribute modifiers for the given modifier condition
     * If there is none or the player doesn't have the modifier this does nothing.
     *
     * Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to remove from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun removeAttributeModifiers(player: Player,
                                 modifier: OriginValue,
                                 origin: Origin? = getOrigin(player.uniqueId),
                                ) {
        for(attribute in origin?.attributeModifiers?.get(modifier)!!) {
            player.getAttribute(attribute.attribute)?.removeModifier(attribute.modifier)
        }
    }

    /**
     * Adds the potions for the given modifier condition
     * If there are none this does nothing
     *
     * Not Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to add from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun addPotions(player: Player,
                  modifier: OriginValue,
                  origin: Origin? = getOrigin(player.uniqueId)
                  ) {
        player.addPotionEffects(origin?.effectModifiers?.get(modifier)!!)
    }
    /**
     * Removes the potions for the given modifier condition
     * If there are none or the players doesn't have the potion,
     * This does nothing.
     * This will also check for if the potion is within the conditions
     * of what is defined as an origin potion via [PotionUtils.isOriginEffect]
     *
     * Not Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to remove from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun removePotions(player: Player,
                   modifier: OriginValue,
                   origin: Origin? = getOrigin(player.uniqueId)
                   ) {
        origin?.effectModifiers?.get(modifier)?.stream()?.map(PotionEffect::getType)?.forEach {
            if(PotionUtils.isOriginEffect(player.getPotionEffect(it) ?: return@forEach)) player.removePotionEffect(it)
        }
    }

    /**
     * Runs [addPotions] and [addAttributeModifiers]
     * This is run as a new Async instance for [addAttributeModifiers]
     * then swaps to the Bukkit thread for [addPotions]
     *
     * Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to add from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun addPassives(player: Player,
                    modifier: OriginValue,
                    origin: Origin? = getOrigin(player.uniqueId)
                    ) {
        skeduleAsync(plugin) {
            addAttributeModifiers(player, modifier, origin)
            switchContext(SynchronizationContext.SYNC)
            addPotions(player, modifier, origin)
        }
    }
    /**
     * Runs [removePotions] and [removeAttributeModifiers]
     * This is run as a new Async instance for [removeAttributeModifiers]
     * then swaps to the Bukkit thread for [removePotions]
     *
     * Thread Safe
     *
     * @param player The Target Player
     * @param modifier The Target Condition to remove from
     * @param origin The Target Players Origin, Will be fetched if not supplied.
     */
    fun removePassives(player: Player,
                       modifier: OriginValue,
                       origin: Origin? = getOrigin(player.uniqueId)
                       ) {
        skeduleAsync(plugin) {
            removeAttributeModifiers(player, modifier, origin)
            switchContext(SynchronizationContext.SYNC)
            removePotions(player, modifier, origin)
        }
    }

    /**
     * Removes all Origin potions defined by [PotionUtils.isOriginEffect]
     * Resets all player attributes to the default value, and removes all modifiers
     * This is run as a new Async instance attributes and then swaps to the
     * Bukkit thread for removing potions.
     *
     * Thread Safe
     *
     * @param player The Target Player
     */
    fun removeAll(player: Player) {
        skeduleAsync(plugin) {
            for(attribute in getPlayerAttributes()) {
                val instance = player.getAttribute(attribute)
                instance?.modifiers?.forEach {instance.removeModifier(it)}
                instance?.baseValue = AttributeUtils.getDefault(attribute)
            }
            switchContext(SynchronizationContext.SYNC)
            player.activePotionEffects.filter(PotionUtils::isOriginEffect)
                .map(PotionEffect::getType).forEach {player.removePotionEffect(it)}
        }
    }

    fun addAll(player: Player,
               origin: Origin? = getOrigin(player),
               conditions: HashSet<OriginValue> = getConditionSet(player),
               ) {
        skeduleAsync(plugin) {
            setBaseAttributes(player, origin)
            val var1 = origin?.baseEffects.orEmpty()
            conditions.forEach {
                addAttributeModifiers(player, it, origin)
                var1.plus(origin?.effectModifiers?.get(it)!!)
            }
            switchContext(SynchronizationContext.SYNC)
            player.addPotionEffects(var1)

        }
    }
    fun refreshAll(player: Player,
                   origin: Origin? = getOrigin(player)
                   ) {
        removeAll(player)
        addAll(player, origin)
    }

    fun add(player: Player, origin: Origin = getOrigin(player)!!, condition: OriginValue = getCondition(player)) {
        origin.conditionAttributeMap[condition]?.forEach { var1x ->
            player.getAttribute(var1x.attribute)!!.baseValue = var1x.value
        }
        if(!Bukkit.isPrimaryThread()) {
            skeduleSync(plugin) {player.addPotionEffects(origin.conditionEffectMap[condition]!!)}
        } else player.addPotionEffects(origin.conditionEffectMap[condition]!!)
    }
    fun refresh(player: Player, origin: Origin = getOrigin(player)!!, condition: OriginValue = getCondition(player)) {
        removeAll(player)
        add(player, origin, condition)
    }

    fun refreshSpecial(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        refreshNightVision(player, origin, playerData)
        refreshSlowFalling(player, origin, playerData)
        refreshJump(player, origin, playerData)
    }

    private fun refreshNightVision(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val nightVision = playerData.getOriginSetting(Special.NIGHTVISION)
        if(origin.nightVision && nightVision > 0) {
            when(nightVision) {
                1 -> if(WorldTime.isNight(player)) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false)) else player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                2 -> if(!player.hasPotionEffect(PotionEffectType.NIGHT_VISION) || player.getPotionEffect(PotionEffectType.NIGHT_VISION)?.hasIcon()!!) player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                else -> TODO()
            }
        }
    }
    private fun refreshJump(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val jumpBoost = playerData.getOriginSetting(Special.JUMPBOOST)
        if(origin.jumpBoost > 0 ) {
            if(jumpBoost > 0 && (!player.hasPotionEffect(PotionEffectType.JUMP) || player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, jumpBoost, true, false, false))
            } else if(jumpBoost == 0 && !player.getPotionEffect(PotionEffectType.JUMP)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.JUMP)
            }
        }
    }
    private fun refreshSlowFalling(player: Player, origin: Origin = getOrigin(player)!!, playerData: PlayerData = playerManager.getPlayerData(player.uniqueId)!!) {
        val slowFalling = playerData.getOriginSetting(Special.SLOWFALLING)
        if(origin.slowFalling) {
            if(slowFalling == 1 && (!player.hasPotionEffect(PotionEffectType.SLOW_FALLING) || player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!)) {
                player.addPotionEffect(PotionEffect(PotionEffectType.SLOW_FALLING, Int.MAX_VALUE, 0, true, false, false))
            } else if(slowFalling == 0 && !player.getPotionEffect(PotionEffectType.SLOW_FALLING)?.hasIcon()!!) {
                player.removePotionEffect(PotionEffectType.SLOW_FALLING)
            }
        }
    }

    private fun refreshAttributes(player: Player, origin: Origin = getOrigin(player)!!, condition: OriginValue = getCondition(player)) {
        skeduleAsync(plugin) {
            for (attribute in getPlayerAttributes()) {
                player.getAttribute(attribute)?.baseValue = AttributeUtils.getDefault(attribute)
            }
            origin.conditionAttributeMap[condition]?.forEach {
                player.getAttribute(it.attribute)!!.baseValue = it.value
            }

        }
    }

    // TODO combined into vals and one final function
    fun refreshTime(player: Player, origin: Origin = getOrigin(player)!!) {
        if(origin.enableTime) {
            val world = player.world
            if(world.environment == NORMAL && !worldManager.isDisabledWorld(world)) {
                when(WorldTime.isDay(player)) {
                    true -> {
                        origin.nightEffects.map(PotionEffect::getType).forEach { if(player.hasPotionEffect(it)
                            && PotionUtils.isOriginEffect(player.getPotionEffect(it)!!)) {
                            player.removePotionEffect(it)
                        } }
                        player.addPotionEffects(origin.dayEffects.toMutableList())
                        origin.attributeModifiers[NIGHT]?.forEach {player.getAttribute(it.attribute)?.removeModifier(it.modifier)}
                        origin.attributeModifiers[DAY]?.forEach {player.getAttribute(it.attribute)?.addModifier(it.modifier)}
                    }
                    false -> {
                        origin.dayEffects.map(PotionEffect::getType).forEach { if(player.hasPotionEffect(it)
                            && PotionUtils.isOriginEffect(player.getPotionEffect(it)!!)) {
                            player.removePotionEffect(it)
                        } }
                        player.addPotionEffects(origin.nightEffects.toMutableList())
                        origin.attributeModifiers[DAY]?.forEach {player.getAttribute(it.attribute)?.removeModifier(it.modifier)}
                        origin.attributeModifiers[NIGHT]?.forEach {player.getAttribute(it.attribute)?.addModifier(it.modifier)}
                        }
                    }
                }
            }
        }
}


class Origin(
    nameMap: LinkedHashMap<OriginValue, String>,
    soundMap: LinkedHashMap<OriginValue, Sound>,
    timeMessageMap: LinkedHashMap<OriginValue, String?>,
    permissionMap: LinkedHashMap<OriginValue, Any?>,
    passiveEnableMap: LinkedHashMap<OriginValue, Boolean>,
    attributeBase: HashMap<OriginValue, HashSet<BaseAttribute>>,
    attributeModifier: HashMap<OriginValue, HashSet<AttributeModifier>>,
    specialMap: HashMap<OriginValue, Any?>,
    effectMap: LinkedHashMap<OriginValue, HashSet<PotionEffect>>,
    attributeMap: LinkedHashMap<OriginValue, HashSet<OriginAttribute>>,
    damageEnableMap: LinkedHashMap<OriginValue, Boolean>,
    damageAmountMap: LinkedHashMap<OriginValue, Int>,
    guiItem: LinkedHashMap<OriginValue, Any?>) {

    private var var1: Any? = null
    val name = nameMap[NAME]!!
    val colour = nameMap[COLOUR]!!
    val displayName = nameMap[DISPLAY_NAME]!!
    val bracketName = nameMap[BRACKET_NAME]!!
    val hurtSound = soundMap[HURT]!!
    val deathSound = soundMap[DEATH]!!
    var dayTitle = Component.empty()
    var daySubtitle = Component.empty()
    var nightTitle = Component.empty()
    var nightSubtitle = Component.empty()
    val requiredPermissions = (permissionMap[PERMISSION_REQUIRED] as String?).orEmpty()
    val givenPermissions = (permissionMap[PERMISSION_GIVEN] as HashSet<String>?).orEmpty()
    val enableEffects: Boolean = passiveEnableMap[GENERAL] ?: false
    val enableTime: Boolean = passiveEnableMap[TIME] ?: false
    val enableLiquid: Boolean = passiveEnableMap[LIQUID] ?: false
    val enableDimension: Boolean = passiveEnableMap[DIMENSION] ?: false
    val nightVision: Boolean = specialMap[TOGGLE_NIGHTVISION] as Boolean? ?: false
    val slowFalling: Boolean = specialMap[TOGGLE_SLOWFALLING] as Boolean? ?: false
    val jumpBoost: Int = specialMap[TOGGLE_JUMPBOOST] as Int? ?: 0
    val enableSun: Boolean = damageEnableMap[SUN] ?: false
    val enableFall: Boolean = damageEnableMap[FALL] ?: false
    val enableRain: Boolean = damageEnableMap[RAIN] ?: false
    val enableWater: Boolean = damageEnableMap[WATER] ?: false
    val enableLava: Boolean = damageEnableMap[LAVA] ?: false
    val sunAmount: Int = damageAmountMap[SUN] ?: 0
    val fallAmount: Int = damageAmountMap[FALL] ?: 100
    val rainAmount: Int = damageAmountMap[RAIN] ?: 0
    val waterAmount: Int = damageAmountMap[WATER] ?: 0
    val lavaAmount: Int = damageAmountMap[LAVA] ?: 100
    val item: ItemStack = guiItem[ITEM] as ItemStack
    val slot: Int = guiItem[SLOT] as Int? ?: 10

    val conditionEffectMap = LinkedHashMap<OriginValue, HashSet<PotionEffect>>()
    val conditionAttributeMap = LinkedHashMap<OriginValue, HashSet<OriginAttribute>>()

    val dayEffects = effectMap[DAY].orEmpty().toHashSet()
    val nightEffects = effectMap[NIGHT].orEmpty().toHashSet()

    val baseEffects = effectMap[GENERAL].orEmpty().toHashSet()
    val effectModifiers = effectMap

    val baseAttributes = attributeBase[GENERAL].orEmpty().toHashSet()
    val attributeModifiers = attributeModifier

    init {
        val mapTypes: List<OriginValue> =
            arrayListOf(
                OD, ON, ODW, ODL,
                ONW, ONL, N, NL,
            E, EW, EL
            )

        var1 = timeMessageMap[DAY_TITLE]
        if(!(var1 as String?).isNullOrEmpty()) {
            dayTitle = Component.text(var1 as String)
        }
        var1 = timeMessageMap[DAY_SUBTITLE]
        if(!(var1 as String?).isNullOrEmpty()) {
            daySubtitle = Component.text(var1 as String)
        }
        var1 = timeMessageMap[NIGHT_TITLE]
        if(!(var1 as String?).isNullOrEmpty()) {
            nightTitle = Component.text(var1 as String)
        }
        var1 = timeMessageMap[NIGHT_SUBTITLE]
        if(!(var1 as String?).isNullOrEmpty()) {
            nightTitle = Component.text(var1 as String)
        }

        for(originValue: OriginValue in mapTypes) {
            val value: String = originValue.toString()
            var dimension: OriginValue? = null
            var time: OriginValue? = null
            var liquid: OriginValue? = null
            if(value.isNotEmpty()) {
                dimension = when (value[0].toString()) {
                    "O" -> OVERWORLD
                    "N" -> NETHER
                    "E" -> END
                    else -> null
                }
            }
            if(value.length > 1) {
                time = when (value[1].toString()) {
                    "D" -> DAY
                    "N" -> NIGHT
                    else -> null
                }
            }
            if(value.length > 2) {
                liquid = when (value[2].toString()) {
                    "W" -> WATER
                    "L" -> LAVA
                    else -> null
                }
            }
            constructPotionMap(originValue, effectMap[GENERAL], effectMap[dimension], effectMap[time], effectMap[liquid])
            constructAttributeMap(originValue, attributeMap[GENERAL], attributeMap[dimension], attributeMap[time], attributeMap[liquid])
        }

        addToMap(this)
    }

    private fun constructPotionMap(originValue: OriginValue,
        general: HashSet<PotionEffect>?,
        world: HashSet<PotionEffect>?,
        time: HashSet<PotionEffect>?,
        liquid: HashSet<PotionEffect>?) {

        val set = HashSet<PotionEffect>()
        for (potion in PotionEffectType.values()) {
            var finalPotion = PotionEffect(potion, 1, -1)
            finalPotion = potionLoop(potion, general, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, world, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, time, finalPotion) ?: finalPotion
            finalPotion = potionLoop(potion, liquid, finalPotion) ?: finalPotion
            if (!(finalPotion.duration == 1 && finalPotion.amplifier == -1)) {
                set.add(finalPotion)
            }
        }
        conditionEffectMap[originValue] = set
    }

    private fun constructAttributeMap(
        originValue: OriginValue,
        general: HashSet<OriginAttribute>?,
        world: HashSet<OriginAttribute>?,
        time: HashSet<OriginAttribute>?,
        liquid: HashSet<OriginAttribute>?) {

        val set = HashSet<OriginAttribute>()
        for (attribute in Attribute.values()) {
            var finalAttribute =
                OriginAttribute(Attribute.GENERIC_FLYING_SPEED, 0.842)
            finalAttribute = attributeLoop(attribute, general, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, world, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, time, finalAttribute) ?: finalAttribute
            finalAttribute = attributeLoop(attribute, liquid, finalAttribute) ?: finalAttribute
            if (!(finalAttribute.attribute == Attribute.GENERIC_FLYING_SPEED && finalAttribute.value == 0.842)) {
                set.add(finalAttribute)
            }
        }
        conditionAttributeMap[originValue] = set
    }

    private fun potionLoop(
        potionType: PotionEffectType?,
        potionEffects: HashSet<PotionEffect>?,
        finalPotion: PotionEffect?): PotionEffect? {

        for (potion in potionEffects.orEmpty()) {
            if (potion.type === potionType && finalPotion!!.amplifier < potion.amplifier) {
                return potion
            }
        }
        return null
    }

    private fun attributeLoop(
        attribute: Attribute?,
        attributes: HashSet<OriginAttribute>?,
        finalAttribute: OriginAttribute?): OriginAttribute? {

        for (var1 in attributes.orEmpty()) {
            if (var1.attribute == attribute && finalAttribute!!.value < var1.value) {
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

    HURT,
    DEATH,

    DAY_TITLE,
    DAY_SUBTITLE,
    DAY_SOUND,

    NIGHT_TITLE,
    NIGHT_SUBTITLE,
    NIGHT_SOUND,

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

    TOGGLE_SLOWFALLING,
    TOGGLE_NIGHTVISION,
    TOGGLE_JUMPBOOST,

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