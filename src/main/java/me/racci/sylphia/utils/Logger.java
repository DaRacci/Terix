package me.racci.sylphia.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class Logger {
	public static void log(Level level, String message) {
		if (message == null) return;
		switch (level) {
			case ERROR -> Bukkit.getConsoleSender().sendMessage(ChatColor.color("&8[&c&lERROR&r&8] &f" + message));
			case WARNING -> Bukkit.getConsoleSender().sendMessage(ChatColor.color("&8[&6&lWARNING&r&8] &f" + message));
			case INFO -> Bukkit.getConsoleSender().sendMessage(ChatColor.color("&8[&e&lINFO&r&8] &f" + message));
			case SUCCESS -> Bukkit.getConsoleSender().sendMessage(ChatColor.color("&8[&a&lSUCCESS&r&8] &f" + message));
			case OUTLINE -> Bukkit.getConsoleSender().sendMessage(ChatColor.color("&7" + message));
		}
	}

	public enum Level { ERROR, WARNING, INFO, SUCCESS, OUTLINE }
}