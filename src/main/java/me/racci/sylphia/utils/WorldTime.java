package me.racci.sylphia.utils;

import org.bukkit.entity.Player;

public class WorldTime {

	public boolean isDay(Player player) {
		long time = player.getWorld().getTime();
		return time < 13400 || time > 23400;
	}

	public boolean isNight(Player player) {
		return !this.isDay(player);
	}

}
