//package me.racci.sylphia.origins.objects
//
//import org.bukkit.potion.PotionEffect
//import me.racci.sylphia.origins.objects.OriginAttribute
//import org.bukkit.potion.PotionEffectType
//import org.apache.commons.collections4.ListUtils
//import org.bukkit.inventory.ItemStack
//import me.racci.sylphia.origins.objects.OriginKotlin
//import org.bukkit.Sound
//import org.bukkit.attribute.Attribute
//import java.lang.IllegalArgumentException
//import java.util.*
//import java.util.function.IntFunction
//
//class OriginKotlin(
//    nameMap: Map<OriginValue, String>?,
//    soundMap: Map<OriginValue, Sound>?,
//    timeMessageMap: Map<OriginValue, String>?,
//    permissionMap: Map<OriginValue, List<String>>?,
//    effectEnableMap: Map<OriginValue, Boolean>?,
//    specialMap: Map<OriginValue, Int>?,
//    effectMap: Map<OriginValue, List<PotionEffect>>,
//    attributeMap: Map<OriginValue, List<OriginAttribute>>,
//    damageEnableMap: Map<OriginValue, Boolean>?,
//    damageAmountMap: Map<OriginValue, Int>?,
//    GUIItem: Map<OriginValue, Any>?
//) {
//    private val nameMap = LinkedHashMap<OriginValue, String>()
//    private val soundMap = LinkedHashMap<OriginValue, Sound>()
//    private val timeMessageMap = LinkedHashMap<OriginValue, String>()
//    private val permissionMap = LinkedHashMap<OriginValue, List<String>>()
//    private val effectEnableMap = LinkedHashMap<OriginValue, Boolean>()
//    private val specialMap = LinkedHashMap<OriginValue, Int>()
//    private val effectMap = LinkedHashMap<OriginValue, List<PotionEffect>>()
//    private val conditionEffectMap = LinkedHashMap<OriginValue, List<PotionEffect?>>()
//    private val attributeMap = LinkedHashMap<OriginValue, List<OriginAttribute>>()
//    private val conditionAttributeMap = LinkedHashMap<OriginValue, List<OriginAttribute?>>()
//    private val damageEnableMap = LinkedHashMap<OriginValue, Boolean>()
//    private val damageAmountMap = LinkedHashMap<OriginValue, Int>()
//    private val GUIItem = LinkedHashMap<OriginValue, Any>()
//    private fun constructPotionMap(
//        originValue: OriginValue,
//        general: List<PotionEffect>,
//        world: List<PotionEffect>,
//        time: List<PotionEffect>?,
//        liquid: List<PotionEffect>?
//    ) {
//        val list: MutableList<PotionEffect?> = ArrayList()
//        for (potion in PotionEffectType.values()) {
//            var finalPotion: PotionEffect? = PotionEffect(Objects.requireNonNull(potion), 1, -1)
//            finalPotion = Objects.requireNonNullElse(potionLoop(potion, general, finalPotion), finalPotion)
//            finalPotion = Objects.requireNonNullElse(potionLoop(potion, world, finalPotion), finalPotion)
//            finalPotion = Objects.requireNonNullElse(potionLoop(potion, time, finalPotion), finalPotion)
//            finalPotion = Objects.requireNonNullElse(potionLoop(potion, liquid, finalPotion), finalPotion)
//            if (finalPotion != null && !(finalPotion.duration == 1 && finalPotion.amplifier == -1)) {
//                list.add(finalPotion)
//            }
//        }
//        conditionEffectMap[originValue] = list
//    }
//
//    private fun constructAttributeMap(
//        originValue: OriginValue,
//        general: List<OriginAttribute>,
//        world: List<OriginAttribute>,
//        time: List<OriginAttribute>?,
//        liquid: List<OriginAttribute>?
//    ) {
//        val list: MutableList<OriginAttribute?> = ArrayList()
//        for (attribute in Attribute.values()) {
//            var finalAttribute: OriginAttribute? = OriginAttribute(Attribute.GENERIC_FLYING_SPEED, 0.842)
//            finalAttribute =
//                Objects.requireNonNullElse(attributeLoop(attribute, general, finalAttribute), finalAttribute)
//            finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, world, finalAttribute), finalAttribute)
//            finalAttribute = Objects.requireNonNullElse(attributeLoop(attribute, time, finalAttribute), finalAttribute)
//            finalAttribute =
//                Objects.requireNonNullElse(attributeLoop(attribute, liquid, finalAttribute), finalAttribute)
//            if (!(finalAttribute.attribute == Attribute.GENERIC_FLYING_SPEED && finalAttribute.value == 0.842)) {
//                list.add(finalAttribute)
//            }
//        }
//        conditionAttributeMap[originValue] = list
//    }
//
//    private fun potionLoop(
//        potionType: PotionEffectType,
//        potionEffects: List<PotionEffect>?,
//        finalPotion: PotionEffect?
//    ): PotionEffect? {
//        for (potion in ListUtils.emptyIfNull(potionEffects)) {
//            if (potion.type === potionType && finalPotion!!.amplifier < potion.amplifier) {
//                return potion
//            }
//        }
//        return null
//    }
//
//    private fun attributeLoop(
//        attribute: Attribute,
//        attributes: List<OriginAttribute>?,
//        finalAttribute: OriginAttribute?
//    ): OriginAttribute? {
//        for (var1 in ListUtils.emptyIfNull(attributes)) {
//            if (var1.attribute == attribute && finalAttribute!!.value < var1.value) {
//                return var1
//            }
//        }
//        return null
//    }
//
//    val name: String?
//        get() = nameMap[OriginValue.NAME]
//    val colour: String?
//        get() = nameMap[OriginValue.COLOUR]
//    val displayName: String?
//        get() = nameMap[OriginValue.DISPLAY_NAME]
//    val bracketName: String?
//        get() = nameMap[OriginValue.BRACKET_NAME]
//    val hurt: Sound?
//        get() = soundMap[OriginValue.HURT]
//    val death: Sound?
//        get() = soundMap[OriginValue.DEATH]
//    val dayTitle: String?
//        get() = timeMessageMap[OriginValue.DAY_TITLE]
//    val daySubtitle: String?
//        get() = timeMessageMap[OriginValue.DAY_SUBTITLE]
//    val nightTitle: String?
//        get() = timeMessageMap[OriginValue.NIGHT_TITLE]
//    val nightSubtitle: String?
//        get() = timeMessageMap[OriginValue.NIGHT_SUBTITLE]
//    val requiredPermission: List<String>
//        get() = permissionMap[OriginValue.PERMISSION_REQUIRED]!!
//    val givenPermission: List<String>
//        get() = permissionMap[OriginValue.PERMISSION_GIVEN]!!
//    val isGeneralEffects: Boolean?
//        get() = effectEnableMap[OriginValue.GENERAL]
//    val isTimeEffects: Boolean?
//        get() = effectEnableMap[OriginValue.TIME]
//    val isLiquidEffects: Boolean?
//        get() = effectEnableMap[OriginValue.LIQUID]
//    val isDimensionEffects: Boolean?
//        get() = effectEnableMap[OriginValue.DIMENSION]
//
//    fun isEffects(originValue: OriginValue): Boolean? {
//        return effectEnableMap[originValue]
//    }
//
//    val isSlowFalling: Boolean
//        get() = specialMap[OriginValue.SPECIAL_SLOWFALLING] == 1
//    val isNightVision: Boolean
//        get() = specialMap[OriginValue.SPECIAL_NIGHTVISION] == 1
//    val isJumpBoost: Boolean
//        get() = specialMap[OriginValue.SPECIAL_JUMPBOOST] != 0
//
//    fun isSpecial(originValue: OriginValue): Boolean {
//        return specialMap[originValue]!! >= 1
//    }
//
//    fun getJumpBoost(): Int? {
//        return specialMap[OriginValue.SPECIAL_JUMPBOOST]
//    }
//
//    val effects: List<PotionEffect>
//        get() = effectMap[OriginValue.GENERAL]!!
//    val dayEffects: List<PotionEffect>
//        get() = effectMap[OriginValue.DAY]!!
//    val nightEffects: List<PotionEffect>
//        get() = effectMap[OriginValue.NIGHT]!!
//    val waterEffects: List<PotionEffect>
//        get() = effectMap[OriginValue.WATER]!!
//    val lavaEffects: List<PotionEffect>
//        get() = effectMap[OriginValue.LAVA]!!
//    val attributes: List<OriginAttribute>
//        get() = attributeMap[OriginValue.GENERAL]!!
//    val dayAttributes: List<OriginAttribute>
//        get() = attributeMap[OriginValue.DAY]!!
//    val nightAttributes: List<OriginAttribute>
//        get() = attributeMap[OriginValue.NIGHT]!!
//    val waterAttributes: List<OriginAttribute>
//        get() = attributeMap[OriginValue.WATER]!!
//    val lavaAttributes: List<OriginAttribute>
//        get() = attributeMap[OriginValue.LAVA]!!
//
//    fun getAttributes(originValue: OriginValue): List<OriginAttribute?> {
//        return conditionAttributeMap[originValue]!!
//    }
//
//    fun getPotions(originValue: OriginValue): List<PotionEffect?> {
//        return conditionEffectMap[originValue]!!
//    }
//
//    val isDamage: Boolean?
//        get() = damageEnableMap[OriginValue.DAMAGE]
//    val isSun: Boolean?
//        get() = damageEnableMap[OriginValue.SUN]
//    val isFall: Boolean?
//        get() = damageEnableMap[OriginValue.FALL]
//    val isRain: Boolean?
//        get() = damageEnableMap[OriginValue.RAIN]
//    val isWater: Boolean?
//        get() = damageEnableMap[OriginValue.WATER]
//    val isLava: Boolean?
//        get() = damageEnableMap[OriginValue.LAVA]
//
//    fun getSun(): Int? {
//        return damageAmountMap[OriginValue.SUN]
//    }
//
//    fun getFall(): Int? {
//        return damageAmountMap[OriginValue.FALL]
//    }
//
//    fun getRain(): Int? {
//        return damageAmountMap[OriginValue.RAIN]
//    }
//
//    fun getWater(): Int? {
//        return damageAmountMap[OriginValue.WATER]
//    }
//
//    fun getLava(): Int? {
//        return damageAmountMap[OriginValue.LAVA]
//    }
//
//    val item: ItemStack?
//        get() = GUIItem[OriginValue.ITEM] as ItemStack?
//    val slot: Int?
//        get() = GUIItem[OriginValue.SLOT] as Int?
//
//    enum class OriginValue {
//        NAME, COLOUR, DISPLAY_NAME, BRACKET_NAME, HURT, DEATH, DAY_TITLE, DAY_SUBTITLE, NIGHT_TITLE, NIGHT_SUBTITLE, PERMISSION_REQUIRED, PERMISSION_GIVEN, GENERAL, TIME, LIQUID, DIMENSION, DAY, NIGHT, WATER, LAVA, OVERWORLD, NETHER, END, SPECIAL_SLOWFALLING, SPECIAL_NIGHTVISION, SPECIAL_JUMPBOOST, DAMAGE, SUN, FALL, RAIN, ITEM, SLOT,  /*
//		O = OVERWORLD
//		N = NETHER
//		E = END
//		D = DAY
//		N = NIGHT
//		W = WATER
//		L = LAVA
//		*/
//        OD, ON, ODW, ODL, ONW, ONL, N, NL, E, EW, EL
//    }
//
//    companion object {
//        private val originMap: MutableMap<String, OriginKotlin> = LinkedHashMap()
//        private fun addToMap(origin: OriginKotlin) {
//            originMap.putIfAbsent(origin.name!!.toUpperCase(), origin)
//        }
//
//        fun valueOf(name: String): OriginKotlin {
//            return originMap[name]
//                ?: throw IllegalArgumentException("No Origin by the name $name found")
//        }
//
//        fun values(): Array<OriginKotlin> {
//            return originMap.values.toArray<OriginKotlin> { _Dummy_.__Array__() }.clone()
//        }
//    }
//
//    init {
//        this.nameMap.putAll(nameMap!!)
//        this.soundMap.putAll(soundMap!!)
//        this.timeMessageMap.putAll(timeMessageMap!!)
//        this.permissionMap.putAll(permissionMap!!)
//        this.effectEnableMap.putAll(effectEnableMap!!)
//        this.specialMap.putAll(specialMap!!)
//        this.effectMap.putAll(effectMap)
//        this.attributeMap.putAll(attributeMap)
//        this.damageEnableMap.putAll(damageEnableMap!!)
//        this.damageAmountMap.putAll(damageAmountMap!!)
//        this.GUIItem.putAll(GUIItem!!)
//        constructPotionMap(
//            OriginValue.OD,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.DAY],
//            null
//        )
//        constructPotionMap(
//            OriginValue.ON,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.NIGHT],
//            null
//        )
//        constructPotionMap(
//            OriginValue.ODW,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.DAY],
//            effectMap[OriginValue.WATER]
//        )
//        constructPotionMap(
//            OriginValue.ODL,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.DAY],
//            effectMap[OriginValue.LAVA]
//        )
//        constructPotionMap(
//            OriginValue.ONW,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.NIGHT],
//            effectMap[OriginValue.WATER]
//        )
//        constructPotionMap(
//            OriginValue.ONL,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.OVERWORLD]!!,
//            effectMap[OriginValue.NIGHT],
//            effectMap[OriginValue.LAVA]
//        )
//        constructPotionMap(OriginValue.N, effectMap[OriginValue.GENERAL]!!, effectMap[OriginValue.NETHER]!!, null, null)
//        constructPotionMap(
//            OriginValue.NL,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.NETHER]!!,
//            null,
//            effectMap[OriginValue.LAVA]
//        )
//        constructPotionMap(OriginValue.E, effectMap[OriginValue.GENERAL]!!, effectMap[OriginValue.END]!!, null, null)
//        constructPotionMap(
//            OriginValue.EW,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.END]!!,
//            null,
//            effectMap[OriginValue.WATER]
//        )
//        constructPotionMap(
//            OriginValue.EL,
//            effectMap[OriginValue.GENERAL]!!,
//            effectMap[OriginValue.END]!!,
//            null,
//            effectMap[OriginValue.LAVA]
//        )
//        constructAttributeMap(
//            OriginValue.OD,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.DAY],
//            null
//        )
//        constructAttributeMap(
//            OriginValue.ON,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.NIGHT],
//            null
//        )
//        constructAttributeMap(
//            OriginValue.ODW,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.DAY],
//            attributeMap[OriginValue.WATER]
//        )
//        constructAttributeMap(
//            OriginValue.ODL,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.DAY],
//            attributeMap[OriginValue.LAVA]
//        )
//        constructAttributeMap(
//            OriginValue.ONW,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.NIGHT],
//            attributeMap[OriginValue.WATER]
//        )
//        constructAttributeMap(
//            OriginValue.ONL,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.OVERWORLD]!!,
//            attributeMap[OriginValue.NIGHT],
//            attributeMap[OriginValue.LAVA]
//        )
//        constructAttributeMap(
//            OriginValue.N,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.NETHER]!!,
//            null,
//            null
//        )
//        constructAttributeMap(
//            OriginValue.NL,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.NETHER]!!,
//            null,
//            attributeMap[OriginValue.LAVA]
//        )
//        constructAttributeMap(
//            OriginValue.E,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.END]!!,
//            null,
//            null
//        )
//        constructAttributeMap(
//            OriginValue.EW,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.END]!!,
//            null,
//            attributeMap[OriginValue.WATER]
//        )
//        constructAttributeMap(
//            OriginValue.EL,
//            attributeMap[OriginValue.GENERAL]!!,
//            attributeMap[OriginValue.END]!!,
//            null,
//            attributeMap[OriginValue.LAVA]
//        )
//        addToMap(this)
//    }
//}