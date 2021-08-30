package me.racci.sylphia.lang;

import org.bukkit.ChatColor;

public enum OriginMessage implements MessageKey {

	COMMAND_GET("Origins.Command.Get"),
	COMMAND_SET("Origins.Command.Set"),
	COMMAND_UNSET("Origins.Command.Unset"),

	RESULT_NULL("Origins.Lore.NotNull"),

	SELECT_BROADCAST("Origins.Select.Broadcast"),
	SELECT_LOCKED("Origins.Select.Locked"),
	SELECT_CURRENT("Origins.Select.Current"),

	LORE_CENTERED("Origins.Lore.Centered"),
	LORE_INDENT("Origins.Lore.Indent"),
	LORE_PASSIVES("Origins.Lore.Passives"),
	LORE_ABILITIES("Origins.Lore.Abilities"),
	LORE_DEBUFFS("Origins.Lore.Debuffs"),
	LORE_SELECT("Origins.Lore.Select");

	private final String path;

	OriginMessage(String path) {
		this.path = path;
	}

	public String getPath() {
		return ChatColor.color(path, true);
	}









}
