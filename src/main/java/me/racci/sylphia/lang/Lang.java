package me.racci.sylphia.lang;

import co.aikar.commands.MessageKeys;
import co.aikar.commands.MinecraftMessageKeys;
import co.aikar.commands.PaperCommandManager;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.util.text.TextUtil;
import me.racci.sylphia.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Lang implements Listener {

	private static final Map<Locale, Map<MessageKey, String>> messages = new HashMap<>();
	private static Map<Locale, String> definedLanguages;
	private final Sylphia plugin;
	String lang = "lang.yml";

	public Lang(Sylphia plugin) {
		this.plugin = plugin;
	}

	public void init() {
		moveFilesToFolder();
		loadLanguageFiles();
	}

	public void loadLanguageFiles() {
		if (!new File(plugin.getDataFolder() + lang).exists()) {
			plugin.saveResource("lang.yml", false);
		}
	}

	public void loadEmbeddedMessages(PaperCommandManager commandManager) {
		InputStream inputStream = plugin.getResource(lang);
		if (inputStream != null) {
			FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			Locale locale = new Locale("en");
			loadMessages(config, locale, commandManager);
		}
	}

	public void loadLanguages(PaperCommandManager commandManager) {
		Logger.log(Logger.LogLevel.INFO, "Loading lang...");
		long startTime = System.currentTimeMillis();
		definedLanguages = new HashMap<>();
		String language = "en";
		try {
			Locale locale = new Locale(language);
			File file = new File(plugin.getDataFolder(), lang);
			// Load and update file
			FileConfiguration config = updateFile(file, YamlConfiguration.loadConfiguration(file), language);
			if (config.contains("file_version")) {
				// Load messages
				loadMessages(config, locale, commandManager);
				definedLanguages.put(locale, language);
			}
			else {
				Logger.log(Logger.LogLevel.ERROR, "Could not load lang file, Does this file exist and does it contain a file_version?");
			}
		} catch (Exception e) {
			Logger.log(Logger.LogLevel.ERROR, "Error loading lang file");
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		Logger.log(Logger.LogLevel.INFO, "Loaded lang in " + (endTime - startTime) + "ms");
	}

	private void loadMessages(FileConfiguration config, Locale locale, PaperCommandManager commandManager) {
		// Load units
		Map<UnitMessage, String> units = new HashMap<>();
		for (UnitMessage key : UnitMessage.values()) {
			String message = config.getString(key.getPath());
			if (message != null) {
				units.put(key, message.replace('&', '§'));
			}
		}
//		// Load message keys
//		Map<MessageKey, String> messages = Lang.messages.get(locale);
//		if (messages == null) {
//			messages = new HashMap<>();
//		}
//		for (String path : config.getKeys(true)) {
//			if (!config.isConfigurationSection(path)) {
//				MessageKey key = null;
//				// Try to find message key
//				for (MessageKey messageKey : MessageKey.values()) {
//					if (messageKey.getPath().equals(path)) {
//						key = messageKey;
//					}
//				}
//				// Create custom key if not found
//				if (key == null) {
//					key = new CustomMessageKey(path);
//				}
//				String message = config.getString(path);
//				if (message != null) {
//					messages.put(key, TextUtil.replace(message
//							,"&", "§"
//							,"{mana_unit}", units.get(UnitMessage.MANA)
//							,"{hp_unit}", units.get(UnitMessage.HP)
//							,"{xp_unit}", units.get(UnitMessage.XP)));
//				}
//			}
//		}
//		// Check that each message key has a value
//		for (MessageKey key : MessageKey.values()) {
//			String message = config.getString(key.getPath());
//			if (message == null && locale.equals(Locale.ENGLISH)) {
//				plugin.getLogger().warning("[" + locale.toLanguageTag() + "] Message with path " + key.getPath() + " not found!");
//			}
//		}
		for (ACFCoreMessage message : ACFCoreMessage.values()) {
			String path = message.getPath();
			commandManager.getLocales().addMessage(locale, MessageKeys.valueOf(message.name()), TextUtil.replace(config.getString(path), "&", "§"));
		}
		for (ACFMinecraftMessage message : ACFMinecraftMessage.values()) {
			String path = message.getPath();
			commandManager.getLocales().addMessage(locale, MinecraftMessageKeys.valueOf(message.name()), TextUtil.replace(config.getString(path), "&", "§"));
		}
		//Lang.messages.put(locale, messages);
	}

	private void moveFilesToFolder() {
		File file = new File(plugin.getDataFolder(), "messages_en.yml");
		if (file.exists()) {
			try {
				File directory = new File(plugin.getDataFolder() + "/messages");
				if (directory.mkdir()) {
					File[] pluginFiles = plugin.getDataFolder().listFiles();
					if (pluginFiles != null) {
						int filesMoved = 0;
						for (File pluginFile : pluginFiles) {
							if (pluginFile.getName().contains("messages") && pluginFile.isFile()) {
								if (pluginFile.renameTo(new File(plugin.getDataFolder() + "/messages/" + pluginFile.getName()))) {
									filesMoved++;
								} else {
									Bukkit.getLogger().warning("[Sylphia] Failed to move file " + pluginFile.getName() + " to messages folder!");
								}
							}
						}
						Bukkit.getLogger().warning("[Sylphia] Moved " + filesMoved + " files to messages folder. " +
								"If you are seeing this message, an update moved message files to a separate messages folder. " +
								"From now on use the messages folder for editing and adding messages. " +
								"If there are still message files in the root directory, you can delete them.");
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				Bukkit.getLogger().severe("[Sylphia] Error moving messages files to messages folder!");
			}
		}
	}

	private FileConfiguration updateFile(File file, FileConfiguration config, String language) {
		if (config.contains("file_version")) {
			InputStream stream = plugin.getResource("messages/messages_" + language + ".yml");
			if (stream != null) {
				int currentVersion = config.getInt("file_version");
				FileConfiguration imbConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
				int imbVersion = imbConfig.getInt("file_version");
				//If versions do not match
				if (currentVersion != imbVersion) {
					try {
						ConfigurationSection configSection = imbConfig.getConfigurationSection("");
						int keysAdded = 0;
						if (configSection != null) {
							for (String key : configSection.getKeys(true)) {
								if (!configSection.isConfigurationSection(key)) {
									if (!config.contains(key)) {
										config.set(key, imbConfig.get(key));
										keysAdded++;
									}
								}
							}
							// Messages to override
							for (MessageUpdates update : MessageUpdates.values()) {
								if (currentVersion < update.getVersion() && imbVersion >= update.getVersion()) {
									ConfigurationSection section = imbConfig.getConfigurationSection(update.getPath());
									if (section != null) {
										for (String key : section.getKeys(false)) {
											config.set(section.getCurrentPath() + "." + key, section.getString(key));
										}
										Bukkit.getLogger().warning("[Sylphia] messages_" + language + ".yml was changed: " + update.getMessage());
									} else {
										Object value = imbConfig.get(update.getPath());
										if (value != null) {
											config.set(update.getPath(), value);
											Bukkit.getLogger().warning("[Sylphia] messages_" + language + ".yml was changed: " + update.getMessage());
										}
									}
								}
							}
						}
						config.set("file_version", imbVersion);
						config.save(file);
						Bukkit.getLogger().info("[Sylphia] messages_" + language + ".yml was updated to a new file version, " + keysAdded + " new keys were added.");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return YamlConfiguration.loadConfiguration(file);
	}

	public static String getMessage(MessageKey key) {
		return messages.get(Locale.ENGLISH).get(key);
	}

}
