package me.racci.terix.factories

import me.racci.raccicore.builders.ItemBuilder
import me.racci.raccicore.interfaces.IFactory
import me.racci.raccicore.utils.strings.colour
import me.racci.raccicore.utils.strings.textOf
import me.racci.terix.enums.Condition
import me.racci.terix.enums.Condition.DAY
import me.racci.terix.enums.Condition.END
import me.racci.terix.enums.Condition.LAVA
import me.racci.terix.enums.Condition.NETHER
import me.racci.terix.enums.Condition.NIGHT
import me.racci.terix.enums.Condition.OVERWORLD
import me.racci.terix.enums.Condition.PARENT
import me.racci.terix.enums.Condition.WATER
import me.racci.terix.lang.Lang
import me.racci.terix.lang.Origins
import me.racci.terix.origins.OriginFile.DAY_ATTRIBUTES
import me.racci.terix.origins.OriginFile.DAY_EFFECTS
import me.racci.terix.origins.OriginFile.DAY_SOUND
import me.racci.terix.origins.OriginFile.DAY_SUBTITLE
import me.racci.terix.origins.OriginFile.DAY_TITLE
import me.racci.terix.origins.OriginFile.END_ATTRIBUTES
import me.racci.terix.origins.OriginFile.END_EFFECTS
import me.racci.terix.origins.OriginFile.FALL_AMOUNT
import me.racci.terix.origins.OriginFile.FALL_ENABLED
import me.racci.terix.origins.OriginFile.GENERAL_ATTRIBUTES
import me.racci.terix.origins.OriginFile.GENERAL_EFFECTS
import me.racci.terix.origins.OriginFile.GUI_ENABLED
import me.racci.terix.origins.OriginFile.GUI_LORE_ABILITIES
import me.racci.terix.origins.OriginFile.GUI_LORE_DEBUFFS
import me.racci.terix.origins.OriginFile.GUI_LORE_DESCRIPTION
import me.racci.terix.origins.OriginFile.GUI_LORE_PASSIVES
import me.racci.terix.origins.OriginFile.GUI_MATERIAL
import me.racci.terix.origins.OriginFile.GUI_SLOT
import me.racci.terix.origins.OriginFile.IDENTITY_COLOUR
import me.racci.terix.origins.OriginFile.IDENTITY_NAME
import me.racci.terix.origins.OriginFile.LAVA_AMOUNT
import me.racci.terix.origins.OriginFile.LAVA_ATTRIBUTES
import me.racci.terix.origins.OriginFile.LAVA_EFFECTS
import me.racci.terix.origins.OriginFile.LAVA_ENABLED
import me.racci.terix.origins.OriginFile.NETHER_ATTRIBUTES
import me.racci.terix.origins.OriginFile.NETHER_EFFECTS
import me.racci.terix.origins.OriginFile.NIGHT_ATTRIBUTES
import me.racci.terix.origins.OriginFile.NIGHT_EFFECTS
import me.racci.terix.origins.OriginFile.NIGHT_SOUND
import me.racci.terix.origins.OriginFile.NIGHT_SUBTITLE
import me.racci.terix.origins.OriginFile.NIGHT_TITLE
import me.racci.terix.origins.OriginFile.OVERWORLD_ATTRIBUTES
import me.racci.terix.origins.OriginFile.OVERWORLD_EFFECTS
import me.racci.terix.origins.OriginFile.PASSIVES_DIMENSION
import me.racci.terix.origins.OriginFile.PASSIVES_GENERAL
import me.racci.terix.origins.OriginFile.PASSIVES_LIQUID
import me.racci.terix.origins.OriginFile.PASSIVES_TIME
import me.racci.terix.origins.OriginFile.PERMISSION_GIVEN
import me.racci.terix.origins.OriginFile.PERMISSION_REQUIRED
import me.racci.terix.origins.OriginFile.RAIN_AMOUNT
import me.racci.terix.origins.OriginFile.RAIN_ENABLED
import me.racci.terix.origins.OriginFile.SOUND_DEATH
import me.racci.terix.origins.OriginFile.SOUND_HURT
import me.racci.terix.origins.OriginFile.SUN_AMOUNT
import me.racci.terix.origins.OriginFile.SUN_ENABLED
import me.racci.terix.origins.OriginFile.TOGGLES_JUMPBOOST
import me.racci.terix.origins.OriginFile.TOGGLES_NIGHTVISION
import me.racci.terix.origins.OriginFile.TOGGLES_SLOWFALLING
import me.racci.terix.origins.OriginFile.WATER_AMOUNT
import me.racci.terix.origins.OriginFile.WATER_ATTRIBUTES
import me.racci.terix.origins.OriginFile.WATER_EFFECTS
import me.racci.terix.origins.OriginFile.WATER_ENABLED
import me.racci.terix.utils.AttributeUtils
import me.racci.terix.utils.PrivateAttribute
import net.kyori.adventure.text.Component
import org.apache.commons.lang3.EnumUtils
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import java.util.EnumMap

