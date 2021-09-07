@file:Suppress("unused")
@file:JvmName("OriginHandler")
package me.racci.sylphia.origins

import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.schedule
import me.racci.raccilib.Level
import me.racci.raccilib.log
import me.racci.raccilib.utils.FileValidationException
import me.racci.raccilib.utils.items.builders.ItemBuilder
import me.racci.raccilib.utils.text.LegacyUtils
import me.racci.raccilib.utils.text.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.enums.Path
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Origins
import me.racci.sylphia.origins.OriginHandler.OriginsMap.originsMap
import me.racci.sylphia.utils.AttributeUtils
import me.racci.sylphia.utils.PotionUtils
import net.kyori.adventure.text.Component
import org.apache.commons.collections4.MapUtils.emptyIfNull
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
    private val passives = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_PASSIVES))!!
    private val abilities = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_ABILITIES))!!
    private val debuffs = LegacyUtils.parseLegacy(spaces + Lang.Messages.get(Origins.LORE_DEBUFFS))!!
    private val select = LegacyUtils.parseLegacy(Lang.Messages.get(Origins.LORE_SELECT))!!
    private val indent: String = Lang.Messages.get(Origins.LORE_INDENT) + " "

    object OriginsMap {
        internal val originsMap: HashMap<String, Origin> = HashMap()
        fun get(): HashMap<String, Origin> {
            return originsMap
        }
    }

    init {
        refreshOrigins()
    }

    fun refreshOrigins() {
        scheduler.schedule(plugin, SynchronizationContext.ASYNC) {
            originsMap.clear()
            val file: Map<String, YamlConfiguration> = getAllOriginConfigurations()
            if(file.isNotEmpty()) {
                for(originEntry: Map.Entry<String, YamlConfiguration> in file.entries) {
                    if(!originsMap.containsKey(originEntry.key)) {
                        originsMap[originEntry.key] = createOrigin(originEntry.key, originEntry.value)
                    }
                }
            } else {
                log(Level.WARNING, "No origins found!")
            }
        }
    }

    private fun createOrigin(origin: String, config: YamlConfiguration): Origin {

        val nameMap = LinkedHashMap<OriginValue, String>()
        val soundMap = LinkedHashMap<OriginValue, Sound>()
        val timeMessageMap = LinkedHashMap<OriginValue, String?>()
        val permissionMap = LinkedHashMap<OriginValue, ArrayList<String?>?>()
        val effectEnableMap = LinkedHashMap<OriginValue, Boolean>()
        val specialMap = LinkedHashMap<OriginValue, Int>()
        val effectMap = LinkedHashMap<OriginValue, List<PotionEffect?>?>()
        val attributeMap = LinkedHashMap<OriginValue, List<OriginAttribute?>?>()
        val damageEnableMap = LinkedHashMap<OriginValue, Boolean>()
        val damageAmountMap = LinkedHashMap<OriginValue, Int>()
        val guiItem = LinkedHashMap<OriginValue, Any>()
        val item: ItemStack?
        val slot: Int?

        // Names and colours
        val name: String = config.getString(Path.NAME.path) ?: "null"
        val colour = colour(config.getString(Path.COLOUR.path) ?: "&8", true)
        nameMap[OriginValue.NAME] = name
        nameMap[OriginValue.COLOUR] = colour!!
        nameMap[OriginValue.DISPLAY_NAME] = colour + name
        nameMap[OriginValue.BRACKET_NAME] = "$colour[$name]"
        // Sounds

        soundMap[OriginValue.HURT] = Sound.valueOf(config.getString(Path.HURT.path)?: "ENTITY_PLAYER_HURT")
        soundMap[OriginValue.DEATH] = Sound.valueOf(config.getString(Path.DEATH.path)?: "ENTITY_PLAYER_DEATH")
        // Time titles and subtitles
        timeMessageMap[OriginValue.DAY_TITLE] = colour(config.getString(Path.DAY_TITLE.path), true)
        timeMessageMap[OriginValue.DAY_SUBTITLE] = colour(config.getString(Path.DAY_SUBTITLE.path), true)
        timeMessageMap[OriginValue.NIGHT_TITLE] = colour(config.getString(Path.NIGHT_TITLE.path), true)
        timeMessageMap[OriginValue.NIGHT_SUBTITLE] = colour(config.getString(Path.NIGHT_SUBTITLE.path), true)
        // Permissions
        permissionMap[OriginValue.PERMISSION_REQUIRED] = ArrayList(config.getStringList(Path.REQUIRED.path))
        permissionMap[OriginValue.PERMISSION_GIVEN] = ArrayList(config.getStringList(Path.GIVEN.path))
        // Effect enables
        effectEnableMap[OriginValue.GENERAL] = config.getBoolean(Path.GENERAL_ENABLE.path)
        effectEnableMap[OriginValue.TIME] = config.getBoolean(Path.TIME_ENABLE.path)
        effectEnableMap[OriginValue.LIQUID] = config.getBoolean(Path.LIQUID_ENABLE.path)
        effectEnableMap[OriginValue.DIMENSION] = config.getBoolean(Path.LIQUID_ENABLE.path)
        // Toggleable effects
        specialMap[OriginValue.SPECIAL_SLOWFALLING] = if (config.getBoolean(Path.SLOWFALLING.path)) 0 else 1
        specialMap[OriginValue.SPECIAL_NIGHTVISION] = if (config.getBoolean(Path.NIGHTVISION.path)) 0 else 1
        specialMap[OriginValue.SPECIAL_JUMPBOOST] = config.getInt(Path.JUMPBOOST.path)
        // Damage enables
        damageEnableMap[OriginValue.DAMAGE] = config.getBoolean(Path.DAMAGE_ENABLE.path)
        damageEnableMap[OriginValue.SUN] = config.getBoolean(Path.SUN_ENABLE.path)
        damageEnableMap[OriginValue.FALL] = config.getBoolean(Path.FALL_ENABLE.path)
        damageEnableMap[OriginValue.RAIN] = config.getBoolean(Path.RAIN_ENABLE.path)
        damageEnableMap[OriginValue.WATER] = config.getBoolean(Path.WATER_ENABLE.path)
        damageEnableMap[OriginValue.LAVA] = config.getBoolean(Path.LAVA_ENABLE.path)
        // Damage amounts
        damageAmountMap[OriginValue.SUN] = config.getInt(Path.SUN_AMOUNT.path)
        damageAmountMap[OriginValue.FALL] = config.getInt(Path.FALL_AMOUNT.path)
        damageAmountMap[OriginValue.RAIN] = config.getInt(Path.RAIN_AMOUNT.path)
        damageAmountMap[OriginValue.WATER] = config.getInt(Path.WATER_AMOUNT.path)
        damageAmountMap[OriginValue.LAVA] = config.getInt(Path.LAVA_AMOUNT.path)

        validateAttribute(Path.GENERAL_ATTRIBUTES.path, OriginValue.GENERAL, config)
        if(effectEnableMap[OriginValue.GENERAL] == true) {
            attributeMap.putAll(validateAttribute(Path.GENERAL_ATTRIBUTES.path, OriginValue.GENERAL, config))
            effectMap.putAll(emptyIfNull(validatePotion(Path.GENERAL_EFFECTS.path, OriginValue.GENERAL, config)))
        }
        if(effectEnableMap[OriginValue.TIME] == true) {
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.DAY_ATTRIBUTES.path, OriginValue.DAY, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.DAY_EFFECTS.path, OriginValue.DAY, config)))
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.NIGHT_ATTRIBUTES.path, OriginValue.NIGHT, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.NIGHT_EFFECTS.path, OriginValue.NIGHT, config)))
        }
        if(effectEnableMap[OriginValue.LIQUID] == true) {
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.WATER_ATTRIBUTES.path, OriginValue.WATER, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.WATER_EFFECTS.path, OriginValue.WATER, config)))
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.LAVA_ATTRIBUTES.path, OriginValue.LAVA, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.LAVA_EFFECTS.path, OriginValue.LAVA, config)))
        }
        if(effectEnableMap[OriginValue.DIMENSION] == true) {
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.OVERWORLD_ATTRIBUTES.path, OriginValue.OVERWORLD, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.OVERWORLD_EFFECTS.path, OriginValue.OVERWORLD, config)))
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.NETHER_ATTRIBUTES.path, OriginValue.NETHER, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.NETHER_EFFECTS.path, OriginValue.NETHER, config)))
            attributeMap.putAll(emptyIfNull(validateAttribute(Path.END_ATTRIBUTES.path, OriginValue.END, config)))
            effectMap.putAll(emptyIfNull(validatePotion(Path.END_EFFECTS.path, OriginValue.END, config)))
        }
        var itemStack: ItemStack? = null
        if(!config.isSet(Path.GUI_SKULL.path) || (config.isBoolean(
                Path.GUI_SKULL.path) && !config.getBoolean(
                Path.GUI_SKULL.path))) {
            if(EnumUtils.isValidEnum(Material::class.java, config.getString(Path.GUI_MATERIAL.path))) {
                itemStack = ItemStack(Material.getMaterial(config.getString(Path.GUI_MATERIAL.path) ?: "APPLE")!!)
            } else {
                log(Level.WARNING, "The material for $origin is invalid!")
            }
        } else if (config.isSet(Path.GUI_SKULL.path) && !config.isBoolean(
                Path.GUI_SKULL.path)) {
            itemStack = ItemBuilder.skull()
                .texture(config.getString(Path.GUI_SKULL.path)!!)
                .build()!!
        }
        val enchanted: Boolean = config.getBoolean(Path.GUI_ENCHANTED.path)
        val itemLore = ArrayList<Component>()
        if(config.get(Path.GUI_LORE_DESCRIPTION.path) != null) {
            itemLore.add(Component.empty())
            config.getStringList(Path.GUI_LORE_DESCRIPTION.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(var1x, true))}
            itemLore.add(Component.empty())
        }
        if(config.get(Path.GUI_LORE_PASSIVES.path) != null) {
            itemLore.add(passives)
            config.getStringList(Path.GUI_LORE_PASSIVES.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
            itemLore.add(Component.empty())
        }
        if(config.get(Path.GUI_LORE_ABILITIES.path) != null) {
            itemLore.add(abilities)
            config.getStringList(Path.GUI_LORE_ABILITIES.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
            itemLore.add(Component.empty())
        }
        if(config.get(Path.GUI_LORE_DEBUFFS.path) != null) {
            itemLore.add(debuffs)
            config.getStringList(Path.GUI_LORE_DEBUFFS.path).forEach { var1x -> LegacyUtils.parseLegacy(colour(indent + var1x, true))}
            itemLore.add(Component.empty())
        }
        item = ItemBuilder.from(itemStack!!)
            .amount(1)
            .glow(enchanted)
            .unbreakable()
            .name(LegacyUtils.parseLegacy(nameMap[OriginValue.DISPLAY_NAME])!!)
            .setNbt("GUIItem", true)
            .lore(itemLore)
            .build()!!
        slot = config.getInt(Path.GUI_SLOT.path)
        guiItem[OriginValue.ITEM] = item
        guiItem[OriginValue.SLOT] = slot
        val finalOrigin = Origin(
            nameMap,
            soundMap,
            timeMessageMap,
            permissionMap,
            effectEnableMap,
            specialMap,
            effectMap,
            attributeMap,
            damageEnableMap,
            damageAmountMap,
            guiItem
        )

        if (permissionMap[OriginValue.PERMISSION_REQUIRED]?.get(0) != null) {
            requiredPermList.putIfAbsent(finalOrigin, permissionMap[OriginValue.PERMISSION_REQUIRED]?.get(0)!!)
        }
        log(Level.INFO, "Debug: Registered origin " + finalOrigin.displayName)
        return finalOrigin
    }

    private fun validateAttribute(path: String, originValue: OriginValue, file: YamlConfiguration): LinkedHashMap<OriginValue, ArrayList<OriginAttribute>> {
        val attributes = ArrayList<OriginAttribute>()
        for (attributeString: String in file.getStringList(path)) {
            attributes.add(AttributeUtils.parseOriginAttribute(attributeString)!!)
        }
        val map = LinkedHashMap<OriginValue, ArrayList<OriginAttribute>>()
        map[originValue] = attributes
        return map
    }

    private fun validatePotion(path: String, originValue: OriginValue, file: YamlConfiguration): Map<OriginValue, ArrayList<PotionEffect>> {
        val potions = ArrayList<PotionEffect>()
        for (effectString: String in file.getStringList(path)) {
            potions.add(PotionUtils.parseOriginPotion(effectString)!!)
        }
        val map = LinkedHashMap<OriginValue, ArrayList<PotionEffect>>()
        map[originValue] = potions
        return map
    }

    private fun getAllOriginConfigurations(): Map<String, YamlConfiguration> {
        val originMap: HashMap<String, YamlConfiguration> = HashMap()
        val files: Array<out File> = File(plugin.dataFolder.absolutePath + "/Origins").listFiles()!!
        for(originFile: File in files) {
            if(!originFile.isDirectory) {
                validateFile(originFile, YamlConfiguration.loadConfiguration(originFile))
                originMap[originFile.name.uppercase().replace(".YML", "")] = YamlConfiguration.loadConfiguration(originFile)
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