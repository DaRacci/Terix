package me.racci.sylphia.hook.perms;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import org.bukkit.entity.Player;

import java.util.List;


@SuppressWarnings("unused")
public class LuckPermsHook implements PermManager {

	private final LuckPerms luckPerms;

	public LuckPermsHook() {
		luckPerms = LuckPermsProvider.get();
	}

	@Override
	public void addPermission(Player player, String permission) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
			user.data().add(Node.builder(permission).value(true).build()));
	}

	@Override
	public void removePermission(Player player, String permission) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user ->
			user.data().remove(Node.builder(permission).value(true).build()));
	}


	@Override
	public void addPermission(Player player, List<String> permissions) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> permissions.forEach(perm ->
			user.data().add(Node.builder(perm).value(true).build())));
	}

	@Override
	public void removePermission(Player player, List<String> permissions) {
		luckPerms.getUserManager().modifyUser(player.getUniqueId(), user -> permissions.forEach(perm ->
			user.data().remove(Node.builder(perm).value(true).build())));
	}
}