object OriginFactory : IFactory<OriginFactory> {

    fun generate(config: YamlConfiguration): Origin {
        val modifierMaps = EnumMap<Condition, HashMap<Attribute, Double>>(Condition::class.java)
        fun mapAttributes(condition: Condition) {
            val path = when (condition.ordinal) {
                0 -> GENERAL_ATTRIBUTES.path
                1 -> DAY_ATTRIBUTES.path
                2 -> NIGHT_ATTRIBUTES.path
                3 -> WATER_ATTRIBUTES.path
                4 -> LAVA_ATTRIBUTES.path
                5 -> OVERWORLD_ATTRIBUTES.path
                6 -> NETHER_ATTRIBUTES.path
                else -> END_ATTRIBUTES.path
            }
            for (var1 in config.getStringList(path)) {
                val attribute = path.split(":".toRegex()).toTypedArray()
                if (!AttributeUtils.isValid(attribute[0])) return
                val fvar1 = attribute[1].toDoubleOrNull() ?: return
                modifierMaps[condition]?.putIfAbsent(Attribute.valueOf(attribute[0]), fvar1)
            }
        }
        val potions = EnumMap<Condition, ArrayList<PotionEffect>>(Condition::class.java)
        fun listPotions(condition: Condition) {
            val path = when (condition.ordinal) {
                0 -> GENERAL_EFFECTS.path
                1 -> DAY_EFFECTS.path
                2 -> NIGHT_EFFECTS.path
                3 -> WATER_EFFECTS.path
                4 -> LAVA_EFFECTS.path
                5 -> OVERWORLD_EFFECTS.path
                6 -> NETHER_EFFECTS.path
                else -> END_EFFECTS.path
            }
            val fvar1 = ArrayList<PotionEffect>()
            for (potionString in config.getStringList(path)) {
                fvar1.add(PotionFactory.newPotion(potionString, condition) ?: continue)
            }
            potions[condition] = fvar1
        }
        val identities = arrayOf(
            config.getString(IDENTITY_NAME.path) ?: IDENTITY_NAME.default as String,
            colour(config.getString(IDENTITY_COLOUR.path) ?: IDENTITY_COLOUR.default as String),
        )
        val sounds = arrayOf(
            Sound.valueOf(config.getString(SOUND_HURT.path) ?: SOUND_HURT.default as String),
            Sound.valueOf(config.getString(SOUND_DEATH.path) ?: SOUND_DEATH.default as String),
            null,
            null,
        )
        config.getString(DAY_SOUND.path).apply { if (this != null) sounds[2] = Sound.valueOf(this) }
        config.getString(NIGHT_SOUND.path).apply { if (this != null) sounds[3] = Sound.valueOf(this) }
        val messages = arrayOf(
            colour(config.getString(DAY_TITLE.path).orEmpty()),
            colour(config.getString(DAY_SUBTITLE.path).orEmpty()),
            colour(config.getString(NIGHT_TITLE.path).orEmpty()),
            colour(config.getString(NIGHT_SUBTITLE.path).orEmpty()),
        )
        val perms = arrayOf(
            config.getStringList(PERMISSION_REQUIRED.path),
            config.getStringList(PERMISSION_GIVEN.path),
        )
        val enables = booleanArrayOf(
            config.getBoolean(PASSIVES_GENERAL.path, false),
            config.getBoolean(PASSIVES_TIME.path, false),
            config.getBoolean(PASSIVES_LIQUID.path, false),
            config.getBoolean(PASSIVES_DIMENSION.path, false),
            config.getBoolean(SUN_ENABLED.path, false),
            config.getBoolean(FALL_ENABLED.path, false),
            config.getBoolean(RAIN_ENABLED.path, false),
            config.getBoolean(WATER_ENABLED.path, false),
            config.getBoolean(LAVA_ENABLED.path, false),
        )
        val specials = byteArrayOf(
            if (config.getBoolean(TOGGLES_NIGHTVISION.path, false)) 1.toByte() else 0.toByte(),
            if (config.getBoolean(TOGGLES_SLOWFALLING.path, false)) 1.toByte() else 0.toByte(),
            config.getInt(TOGGLES_JUMPBOOST.path, 0).toByte(),
        )
        val damages = intArrayOf(
            config.getInt(SUN_AMOUNT.path, 100),
            config.getInt(FALL_AMOUNT.path, 100),
            config.getInt(RAIN_AMOUNT.path, 100),
            config.getInt(WATER_AMOUNT.path, 100),
            config.getInt(LAVA_AMOUNT.path, 100),
        )
        val guis = GUIItem(
            if (config.getBoolean(GUI_ENABLED.path)) {
                var var1: Any?
                val var2: Any?
                var1 = config.getString(GUI_MATERIAL.path)
                var1 = if (EnumUtils.isValidEnum(Material::class.java, var1)) {
                    Material.valueOf(var1 ?: "BARRIER")
                } else {
                    ItemBuilder.head {
                        texture = var1 as String
                    }
                }
                var2 = ArrayList<Component>()
                var2.add(Component.empty())
                if (config.get(GUI_LORE_DESCRIPTION.path) != null) {
                    config.getStringList(GUI_LORE_DESCRIPTION.path).forEach { var1x -> textOf(colour(var1x, true)) }
                    var2.add(Component.empty())
                }
                if (config.get(GUI_LORE_PASSIVES.path) != null) {
                    var2.add(textOf(Lang[Origins.LORE_PASSIVES]))
                    config.getStringList(GUI_LORE_PASSIVES.path).forEach { var1x -> textOf(colour("              $var1x", true)) }
                    var2.add(Component.empty())
                }
                if (config.get(GUI_LORE_ABILITIES.path) != null) {
                    var2.add(textOf(Lang[Origins.LORE_ABILITIES]))
                    config.getStringList(GUI_LORE_ABILITIES.path).forEach { var1x -> textOf(colour("              $var1x", true)) }
                    var2.add(Component.empty())
                }
                if (config.get(GUI_LORE_DEBUFFS.path) != null) {
                    var2.add(textOf(Lang[Origins.LORE_DEBUFFS]))
                    config.getStringList(GUI_LORE_DEBUFFS.path).forEach { var1x -> textOf(colour("              $var1x", true)) }
                    var2.add(Component.empty())
                }
                ItemBuilder.from(var1 as ItemStack) {
                    amount = 1
                    glow()
                    nbt = "GUIItem" to true
                    name = textOf(identities[0])
                    lore = var2 as Component
                }
            } else {
                ItemBuilder.from(Material.BEDROCK) {
                    nbt = "GUIItem" to false
                }
            },
            config.getInt(GUI_SLOT.path, 0)
        )
        val baseAttributes = EnumMap<Attribute, Double>(Attribute::class.java)

        if (enables[0]) {
            for (string in config.getStringList(GENERAL_ATTRIBUTES.path)) {
                val attribute = string.split(":".toRegex()).toTypedArray()
                val fvar1 = if (AttributeUtils.isValid(attribute[0])) PrivateAttribute.valueOf(attribute[0]) else continue
                if ((attribute[1].toDoubleOrNull() ?: continue) !in fvar1.minLevel..fvar1.maxLevel) continue
                baseAttributes[Attribute.valueOf(attribute[0])] = attribute[1].toDouble()
            }
            listPotions(PARENT)
        }
        if (enables[1]) {
            mapAttributes(DAY)
            mapAttributes(NIGHT)
            listPotions(DAY)
            listPotions(NIGHT)
        }
        if (enables[2]) {
            mapAttributes(WATER)
            mapAttributes(LAVA)
            listPotions(WATER)
            listPotions(LAVA)
        }
        if (enables[3]) {
            mapAttributes(OVERWORLD)
            mapAttributes(NETHER)
            mapAttributes(END)
            listPotions(OVERWORLD)
            listPotions(NETHER)
            listPotions(END)
        }
        val attributes = EnumMap<Condition, AttributeCondition>(Condition::class.java)
        for (condition in Condition.values()) {
            val fvar2 = AttributeFactory.newCondition(condition, modifierMaps[condition] ?: LinkedHashMap())
            attributes[condition] = fvar2
            if (condition.ordinal == 7) break
        }

        for (fvar1 in Condition.values()) {
            potions.putIfAbsent(fvar1, ArrayList())
        }

        return Origin(
            identities,
            sounds,
            messages,
            perms,
            enables,
            specials,
            damages,
            guis,
            baseAttributes,
            attributes,
            potions,
        )
    }

