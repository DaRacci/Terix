package me.racci.sylphia.data.storage;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.data.PlayerDataLoadEvent;
import me.racci.sylphia.enums.originsettings.OriginSetting;
import me.racci.sylphia.enums.originsettings.OriginSettings;
import me.racci.sylphia.enums.punishments.Punishment;
import me.racci.sylphia.enums.punishments.Punishments;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.UUID;

public class YamlStorageProvider extends StorageProvider {

	public YamlStorageProvider(Sylphia plugin) {
		super(plugin);
	}

	@Override
	public void load(Player player) {
		File file = new File(plugin.getDataFolder() + "/Players/" + player.getUniqueId() + ".yml");
		if (file.exists()) {
			FileConfiguration config = YamlConfiguration.loadConfiguration(file);
			PlayerData playerData = new PlayerData(player, plugin);
			try {
				// Make sure file name and uuid match
				UUID id = UUID.fromString(config.getString("uuid", player.getUniqueId().toString()));
				if (!player.getUniqueId().equals(id)) {
					throw new IllegalArgumentException("File name and uuid field do not match!");
				}
				// Load origin data
				playerData.setOrigin(config.getString("Origins.Origin"));
				playerData.setLastOrigin(config.getString("Origins.LastOrigin"));
				for (OriginSetting originSetting : OriginSettings.values()) {
					String path = "Settings." + originSetting.name().toUpperCase();
					int value = config.getInt(path, 1);
					playerData.setOriginSetting(originSetting, value);
				}
				for (Punishment punishment : Punishments.values()) {
					String path = "Punishments." + punishment.name().toUpperCase();
					int value = config.getInt(path, 0);
					playerData.setPunishments(punishment, value);
				}
				playerData.setUnban(config.getBoolean("Punishments.PurchasedUnban", false));
				playerData.setSilentmode(config.getBoolean("Staff.Silentmode", false));
				playerManager.addPlayerData(playerData);
				PlayerDataLoadEvent event = new PlayerDataLoadEvent(playerData);
				new BukkitRunnable() {
					@Override
					public void run() {
						Bukkit.getPluginManager().callEvent(event);
					}
				}.runTask(plugin);
			} catch (Exception e) {
				Bukkit.getLogger().warning("There was an error loading player data for player " + player.getName() + " with UUID " + player.getUniqueId() + ", see below for details.");
				e.printStackTrace();
				PlayerData data = createNewPlayer(player);
				data.setShouldSave(false);
				sendErrorMessageToPlayer(player, e);
			}
		} else {
			createNewPlayer(player);
		}
	}

	@Override
	public void save(Player player, boolean removeFromMemory) {
		PlayerData playerData = playerManager.getPlayerData(player);
		if (playerData == null) return;
		if (playerData.shouldNotSave()) return;
		// Save lock
		if (playerData.isSaving()) return;
		playerData.setSaving(true);
		// Load file
		File file = new File(plugin.getDataFolder() + "/Players/" + player.getUniqueId() + ".yml");
		FileConfiguration config = YamlConfiguration.loadConfiguration(file);
		try {
			config.set("User-Info.Username", player.getName());
			config.set("User-Info.UUID", player.getUniqueId().toString());
			// Save origin data
			if(!(playerData.getOrigin() == null)) {
				config.set("Origins.Origin", playerData.getOrigin());
			} else {
				config.set("Origins.Origin", "");
			}
			if(!(playerData.getLastOrigin() == null)) {
				config.set("Origins.LastOrigin", playerData.getLastOrigin());
			} else {
				config.set("Origins.LastOrigin", "");
			}
			for (OriginSetting originSetting : OriginSettings.values()) {
				String path = "Settings." + originSetting.toString();
				config.set(path, playerData.getOriginSetting(originSetting));
			}
			for (Punishment punishment : Punishments.values()) {
				String path = "Punishments." + punishment.toString();
				config.set(path, playerData.getPunishment(punishment));
			}
			config.set("Punishments.PurchasedUnban", playerData.isUnban());
			config.set("Staff.Silentmode", playerData.isSilentmode());
			config.save(file);
			if (removeFromMemory) {
				playerManager.removePlayerData(player.getUniqueId()); // Remove from memory
			}
		} catch (Exception e) {
			Bukkit.getLogger().warning("There was an error saving player data for player " + player.getName() + " with UUID " + player.getUniqueId() + ", see below for details.");
			e.printStackTrace();
		}
		playerData.setSaving(false); // Unlock
	}

	@Override
	public void save(Player player) {
		save(player, true);
	}
}
