package me.racci.sylphia.lang;

import org.bukkit.ChatColor;

@SuppressWarnings("unused")
public enum Prefix implements MessageKey {

	// Sylphia »
	SYLPHIA,
	SYLPHIA_BOLD,
	// Error »
	ERROR,
	ERROR_BOLD,
	// Origins »
	ORIGINS,
	ORIGINS_BOLD;

	private final String path;

	Prefix() {
		this.path = "Prefixes." + this.name();
	}

	Prefix(String path) {
		this.path = "Prefixes." + path;
	}

	public String getPath() {
		return ChatColor.color(path, true);
	}









}
