package me.racci.sylphia.origins.enums.options;

public enum Path {


	NAME("Name", PathType.STRING),
	COLOUR("Colour", PathType.STRING),

	HURT_SOUND("Hurt-Sound", PathType.SOUND),
	DEATH_SOUND("Death-Sound", PathType.SOUND),

	PERMISSION_REQUIRED("Permissions.Required", PathType.LIST),
	PERMISSION_GIVEN("Permissions.Given", PathType.LIST),

	DAY_MESSAGE("Day/Night.Day", PathType.STRING),
	NIGHT_MESSAGE("Day/Night.Night", PathType.STRING),

	GENERAL_ENABLE("Effects.Enables.General-Enabled", PathType.BOOLEAN),
	TIME_ENABLE("Effects.Enables.Day/Night-Enabled", PathType.BOOLEAN),
	LIQUID_ENABLE("Effects.Enables.Water/Lava-Enabled", PathType.BOOLEAN),

	GENERAL_EFFECTS("Effects.General.Effects", PathType.EFFECT),
	GENERAL_ATTRIBUTES("Effects.General.Attributes", PathType.ATTRIBUTE),
	DAY_EFFECTS("Effects.Day/Night.Day-Effects", PathType.EFFECT),
	DAY_ATTRIBUTES("Effects.Day/Night.Day-Attributes", PathType.ATTRIBUTE),
	NIGHT_EFFECTS("Effects.Day/Night.Night-Effects", PathType.EFFECT),
	NIGHT_ATTRIBUTES("Effects.Day/Night.Night-Attributes", PathType.ATTRIBUTE),
	WATER_EFFECTS("Effects.Water/Lava.Water-Effects", PathType.EFFECT),
	WATER_ATTRIBUTES("Effects.Water/Lava.Water-Attributes", PathType.ATTRIBUTE),
	LAVA_EFFECTS("Effects.Water/Lava.Lava-Effects", PathType.EFFECT),
	LAVA_ATTRIBUTES("Effects.Water/Lava.Lava-Attributes", PathType.ATTRIBUTE),

	SLOWFALLING("Effects.Effects.Slow-falling", PathType.INT),
	NIGHTVISION("Effects.Effects.Night-vision", PathType.INT),
	JUMPBOOST("Effects.Effects.Jump-boost", PathType.INT),

	DAMAGE_ENABLE("Damage.Enables.General-Enabled", PathType.BOOLEAN),
	SUN_ENABLE("Damage.Enables.Sun-Enabled", PathType.BOOLEAN),
	FALL_ENABLE("Damage.Enables.Fall-Enabled", PathType.BOOLEAN),
	RAIN_ENABLE("Damage.Enables.Rain-Enabled", PathType.BOOLEAN),
	WATER_ENABLE("Damage.Enables.Water-Enabled", PathType.BOOLEAN),
	LAVA_ENABLE("Damage.Enables.Lava-Enabled", PathType.BOOLEAN),

	SUN_AMOUNT("Damage.Amounts.Sun", PathType.INT),
	FALL_AMOUNT("Damage.Amounts.Fall", PathType.INT),
	RAIN_AMOUNT("Damage.Amounts.Rain", PathType.INT),
	WATER_AMOUNT("Damage.Amounts.Water", PathType.INT),
	LAVA_AMOUNT("Damage.Amounts.Lava", PathType.INT),

	GUI_ENCHANTED("GUI.Enchanted", PathType.BOOLEAN),
	GUI_SKULL("GUI.Skull", PathType.STRING),
	GUI_MATERIAL("GUI.Material", PathType.MATERIAL),
	GUI_LORE("GUI.Lore", PathType.LIST);


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
