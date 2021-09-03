package me.racci.sylphia.listeners;

import co.aikar.taskchain.TaskChain;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerManager;
import me.racci.sylphia.origins.OriginHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinLeaveEvent implements Listener {
	Sylphia plugin;

	public PlayerJoinLeaveEvent(Sylphia plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		OriginHandler originHandler = plugin.getOriginHandler();
		PlayerManager playerManager = plugin.getPlayerManager();
		TaskChain<?> chain = Sylphia.newSharedChain("Join: " + player.getName().toUpperCase());
		chain
			.async(() -> {
				if (playerManager.getPlayerData(player) == null) {
					plugin.getStorageProvider().load(player);
				}
			}).delay(15)
			.sync(() -> originHandler.setTest(player))
			.execute();
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		event.quitMessage(null);
		Player player = event.getPlayer();
		Sylphia.newChain().async(() -> plugin.getStorageProvider().save(player, true));
	}
}
