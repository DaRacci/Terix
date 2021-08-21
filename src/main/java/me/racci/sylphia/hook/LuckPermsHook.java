package me.racci.sylphia.hook;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;


@SuppressWarnings("unused")
public class LuckPermsHook {

	private final LuckPerms luckPerms;

	public LuckPermsHook() {
		luckPerms = LuckPermsProvider.get();
	}

	public void addPermission(Player player, String permission, boolean value) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
				user.data().add(Node.builder(permission).value(value).build()));
	}

	public void addPermission(Player player, String permission) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
				user.data().add(Node.builder(permission).value(true).build()));
	}

	public void removePermission(Player player, String permission, boolean value) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
				user.data().remove(Node.builder(permission).value(value).build()));
	}


}

