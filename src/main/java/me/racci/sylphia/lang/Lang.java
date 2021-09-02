package me.racci.sylphia.lang;

import co.aikar.commands.MessageKeys;
import co.aikar.commands.MinecraftMessageKeys;
import co.aikar.commands.PaperCommandManager;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.utils.TextUtil;
import me.racci.sylphia.utils.Logger;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Lang implements Listener {

	private static final Map<MessageKey, String> messagesMap = new HashMap<>();
	private final Sylphia plugin;
	private static final String lang = "lang.yml";
	private static final String fileVersion = "File_Version";

	public Lang(Sylphia plugin) {
		this.plugin = plugin;
	}

	public void load() {
		loadLangFile();
	}

	private void loadLangFile() {
		if(!(new File(plugin.getDataFolder(),lang).exists())) {
			plugin.saveResource(lang, false);
		}
	}

	public void loadEmbeddedMessages(PaperCommandManager commandManager) {
		InputStream inputStream = plugin.getResource(lang);
		if (inputStream != null) {
			try {
				FileConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
				loadMessages(config, commandManager);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void loadLanguages(PaperCommandManager commandManager) {
		Logger.log(Logger.Level.INFO, "Loading lang...");
		long startTime = System.currentTimeMillis();
		try {
			File file = new File(plugin.getDataFolder(), lang);
			FileConfiguration config = updateFile(file, YamlConfiguration.loadConfiguration(file));
			if (config.contains(fileVersion)) {
				loadMessages(config, commandManager);
			}
			else {
				Logger.log(Logger.Level.ERROR, "Could not load lang file, Does this file exist and does it contain a File_Version?");
			}
		} catch (Exception e) {
			Logger.log(Logger.Level.ERROR, "Error loading lang file");
			e.printStackTrace();
		}
		long endTime = System.currentTimeMillis();
		Logger.log(Logger.Level.INFO, "Loaded lang in " + (endTime - startTime) + "ms");
	}

	private void loadMessages(FileConfiguration config, PaperCommandManager commandManager) {
		Map<MessageKey, String> prefixes = new HashMap<>();
		for (Prefix key : Prefix.values()) {
			String message = config.getString(key.getPath());
			if (message != null) {
				prefixes.put(key, message.replace("&", "§"));
			}
		}
		Map<MessageKey, String> messages = Lang.messagesMap;
		for (String path : config.getKeys(true)) {
			if (!config.isConfigurationSection(path)) {
				MessageKey key = null;
				for (MessageKey messageKey : MessageKey.values()) {
					if (messageKey.getPath().equals(path)) {
						key = messageKey;
					}
				}
				if (key == null) {
					key = new CustomMessageKey(path);
				}
				String message = config.getString(path);
				if (message != null) {
					messages.put(key, ChatColor.color(TextUtil.replace(message,
							"{Origins}", prefixes.get(Prefix.ORIGINS),
							"{Sylphia}", prefixes.get(Prefix.SYLPHIA),
							"{Error}", prefixes.get(Prefix.ERROR)), true));
				}
			}
		}
		for (MessageKey key : MessageKey.values()) {
			String message = config.getString(key.getPath());
			if (message == null) {
				Logger.log(Logger.Level.WARNING, "Message with path " + key.getPath() + " not found!");
			}
		}
		for (ACFCoreMessage message : ACFCoreMessage.values()) {
			String path = message.getPath();
			commandManager.getLocales().addMessage(Locale.ENGLISH, MessageKeys.valueOf(message.name()), TextUtil.replace(config.getString(path), "&", "§"));
		}
		for (ACFMinecraftMessage message : ACFMinecraftMessage.values()) {
			String path = message.getPath();
			commandManager.getLocales().addMessage(Locale.ENGLISH, MinecraftMessageKeys.valueOf(message.name()), TextUtil.replace(config.getString(path), "&", "§"));
		}
		Lang.messagesMap.putAll(messages);
	}

	private FileConfiguration updateFile(File file, FileConfiguration config) {
		if (config.contains(fileVersion)) {
			InputStream stream = plugin.getResource(lang);
			if (stream != null) {
				int currentVersion = config.getInt(fileVersion);
				FileConfiguration imbConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
				int imbVersion = imbConfig.getInt(fileVersion);
				if (currentVersion != imbVersion) {
					try {
						ConfigurationSection configSection = imbConfig.getConfigurationSection("");
						int keysAdded = 0;
						if (configSection != null) {
							for (String key : configSection.getKeys(true)) {
								if (!configSection.isConfigurationSection(key) && !config.contains(key)) {
									config.set(key, imbConfig.get(key));
									keysAdded++;
								}
							}
						}
						config.set(fileVersion, imbVersion);
						config.save(file);
						Logger.log(Logger.Level.INFO, lang + " was updated to a new file version, " + keysAdded + " new keys were added.");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return YamlConfiguration.loadConfiguration(file);
	}

	public static String getMessage(MessageKey key) {
		return messagesMap.get(key);
	}

}
