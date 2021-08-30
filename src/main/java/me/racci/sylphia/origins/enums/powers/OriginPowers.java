package me.racci.sylphia.origins.enums.powers;

import me.racci.sylphia.lang.MessageKey;
import me.racci.sylphia.origins.enums.Origin;
import me.racci.sylphia.origins.enums.Origins;

public enum OriginPowers implements MessageKey {

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
		path = "origins." + origin.getPowers().toLowerCase() + "." + origin.getName().toLowerCase() + "." + this.name().substring(origin.getName().length() + 1).toLowerCase();
	}

	public String getPath() {
		return path;
	}
}