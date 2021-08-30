package me.racci.sylphia.utils;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldTime {

	private WorldTime() {
	}

	public static boolean isDay(@NotNull Player player) {
		long time = player.getWorld().getTime();
		return time < 13400 || time > 23400;
	}
}
