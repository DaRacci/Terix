package me.racci.sylphia.hook.perms;

import org.bukkit.entity.Player;

import java.util.List;

public interface PermManager {

	void addPermission(Player player, List<String> permissions);

	void addPermission(Player player, String permission);

	void removePermission(Player player, List<String> permissions);

	void removePermission(Player player, String permission);
}
