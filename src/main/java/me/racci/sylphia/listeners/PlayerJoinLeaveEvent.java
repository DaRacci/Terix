package me.racci.sylphia.listeners;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerManager;
import me.racci.sylphia.origins.OriginHandler;
import me.racci.sylphia.utils.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

@SuppressWarnings("unused")
public class PlayerJoinLeaveEvent implements Listener {
	Sylphia plugin;

	public PlayerJoinLeaveEvent(Sylphia plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		event.joinMessage(null);
		Player player = event.getPlayer();
		OriginHandler originHandler = plugin.getOriginHandler();
		PlayerManager playerManager = plugin.getPlayerManager();
		if (playerManager.getPlayerData(player) == null) {
			loadPlayerDataAsync(player);
		}
		applyEffects(player, playerManager, originHandler);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		event.quitMessage(null);
		Player player = event.getPlayer();
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.getStorageProvider().save(player);
			}
		}.runTaskAsynchronously(plugin);
	}

	private void loadPlayerDataAsync(Player player) {
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.getStorageProvider().load(player);
			}
		}.runTaskAsynchronously(plugin);
	}

	private void applyEffects(Player player, PlayerManager playerManager, OriginHandler originHandler) {
		new BukkitRunnable() {
			@Override
			public void run() {
				if (playerManager.getPlayerData(player) != null) {
//					originHandler.applyGeneral(player);
					Logger.log(Logger.Level.INFO, "Applied effects for " + player.getName() + " on join.");
				} else {
					applyEffects(player, playerManager, originHandler);
					Logger.log(Logger.Level.INFO, "Rescheduling task for applying effects for " + player.getName());
				}
			}
		}.runTaskLater(plugin, 15);
	}
}
