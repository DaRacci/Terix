@file:Suppress("unused")
@file:JvmName("OriginHandler")
package me.racci.sylphia.origins

import me.racci.raccilib.Level
import me.racci.raccilib.log
import me.racci.raccilib.utils.FileValidationException
import me.racci.raccilib.utils.items.builders.ItemBuilder
import me.racci.raccilib.utils.strings.LegacyUtils
import me.racci.raccilib.utils.strings.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Origins
import me.racci.sylphia.origins.OriginFile.*
import me.racci.sylphia.origins.OriginHandler.OriginsMap.origins
import me.racci.sylphia.utils.AttributeUtils
import me.racci.sylphia.utils.PotionUtils
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.scheduler.BukkitScheduler
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


class OriginHandler(private val plugin: Sylphia): Listener {

    // TODO Move to GUI
    private val requiredPermList = HashMap<Origin, String>()

    private val scheduler: BukkitScheduler = Bukkit.getScheduler()
    private val spaces = "              "
    private val passives = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_PASSIVES))
    private val abilities = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_ABILITIES))
    private val debuffs = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_DEBUFFS))
    private val select = LegacyUtils.parseLegacy(Lang.Messages.get(Origins.LORE_SELECT))
    private val indent: String = Lang.Messages.get(Origins.LORE_INDENT) + " "

    object OriginsMap {
        internal val origins: HashMap<String, Origin> = HashMap()
        fun get(): HashMap<String, Origin> {
            return origins
        }
    }

    init {
        loadOrigins()
    }

    fun loadOrigins() {
        origins.clear()
        val file: Map<String, YamlConfiguration> = getAllOriginConfigurations()
        if(file.isNotEmpty()) {
            for(originEntry: Map.Entry<String, YamlConfiguration> in file.entries) {
                if(!origins.containsKey(originEntry.key)) {
                    origins[originEntry.key] = createOrigin(originEntry.value)
                }
            }
        } else {
            log(Level.WARNING, "No origins found!")
        }

    }

    private fun createOrigin(config: YamlConfiguration): Origin {

        val identityMap = LinkedHashMap<OriginValue, String>()
        val soundMap = LinkedHashMap<OriginValue, Sound>()
        val timeMessageMap = LinkedHashMap<OriginValue, String?>()
        val permissionMap = LinkedHashMap<OriginValue, Any?>()
        val passiveEnableMap = LinkedHashMap<OriginValue, Boolean>()
        val toggleMap = LinkedHashMap<OriginValue, Any>()
        val effectMap = LinkedHashMap<OriginValue, HashSet<PotionEffect>>()
        val attributeMap = LinkedHashMap<OriginValue, HashSet<OriginAttribute>>()
        val damageEnableMap = LinkedHashMap<OriginValue, Boolean>()
        val damageAmountMap = LinkedHashMap<OriginValue, Int>()
        val guiItem = LinkedHashMap<OriginValue, Any?>()

        val attributeBase = HashMap<OriginValue, HashSet<BaseAttribute>>()
        val attributeModifier = HashMap<OriginValue, HashSet<AttributeModifier>>()


        var var1: Any? = config.getString(IDENTITY_NAME.path) ?: IDENTITY_NAME.default
        var var2: Any? = colour(config.getString(IDENTITY_COLOUR.path)) ?: IDENTITY_COLOUR.default

        identityMap[OriginValue.NAME] = var1 as String
        identityMap[OriginValue.COLOUR] = var2 as String
        identityMap[OriginValue.DISPLAY_NAME] = "$var2$var1"
        identityMap[OriginValue.BRACKET_NAME] = "$var2[$var1]"

        soundMap[OriginValue.HURT] = Sound.valueOf(config.getString(SOUND_HURT.path) ?: "ENTITY_PLAYER_HURT")
        soundMap[OriginValue.DEATH] = Sound.valueOf(config.getString(SOUND_HURT.path) ?: "ENTITY_PLAYER_DEATH")
        soundMap[OriginValue.DAY_SOUND] = Sound.valueOf(config.getString(DAY_SOUND.path) ?: "ENTITY_PLAYER_HURT")
        soundMap[OriginValue.NIGHT_SOUND] = Sound.valueOf(config.getString(NIGHT_SOUND.path) ?: "ENTITY_PLAYER_HURT")

        timeMessageMap[OriginValue.DAY_TITLE] = colour(config.getString(DAY_TITLE.path)) ?: DAY_TITLE.default as String
        timeMessageMap[OriginValue.DAY_SUBTITLE] = colour(config.getString(DAY_SUBTITLE.path)) ?: DAY_SUBTITLE.default as String
        timeMessageMap[OriginValue.NIGHT_TITLE] = colour(config.getString(NIGHT_TITLE.path)) ?: NIGHT_TITLE.default as String
        timeMessageMap[OriginValue.NIGHT_SUBTITLE] = colour(config.getString(NIGHT_SUBTITLE.path)) ?: NIGHT_SUBTITLE.default as String

        permissionMap[OriginValue.PERMISSION_REQUIRED] = config.getString(PERMISSION_REQUIRED.path).orEmpty()
        permissionMap[OriginValue.PERMISSION_GIVEN] = setOf(config.getStringList(PERMISSION_GIVEN.path)).toHashSet()

        passiveEnableMap[OriginValue.GENERAL] = config.getBoolean(PASSIVES_GENERAL.path)
        passiveEnableMap[OriginValue.TIME] = config.getBoolean(PASSIVES_TIME.path)
        passiveEnableMap[OriginValue.LIQUID] = config.getBoolean(PASSIVES_LIQUID.path)
        passiveEnableMap[OriginValue.DIMENSION] = config.getBoolean(PASSIVES_DIMENSION.path)

        toggleMap[OriginValue.TOGGLE_SLOWFALLING] = config.getBoolean(TOGGLES_SLOWFALLING.path)
        toggleMap[OriginValue.TOGGLE_NIGHTVISION] = config.getBoolean(TOGGLES_NIGHTVISION.path)
        var1 = config.get(TOGGLES_JUMPBOOST.path) ?: false
        toggleMap[OriginValue.TOGGLE_JUMPBOOST] =
            if(var1 is Boolean)
                if(var1) 1 else 0
                    else if(var1 is Int && (var1 in 0..16)) var1 else 0

        damageEnableMap[OriginValue.SUN] = config.getBoolean(SUN_ENABLED.path)
        damageEnableMap[OriginValue.FALL] = config.getBoolean(FALL_ENABLED.path)
        damageEnableMap[OriginValue.RAIN] = config.getBoolean(RAIN_ENABLED.path)
        damageEnableMap[OriginValue.WATER] = config.getBoolean(WATER_ENABLED.path)
        damageEnableMap[OriginValue.LAVA] = config.getBoolean(LAVA_ENABLED.path)

        damageAmountMap[OriginValue.SUN] = config.get(SUN_AMOUNT.path) as Int? ?: SUN_AMOUNT.default as Int
        damageAmountMap[OriginValue.FALL] = config.get(FALL_AMOUNT.path) as Int? ?: FALL_AMOUNT.default as Int
        damageAmountMap[OriginValue.RAIN] = config.get(RAIN_AMOUNT.path) as Int? ?: RAIN_AMOUNT.default as Int
        damageAmountMap[OriginValue.WATER] = config.get(WATER_AMOUNT.path) as Int? ?: WATER_AMOUNT.default as Int
        damageAmountMap[OriginValue.LAVA] = config.get(LAVA_AMOUNT.path) as Int? ?: LAVA_AMOUNT.default as Int

        attributeBase[OriginValue.GENERAL] = if(passiveEnableMap[OriginValue.GENERAL] == true) attributeBase(GENERAL_ATTRIBUTES.path, config) else emptySet<BaseAttribute>().toHashSet()
        attributeModifier[OriginValue.DAY] = if(passiveEnableMap[OriginValue.TIME] == true) attributeModifiers(OriginValue.DAY, DAY_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.NIGHT] = if(passiveEnableMap[OriginValue.TIME] == true) attributeModifiers(OriginValue.NIGHT, NIGHT_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.WATER] = if(passiveEnableMap[OriginValue.LIQUID] == true) attributeModifiers(OriginValue.WATER, WATER_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.LAVA] = if(passiveEnableMap[OriginValue.LIQUID] == true) attributeModifiers(OriginValue.LAVA, LAVA_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.OVERWORLD] = if(passiveEnableMap[OriginValue.DIMENSION] == true) attributeModifiers(OriginValue.OVERWORLD, OVERWORLD_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.NETHER] = if(passiveEnableMap[OriginValue.DIMENSION] == true) attributeModifiers(OriginValue.NETHER, NETHER_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()
        attributeModifier[OriginValue.END] = if(passiveEnableMap[OriginValue.DIMENSION] == true) attributeModifiers(OriginValue.END, END_ATTRIBUTES.path, config) else emptySet<AttributeModifier>().toHashSet()

        effectMap[OriginValue.GENERAL] = if(passiveEnableMap[OriginValue.GENERAL] == true) verifyPotions(GENERAL_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.DAY] = if(passiveEnableMap[OriginValue.TIME] == true) verifyPotions(DAY_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.NIGHT] = if(passiveEnableMap[OriginValue.TIME] == true) verifyPotions(NIGHT_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.WATER] = if(passiveEnableMap[OriginValue.LIQUID] == true) verifyPotions(WATER_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.LAVA] = if(passiveEnableMap[OriginValue.LIQUID] == true) verifyPotions(LAVA_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.OVERWORLD] = if(passiveEnableMap[OriginValue.DIMENSION] == true) verifyPotions(OVERWORLD_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.NETHER] = if(passiveEnableMap[OriginValue.DIMENSION] == true) verifyPotions(NETHER_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()
        effectMap[OriginValue.END] = if(passiveEnableMap[OriginValue.DIMENSION] == true) verifyPotions(END_EFFECTS.path, config) else emptySet<PotionEffect>().toHashSet()

        var1 = config.getBoolean(GUI_ENABLED.path)
        if(var1) {
            var1 = config.getString(GUI_MATERIAL.path)
            var1 = if(EnumUtils.isValidEnum(Material::class.java, var1)) {
                Material.valueOf(var1 ?: "BARRIER")
            } else {
                ItemBuilder.skull()
                    .texture(var1 as String)
                    .build()
            }
            var2 = ArrayList<Component>()
            var2.add(Component.empty())
            if(config.get(GUI_LORE_DESCRIPTION.path) != null) {
                config.getStringList(GUI_LORE_DESCRIPTION.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(var1x, true))}
                var2.add(Component.empty())
            }
            if(config.get(GUI_LORE_PASSIVES.path) != null) {
                var2.add(passives)
                config.getStringList(GUI_LORE_PASSIVES.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
                var2.add(Component.empty())
            }
            if(config.get(GUI_LORE_ABILITIES.path) != null) {
                var2.add(abilities)
                config.getStringList(GUI_LORE_ABILITIES.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
                var2.add(Component.empty())
            }
            if(config.get(GUI_LORE_DEBUFFS.path) != null) {
                var2.add(debuffs)
                config.getStringList(GUI_LORE_DEBUFFS.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
                var2.add(Component.empty())
            }

            var1 = ItemBuilder.from(var1 as ItemStack)
                .amount(1)
                .glow(config.getBoolean(GUI_GLOW.path))
                .setNbt("GUIItem", true)
                .name(LegacyUtils.parseLegacy(identityMap[OriginValue.DISPLAY_NAME]))
                .lore(var2)
                .build()
        }
//        TODO("If item nbt is false make item null in constructor")
        if(var1 is Boolean) {
            var1 = ItemBuilder.from(Material.BEDROCK)
                .setNbt("GUIItem", false)
                .build()
        }
        var2 = config.getInt(GUI_SLOT.path)
        guiItem[OriginValue.ITEM] = var1
        guiItem[OriginValue.SLOT] = var2
        return Origin(
            identityMap,
            soundMap,
            timeMessageMap,
            permissionMap,
            passiveEnableMap,
            attributeBase,
            attributeModifier,
            toggleMap,
            effectMap,
            attributeMap,
            damageEnableMap,
            damageAmountMap,
            guiItem
        )
    }

    private fun attributeBase(path: String, file: YamlConfiguration) : HashSet<BaseAttribute> {
        val attributes = HashSet<BaseAttribute>()
        for(attributeString in file.getStringList(path)) {
            attributes.add(AttributeUtils.createBaseAttribute(attributeString) ?: continue)
        }
        return attributes
    }

    private fun attributeModifiers(condition: OriginValue, path: String, file: YamlConfiguration): HashSet<AttributeModifier> {
        val attributes = HashSet<AttributeModifier>()
        for(attributeString in file.getStringList(path)) {
            attributes.add(AttributeUtils.createModifier(condition, attributeString) ?: continue)
        }
        return attributes
    }

    private fun verifyPotions(path: String, file: YamlConfiguration): HashSet<PotionEffect> {
        val potions = HashSet<PotionEffect>()
        for(potionString in file.getStringList(path)) {
            potions.add(PotionUtils.createPotion(potionString) ?: continue)
        }
        return potions
    }

    private fun validatePotion(path: String, originValue: OriginValue, file: YamlConfiguration): Map<OriginValue, ArrayList<PotionEffect>> {
        val potions = ArrayList<PotionEffect>()
        for (effectString: String in file.getStringList(path)) {
            potions.add(PotionUtils.createPotion(effectString)!!)
        }
        val map = LinkedHashMap<OriginValue, ArrayList<PotionEffect>>()
        map[originValue] = potions
        return map
    }

    private fun getAllOriginConfigurations(): Map<String, YamlConfiguration> {
        val originMap: HashMap<String, YamlConfiguration> = HashMap()
        val files: Array<out File>? = File(plugin.dataFolder.absolutePath + "/Origins").listFiles()
        if (files != null) {
            for(originFile: File in files) {
                if(!originFile.isDirectory) {
                    validateFile(originFile, YamlConfiguration.loadConfiguration(originFile))
                    originMap[originFile.name.uppercase().replace(".YML", "")] = YamlConfiguration.loadConfiguration(originFile)
                }
            }
        }
        return originMap
    }

    private fun validateFile(file: File, config: YamlConfiguration) {
        try {
            val defaultFile: YamlConfiguration = YamlConfiguration.loadConfiguration(InputStreamReader(plugin.getResource("Origin.yml")!!, StandardCharsets.UTF_8))
            val configSection: ConfigurationSection = defaultFile.getConfigurationSection("")!!
            var keysAdded = 0
            for(key: String in configSection.getKeys(true)) {
                if(!configSection.isConfigurationSection(key) && !config.contains(key)) {
                    config.set(key, defaultFile.get(key))
                    keysAdded++
                }
            }
            config.save(file)
            if(keysAdded > 0) {
                log(Level.INFO, "${file.name} was missing keys and had $keysAdded new keys added.")
            }
        } catch (e: Exception) {
            throw FileValidationException("There was an error validating the config " + config.name + e.printStackTrace())
        }
    }
}