package me.racci.sylphia.origins

import me.racci.sylphia.enums.ValueType
import me.racci.sylphia.enums.ValueType.*
import org.bukkit.Material.BARRIER

enum class OriginFile (val path: String,
                       val type: ValueType,
                       val default: Any?,
                       val optional: Boolean = false) {

    IDENTITY_NAME("Identity.Name", STRING, "Null"),
    IDENTITY_COLOUR("Identity.Colour", STRING, ""),

    SOUND_HURT("_Sound.Hurt", SOUND, "ENTITY_PLAYER_HURT"),
    SOUND_DEATH("_Sound.Death", SOUND, "ENTITY_PLAYER_DEATH"),

    PERMISSION_REQUIRED("Permissions.Required", STRING, null, true),
    PERMISSION_GIVEN("Permission.Given", LIST, null, true),

    DAY_TITLE("Time-Message.Day.Title", STRING, "", true),
    DAY_SUBTITLE("Time-Message.Day.Subtitle", STRING, "", true),
    DAY_SOUND("Time-Message.Day._Sound", SOUND, "", true),

    NIGHT_TITLE("Time-Message.Night.Title", STRING, "", true),
    NIGHT_SUBTITLE("Time-Message.Night.Subtitle", STRING, "", true),
    NIGHT_SOUND("Time-Message.Night._Sound", SOUND, "", true),

    PASSIVES_GENERAL("Passives.Enabled", BOOLEAN, false),
    PASSIVES_TIME("Passives.Time", BOOLEAN, false),
    PASSIVES_LIQUID("Passives.Liquid", BOOLEAN, false),
    PASSIVES_DIMENSION("Passives.Dimension", BOOLEAN, false),

    TOGGLES_SLOWFALLING("Passives.Toggle.Slow-falling", BOOLEAN, 0),
    TOGGLES_NIGHTVISION("Passives.Toggle.Night-vision", BOOLEAN, 0),
    TOGGLES_JUMPBOOST("Passives.Toggle.Jump-boost", BOOLEAN, 0),

    GENERAL_EFFECTS("Passives.General.Effects", LIST, null, true),
    DAY_EFFECTS("Passives.Day.Effects", LIST, null, true),
    NIGHT_EFFECTS("Passives.Night.Effects", LIST, null, true),
    WATER_EFFECTS("Passives.Water.Effects", LIST, null, true),
    LAVA_EFFECTS("Passives.Lava.Effects", LIST, null, true),
    OVERWORLD_EFFECTS("Passives.Overworld.Effects", LIST, null, true),
    NETHER_EFFECTS("Passives.Nether.Effects", LIST, null, true),
    END_EFFECTS("Passives.End.Effects", LIST, null, true),

    GENERAL_ATTRIBUTES("Passives.General.Attributes", LIST, null, true),
    DAY_ATTRIBUTES("Passives.Day.Attributes", LIST, null, true),
    NIGHT_ATTRIBUTES("Passives.Night.Attributes", LIST, null, true),
    WATER_ATTRIBUTES("Passives.Water.Attributes", LIST, null, true),
    LAVA_ATTRIBUTES("Passives.Lava.Attributes", LIST, null, true),
    OVERWORLD_ATTRIBUTES("Passives.Overworld.Attributes", LIST, null, true),
    NETHER_ATTRIBUTES("Passives.Nether.Attributes", LIST, null, true),
    END_ATTRIBUTES("Passives.End.Attributes", LIST, null, true),

    SUN_ENABLED("Damage.Sun", BOOLEAN, false),
    FALL_ENABLED("Damage.Fall", BOOLEAN, false),
    RAIN_ENABLED("Damage.Rain", BOOLEAN, false),
    WATER_ENABLED("Damage.Water", BOOLEAN, false),
    LAVA_ENABLED("Damage.Lava", BOOLEAN, false),

    SUN_AMOUNT("Damage.Amounts.Sun", INT, 0),
    FALL_AMOUNT("Damage.Amounts.Fall", INT, 100),
    RAIN_AMOUNT("Damage.Amounts.Rain", INT, 0),
    WATER_AMOUNT("Damage.Amounts.Water", INT, 0),
    LAVA_AMOUNT("Damage.Amounts.Lava", INT, 100),

    GUI_ENABLED("Gui.Enabled", BOOLEAN, false),
    GUI_SLOT("Gui.Item.Slot", INT, 10),
    GUI_MATERIAL("Gui.Item.Material", MATERIAL_HEAD, BARRIER),
    GUI_GLOW("Gui.Item.Glow", BOOLEAN, false, true),
    GUI_LORE_DESCRIPTION("Gui.Lore.Description", LIST, null, true),
    GUI_LORE_PASSIVES("Gui.Lore.Passives", LIST, null, true),
    GUI_LORE_ABILITIES("Gui.Lore.Abilities", LIST, null, true),
    GUI_LORE_DEBUFFS("Gui.Lore.Debuffs", LIST, null, true);

}

