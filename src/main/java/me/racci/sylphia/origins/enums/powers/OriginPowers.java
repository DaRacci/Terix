package me.racci.sylphia.origins.enums.powers;

import me.racci.sylphia.lang.MessageKey;

public enum OriginPowers implements MessageKey {

	// Format

	// Vampire
	DARKFLIGHT_NAME("Powers.Vampire.Darkflight.Name"),
	DARKFLIGHT_DESC("Powers.Vampire.Darkflight.Desc"),
	DARKFLIGHT_INFO("Powers.Vampire.Darkflight.Info"),
	DARKFLIGHT_USE("Powers.Vampire.Darkflight.Use"),
	DARKSHADOW_NAME("Powers.Vampire.DarkShadow.Name"),
	DARKSHADOW_DESC("Powers.Vampire.DarkShadow.Desc"),
	DARKSHADOW_INFO("Powers.Vampire.DarkShadow.Info"),
	DARKSHADOW_USE("Powers.Vampire.DarkShadow.Use"),
	DARKCONVERSION_NAME("Powers.Vampire.Darkconversion.Name"),
	DARKCONVERSION_DESC("Powers.Vampire.Darkconversion.Desc"),
	DARKCONVERSION_INFO("Powers.Vampire.Darkconversion.Info"),
	DARKCONVERSION_USE("Powers.Vampire.Darkconversion.Use");

	private final String path;

	OriginPowers(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}