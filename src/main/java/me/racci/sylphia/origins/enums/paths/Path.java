package me.racci.sylphia.origins.enums.paths;

public enum Path {


	NAME("Identity.Name", PathType.STRING),
	COLOUR("Identity.Colour", PathType.STRING),

	HURT("Sound.Hurt", PathType.SOUND),
	DEATH("Sound.Death", PathType.SOUND),

	REQUIRED("Permissions.Required", PathType.LIST),
	GIVEN("Permissions.Given", PathType.LIST),

	DAY_TITLE("TimeMessage.Day.Title", PathType.STRING),
	DAY_SUBTITLE("TimeMessages.Day.Subtitle", PathType.STRING),
	NIGHT_TITLE("TimeMessage.Night.Title", PathType.STRING),
	NIGHT_SUBTITLE("TimeMessages.Night.Subtitle", PathType.STRING),

	GENERAL_ENABLE("Effects.Enabled", PathType.BOOLEAN),
	TIME_ENABLE("Effects.Time", PathType.BOOLEAN),
	LIQUID_ENABLE("Effects.Liquid", PathType.BOOLEAN),
	DIMENSION_ENABLE("Effects.Dimension", PathType.BOOLEAN),

	GENERAL_EFFECTS("Effects.General.Effects", PathType.EFFECT),
	GENERAL_ATTRIBUTES("Effects.General.Attributes", PathType.ATTRIBUTE),
	DAY_EFFECTS("Effects.Day.Effects", PathType.EFFECT),
	DAY_ATTRIBUTES("Effects.Day.Attributes", PathType.ATTRIBUTE),
	NIGHT_EFFECTS("Effects.Night.Effects", PathType.EFFECT),
	NIGHT_ATTRIBUTES("Effects.Night.Attributes", PathType.ATTRIBUTE),
	WATER_EFFECTS("Effects.Water.Effects", PathType.EFFECT),
	WATER_ATTRIBUTES("Effects.Water.Attributes", PathType.ATTRIBUTE),
	LAVA_EFFECTS("Effects.Lava.Effects", PathType.EFFECT),
	LAVA_ATTRIBUTES("Effects.Lava.Attributes", PathType.ATTRIBUTE),
	OVERWORLD_EFFECTS("Effects.Overworld.Effects", PathType.EFFECT),
	OVERWORLD_ATTRIBUTES("Effects.Overworld.Attributes", PathType.ATTRIBUTE),
	NETHER_EFFECTS("Effects.Nether.Effects", PathType.EFFECT),
	NETHER_ATTRIBUTES("Effects.Nether.Attributes", PathType.ATTRIBUTE),
	END_EFFECTS("Effects.End.Effects", PathType.EFFECT),
	END_ATTRIBUTES("Effects.End.Attributes", PathType.ATTRIBUTE),

	SLOWFALLING("Effects.Effects.Slow-falling", PathType.BOOLEAN),
	NIGHTVISION("Effects.Effects.Night-vision", PathType.BOOLEAN),
	JUMPBOOST("Effects.Effects.Jump-boost", PathType.INT),

	DAMAGE_ENABLE("Damage.Enabled", PathType.BOOLEAN),
	SUN_ENABLE("Damage.Sun", PathType.BOOLEAN),
	FALL_ENABLE("Damage.Fall", PathType.BOOLEAN),
	RAIN_ENABLE("Damage.Rain", PathType.BOOLEAN),
	WATER_ENABLE("Damage.Water", PathType.BOOLEAN),
	LAVA_ENABLE("Damage.Lava", PathType.BOOLEAN),

	SUN_AMOUNT("Damage.Amounts.Sun", PathType.INT),
	FALL_AMOUNT("Damage.Amounts.Fall", PathType.INT),
	RAIN_AMOUNT("Damage.Amounts.Rain", PathType.INT),
	WATER_AMOUNT("Damage.Amounts.Water", PathType.INT),
	LAVA_AMOUNT("Damage.Amounts.Lava", PathType.INT),

	GUI_SKULL("GUI.Item.Skull", PathType.STRING),
	GUI_MATERIAL("GUI.Item.Material", PathType.MATERIAL),
	GUI_ENCHANTED("GUI.Item.Enchanted", PathType.BOOLEAN),
	GUI_LORE_DESCRIPTION("GUI.Lore.Description", PathType.LIST),
	GUI_LORE_PASSIVES("GUI.Lore.Passives", PathType.LIST),
	GUI_LORE_ABILITIES("GUI.Lore.Abilities", PathType.LIST),
	GUI_LORE_DEBUFFS("GUI.Lore.Debuffs", PathType.LIST);


	private final String path;
	private final PathType type;

	Path(String path, PathType type) {
		this.path = path;
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public PathType getType() {
		return type;
	}

	
}
