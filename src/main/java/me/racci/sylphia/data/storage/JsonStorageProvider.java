package me.racci.sylphia.data.storage;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.events.DataLoadEvent;
import me.racci.sylphia.origins.enums.specials.Special;
import me.racci.sylphia.origins.enums.specials.Specials;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.UUID;

public class JsonStorageProvider extends StorageProvider {

	public JsonStorageProvider(Sylphia plugin) {
		super(plugin);
	}

	@Override
	public void load(Player player) {
//		Json playerFile = LightningBuilder
//				.fromPath(player.getUniqueId() + ".json", plugin.getDataFolder() + "/Players/")
//				.setDataType(DataType.SORTED)
//				.setReloadSettings(ReloadSettings.MANUALLY)
//				.addInputStreamFromResource("Player.json")
//				.createJson();





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
				for (Special originSetting : Specials.values()) {
					String path = "Settings." + originSetting.name().toUpperCase();
					int value = config.getInt(path, 1);
					playerData.setOriginSetting(originSetting, value);
				}
				playerManager.addPlayerData(playerData);
				DataLoadEvent event = new DataLoadEvent(playerData);
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
			for (Special originSetting : Specials.values()) {
				String path = "Settings." + originSetting.toString();
				config.set(path, playerData.getOriginSetting(originSetting));
			}
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
