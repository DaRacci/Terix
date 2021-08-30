package me.racci.sylphia.lang;

import org.apache.commons.text.WordUtils;

public enum Colours implements MessageKey {


	PLAYER,
	MISSING;

	private final String path;

	Colours() {
		this.path = "Messages.Commands." + WordUtils.capitalizeFully(this.name());
	}

	public String getPath() {
		return path;
	}

}
