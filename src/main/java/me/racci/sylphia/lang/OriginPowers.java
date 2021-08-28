package me.racci.sylphia.lang;

import me.racci.sylphia.enums.origins.Origin;
import me.racci.sylphia.enums.origins.Origins;

public enum OriginPowers implements MessageKey{

	// Format

	// Vampire
	DARKFLIGHT_NAME,
	DARKFLIGHT_DESC,
	DARKFLIGHT_INFO,
	DARKFLIGHT_USE,
	DARKSHADOW_NAME,
	DARKSHADOW_DESC,
	DARKSHADOW_INFO,
	DARKSHADOW_USE,
	DARKCONVERSION_NAME,
	DARKCONVERSION_DESC,
	DARKCONVERSION_INFO,
	DARKCONVERSION_USE;

	private final String path;

	OriginPowers() {
		Origin origin;
		try {
			origin = Origins.valueOf(this.name().substring(0, this.name().lastIndexOf("_")));
		}
		catch (IllegalArgumentException e) {
			origin = Origins.valueOf(this.name().substring(0, this.name().indexOf("_")));
		}
		path = "origins." + origin.getPower().toLowerCase() + "." + origin.name().toLowerCase() + "." + this.name().substring(origin.name().length() + 1).toLowerCase();
	}

	public String getPath() {
		return path;
	}
}