package me.racci.sylphia.lang;

import org.apache.commons.text.WordUtils;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

public enum Prefix implements MessageKey {

	SYLPHIA,
	ERROR,
	ORIGINS;

	private final String path;

	Prefix() {
		this.path = "Prefixes." + WordUtils.capitalizeFully(this.name().toLowerCase());
	}

	@NotNull
	public String getPath() {
		return ChatColor.color(path, true);
	}









}
