package me.racci.sylphia.data.storage;


import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.data.PlayerDataLoadEvent;
import me.racci.sylphia.data.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class StorageProvider {

	public final Sylphia plugin;
	public final PlayerManager playerManager;

	public StorageProvider(Sylphia plugin) {
		this.playerManager = plugin.getPlayerManager();
		this.plugin = plugin;
	}

	public PlayerData createNewPlayer(Player player) {
		PlayerData playerData = new PlayerData(player, plugin);
		playerManager.addPlayerData(playerData);
		PlayerDataLoadEvent event = new PlayerDataLoadEvent(playerData);
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

//	protected void sortLeaderboards(Map<Origin, List<SkillValue>> leaderboards, List<SkillValue> powerLeaderboard, List<SkillValue> averageLeaderboard) {
//		LeaderboardManager manager = plugin.getLeaderboardManager();
//		LeaderboardSorter sorter = new LeaderboardSorter();
//		for (Origin origin : Origins.values()) {
//			leaderboards.get(origin).sort(sorter);
//		}
//		powerLeaderboard.sort(sorter);
//		AverageSorter averageSorter = new AverageSorter();
//		averageLeaderboard.sort(averageSorter);
//
//		// Add origin leaderboards to map
//		for (Origin origin : Origins.values()) {
//			manager.setLeaderboard(origin, leaderboards.get(origin));
//		}
//		manager.setPowerLeaderboard(powerLeaderboard);
//		manager.setAverageLeaderboard(averageLeaderboard);
//		manager.setSorting(false);
//	}

	public abstract void load(Player player);

	public abstract void save(Player player);

	public abstract void save(Player player, boolean removeFromMemory);

}
