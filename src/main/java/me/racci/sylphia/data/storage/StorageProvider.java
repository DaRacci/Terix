package me.racci.sylphia.data.storage;


import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.events.DataLoadEvent;
import me.racci.sylphia.data.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class StorageProvider {

	public final Sylphia plugin;
	public final PlayerManager playerManager;

	protected StorageProvider(Sylphia plugin) {
		this.playerManager = plugin.getPlayerManager();
		this.plugin = plugin;
	}

	public PlayerData createNewPlayer(Player player) {
		PlayerData playerData = new PlayerData(player, plugin);
		playerManager.addPlayerData(playerData);
		DataLoadEvent event = new DataLoadEvent(playerData);
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(event);
			}
		}.runTask(plugin);
		return playerData;
	}

	protected void sendErrorMessageToPlayer(Player player, Exception e) {
		player.sendMessage(ChatColor.RED + "There was an error loading your origin data: " + e.getMessage() +
				". Please report the error to your server administrator. To prevent your data from resetting permanently" +
				", your origin data will not be saved. Try relogging to attempt loading again.");
	}

	public abstract void load(Player player);

	public abstract void save(Player player);

	public abstract void save(Player player, boolean removeFromMemory);

}
