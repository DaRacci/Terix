package me.racci.sylphia.enums.originspaths;

public enum OriginPaths implements OriginPath {

	Name,
	OriginColour,
	HurtSound,
	DeathSound,
	DayNight_Day,
	DayNight_Night,
	Effects_Enables_General8Enabled,
	Effects_Enables_Night9Day8Enabled,
	Effects_Enables_Water9Lava8Enabled,
	Effects_Effects_Slow8falling,
	Effects_Effects_Night8vision,
	Effects_Effects_Jump8boost;




	public String getPath() {
		String name = this.name().replace("_", ".");
		name = name.replace("8", "-");
		name = name.replace("9", "/");
		return name;
	}


//	public static List<OriginPaths> getOrderedValues() {
//		List<OriginPaths> list = new ArrayList<>();
//		list.add(OriginPaths.BANS);
//		list.add(OriginPaths.KICKS);
//		list.add(OriginPaths.MUTES);
//		list.add(OriginPaths.WARNS);
//		return list;
//	}
	
}
