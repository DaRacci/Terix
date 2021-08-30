package me.racci.sylphia;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import me.racci.sylphia.commands.OriginCommand;
import me.racci.sylphia.commands.OriginSelector;
import me.racci.sylphia.configuration.OptionL;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.data.PlayerManager;
import me.racci.sylphia.data.storage.StorageProvider;
import me.racci.sylphia.data.storage.YamlStorageProvider;
import me.racci.sylphia.hook.PlaceholderAPIHook;
import me.racci.sylphia.hook.perms.LuckPermsHook;
import me.racci.sylphia.hook.perms.PermManager;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Prefix;
import me.racci.sylphia.listeners.PlayerChatEvent;
import me.racci.sylphia.listeners.PlayerJoinLeaveEvent;
import me.racci.sylphia.origins.OriginHandler;
import me.racci.sylphia.origins.enums.Origin;
import me.racci.sylphia.origins.enums.OriginRegistry;
import me.racci.sylphia.origins.enums.Origins;
import me.racci.sylphia.region.RegionListener;
import me.racci.sylphia.region.RegionManager;
import me.racci.sylphia.util.world.WorldManager;
import me.racci.sylphia.utils.Logger;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Sylphia extends JavaPlugin {

	private PlayerManager playerManager;
	private StorageProvider storageProvider;
	private WorldManager worldManager;
	private boolean placeholderAPI;
	private boolean luckPerms;
	private OptionL optionLoader;
	private PaperCommandManager commandManager;
	private Lang lang;
	private RegionManager regionManager;

	private OriginRegistry originRegistry;

	private OriginHandler originHandler;
	private PermManager permManager;




	@Override
	public void onEnable() {
		Logger.log(Logger.LogLevel.OUTLINE, "*******************************");
		Logger.log(Logger.LogLevel.INFO, "Sylphia has started loading!");
		// Registries
		originRegistry = new OriginRegistry();
		registerOrigins();

		// GUI



		// Addon Hooks
		placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
		if (placeholderAPI) {
			new PlaceholderAPIHook(this).register();
			Logger.log(Logger.LogLevel.INFO, "Hooked PlaceholderAPI!");
		}

		// Load config
		loadConfig();
		optionLoader = new OptionL(this);
		optionLoader.loadOptions();
		commandManager = new PaperCommandManager(this);
		Logger.log(Logger.LogLevel.INFO, "Loaded Config");

		// Load Lang
		lang = new Lang(this);
		getServer().getPluginManager().registerEvents(lang, this);
		lang.load();
		lang.loadEmbeddedMessages(commandManager);
		lang.loadLanguages(commandManager);

		// Register Commands
		registerCommands();
		Logger.log(Logger.LogLevel.INFO, "Loaded Commands");

		// Region Manager
		this.regionManager = new RegionManager(this);
		Logger.log(Logger.LogLevel.INFO, "Loaded Region Manager");

		// Register Events
		registerEvents();
		Logger.log(Logger.LogLevel.INFO, "Loaded Events");

		// Initialize Storage
		this.playerManager = new PlayerManager();
		setStorageProvider(new YamlStorageProvider(this));
		Logger.log(Logger.LogLevel.INFO, "Loaded Storage Provider");

		// Load World Manager
		worldManager = new WorldManager(this);
		worldManager.loadWorlds();
		Logger.log(Logger.LogLevel.INFO, "Loaded World Manager");

		// Register Permission Manager
		try {
			if(Bukkit.getServicesManager().getRegistration(LuckPerms.class) != null) {
				this.permManager = new LuckPermsHook();
				luckPerms = true;
				Logger.log(Logger.LogLevel.INFO, "Hooked LuckPerms!");
			}
		} catch (NoClassDefFoundError e) {
			Logger.log(Logger.LogLevel.ERROR, "No permission manager found, please make sure one is installed!");
		}

		this.originHandler = new OriginHandler(this);
		Logger.log(Logger.LogLevel.OUTLINE, "*******************************");
		Logger.log(Logger.LogLevel.SUCCESS, "Sylphia has finished loading successfully");
	}

	@Override
	public void onDisable() {
		for (PlayerData playerData : playerManager.getPlayerDataMap().values()) {
			storageProvider.save(playerData.getPlayer(), false);
		}
		playerManager.getPlayerDataMap().clear();
		File file = new File(this.getDataFolder(), "config.yml");
		if (file.exists()) {
			reloadConfig();
			saveConfig();
		}
		regionManager.saveAllRegions(false, true);
		regionManager.clearRegionMap();
	}

	public void loadConfig() {
		getConfig().options().copyDefaults(true);
		saveDefaultConfig();
		try {
			InputStream is = getResource("config.yml");
			if (is != null) {
				YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
				ConfigurationSection config = defConfig.getConfigurationSection("");
				if (config != null) {
					for (String key : config.getKeys(true)) {
						if (!getConfig().contains(key)) {
							getConfig().set(key, defConfig.get(key));
						}
					}
				}
				saveConfig();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registerCommands() {
		commandManager.enableUnstableAPI("help"); //Need to learn what this is defined by
		commandManager.usePerIssuerLocale(true, false); //Probably remove this, don't plan on adding more than english.
		commandManager.getCommandContexts().registerContext(Origin.class, c -> {
			Origin origin = originRegistry.getOrigin(c.popFirstArg());
			if (origin != null) {
				return origin;
			} else {
				throw new InvalidCommandArgument("Origin " + c.popFirstArg() + " not found!");
			}
		});
		commandManager.getCommandCompletions().registerAsyncCompletion("origins", c -> {
			List<String> values = new ArrayList<>();
			for (Origin origin : originRegistry.getOrigins()) {
				values.add(origin.toString().toLowerCase());
			}
			return values;
		});
		commandManager.registerCommand(new OriginCommand(this));
		commandManager.registerCommand(new OriginSelector(this));
	}

	public void registerEvents() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerJoinLeaveEvent(this), this);
		pm.registerEvents(new PlayerChatEvent(this), this);
		pm.registerEvents(new RegionListener(this), this);
	}

	// Want to find how to make a dynamic list based off the loaded origins at start and reloads
	private void registerOrigins() {
		originRegistry.register("human", Origins.HUMAN);
		originRegistry.register("raccoon", Origins.RACCOON);
	}

	public PlayerManager getPlayerManager() {
		return playerManager;
	}

	public OriginHandler getOriginHandler() {
		return this.originHandler;
	}

	public WorldManager getWorldManager() {
		return worldManager;
	}

	public PaperCommandManager getCommandManager() {
		return commandManager;
	}

	// Make a way of getting prefix from a set of options
	public static String getPrefix() {
		return Lang.getMessage(Prefix.SYLPHIA);
	}

	public OptionL getOptionLoader() {
		return optionLoader;
	}

	public Lang getLang() {
		return lang;
	}

	public boolean isPlaceholderAPI() {
		return placeholderAPI;
	}

	public boolean isLuckPerms() {
		return luckPerms;
	}

	public StorageProvider getStorageProvider() {
		return storageProvider;
	}

	public void setStorageProvider(StorageProvider storageProvider) {
		this.storageProvider = storageProvider;
	}

	public RegionManager getRegionManager() {
		return regionManager;
	}

	public OriginRegistry getOriginRegistry() {
		return originRegistry;
	}

	public PermManager getPermManager() {
		return permManager;
	}


}