    override fun init() {
        TODO("Not yet implemented")
    }

    override fun reload() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

data class Origin(
    private val identities: Array<String>,
    private val sounds: Array<org.bukkit.Sound?>,
    private val messages: Array<String>,
    private val perms: Array<List<String>>,
    private val enables: BooleanArray,
    private val specials: ByteArray,
    private val damages: IntArray,
    private val guis: GUIItem,
    val baseAttributes: EnumMap<Attribute, Double>,
    val attributes: EnumMap<Condition, AttributeCondition>,
    val potions: EnumMap<Condition, ArrayList<PotionEffect>>,
) {

    val identity = Identity()
    val sound = Sound()
    val message = Message()
    val permissions = Permissions()
    val enable = Enables()
    val special = Specials()
    val damage = Damage()
    val gui = GUI()

    inner class Identity {
        val name get() = identities[0]
        val colour get() = identities[1]
        val displayName get() = "$colour$name"
        val bracketName get() = "$colour[$name]"
    }

    inner class Sound {
        val hurtSound get() = sounds[0]
        val deathSound get() = sounds[1]
        val daySound get() = sounds[2]
        val nightSound get() = sounds[3]
    }

    inner class Message {
        val dayTitle get() = messages[0]
        val daySubtitle get() = messages[1]
        val nightTitle get() = messages[2]
        val nightSubtitle get() = messages[3]
    }

    inner class Permissions {
        val required get() = perms[0]
        val given get() = perms[1]
    }

    inner class Enables {
        val passives get() = enables[0]
        val time get() = enables[1]
        val liquid get() = enables[2]
        val dimension get() = enables[3]
        val sun get() = enables[4]
        val fall get() = enables[5]
        val rain get() = enables[6]
        val water get() = enables[7]
        val lava get() = enables[8]
    }

    inner class Specials {
        val nightVision get() = when (specials[0]) {
            0.toByte() -> false
            else -> true
        }
        val slowFalling get() = when (specials[1]) {
            0.toByte() -> false
            else -> true
        }
        val jumpBoost get() = specials[2].toInt()
    }

    inner class Damage {
        val sun get() = damages[0]
        val fall get() = damages[1]
        val rain get() = damages[2]
        val water get() = damages[3]
        val lava get() = damages[4]
    }

    inner class GUI {
        val item get() = guis.item
        val slot get() = guis.slot
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Origin

        if (!identities.contentEquals(other.identities)) return false
        if (!sounds.contentEquals(other.sounds)) return false
        if (!messages.contentEquals(other.messages)) return false
        if (!perms.contentEquals(other.perms)) return false
        if (!enables.contentEquals(other.enables)) return false
        if (!specials.contentEquals(other.specials)) return false
        if (!damages.contentEquals(other.damages)) return false
        if (baseAttributes != other.baseAttributes) return false
        if (attributes != other.attributes) return false
        if (potions != other.potions) return false
        if (identity != other.identity) return false
        if (sound != other.sound) return false
        if (message != other.message) return false
        if (permissions != other.permissions) return false
        if (enable != other.enable) return false
        if (special != other.special) return false
        if (damage != other.damage) return false
        if (gui != other.gui) return false

        return true
    }

    override fun hashCode(): Int {
        var result = identities.contentHashCode()
        result = 31 * result + sounds.contentHashCode()
        result = 31 * result + messages.contentHashCode()
        result = 31 * result + perms.contentHashCode()
        result = 31 * result + enables.contentHashCode()
        result = 31 * result + specials.contentHashCode()
        result = 31 * result + damages.contentHashCode()
        result = 31 * result + baseAttributes.hashCode()
        result = 31 * result + attributes.hashCode()
        result = 31 * result + potions.hashCode()
        result = 31 * result + identity.hashCode()
        result = 31 * result + sound.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + permissions.hashCode()
        result = 31 * result + enable.hashCode()
        result = 31 * result + special.hashCode()
        result = 31 * result + damage.hashCode()
        result = 31 * result + gui.hashCode()
        return result
    }
}
