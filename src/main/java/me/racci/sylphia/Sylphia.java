package me.racci.sylphia;

import co.aikar.commands.PaperCommandManager;
import me.racci.sylphia.commands.OriginCommand;
import me.racci.sylphia.data.configuration.OptionL;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.data.PlayerManager;
import me.racci.sylphia.data.storage.StorageProvider;
import me.racci.sylphia.data.storage.YamlStorageProvider;
import me.racci.sylphia.hook.PlaceholderAPIHook;
import me.racci.sylphia.hook.perms.LuckPermsHook;
import me.racci.sylphia.hook.perms.PermManager;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.listeners.PlayerChatEvent;
import me.racci.sylphia.listeners.PlayerConsumeEvent;
import me.racci.sylphia.listeners.PlayerJoinLeaveEvent;
import me.racci.sylphia.origins.OriginHandler;
import me.racci.sylphia.origins.objects.Origin;
import me.racci.sylphia.utils.Logger;
import me.racci.sylphia.utils.minecraft.WorldManager;
import me.racci.sylphia.utils.eventlistners.PlayerMoveListener;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class Sylphia extends JavaPlugin {

	private PlayerManager playerManager;
	private StorageProvider storageProvider;
//	private SelectorGUI originSelectorGUI;
	private WorldManager worldManager;
	private boolean placeholderAPI;
	private boolean luckPerms;
	private OptionL optionLoader;
	private PaperCommandManager commandManager;
	private Lang lang;

	private OriginHandler originHandler;
	private PermManager permManager;




	@Override
	public void onEnable() {
		Logger.log(Logger.Level.OUTLINE, "*******************************");
		Logger.log(Logger.Level.INFO, "Sylphia has started loading!");
//		final Items items = new Items(this);

		// Addon Hooks
		placeholderAPI = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
		if (placeholderAPI) {
			new PlaceholderAPIHook(this).register();
			Logger.log(Logger.Level.INFO, "Hooked PlaceholderAPI!");
		}

		// Load config
		loadConfig();
		optionLoader = new OptionL(this);
		optionLoader.loadOptions();
		commandManager = new PaperCommandManager(this);
		Logger.log(Logger.Level.INFO, "Loaded Config");

		// Load Lang
		lang = new Lang(this);
		getServer().getPluginManager().registerEvents(lang, this);
		lang.load();
		lang.loadEmbeddedMessages(commandManager);
		lang.loadLanguages(commandManager);

		this.originHandler = new OriginHandler(this);

		// Register Commands
		registerCommands();
		Logger.log(Logger.Level.INFO, "Loaded Commands");

		// Register Events
		registerEvents();
		Logger.log(Logger.Level.INFO, "Loaded Events");

		// Initialize Storage
		this.playerManager = new PlayerManager();
		setStorageProvider(new YamlStorageProvider(this));
		Logger.log(Logger.Level.INFO, "Loaded Storage Provider");

		// Load World Manager
		worldManager = new WorldManager(this);
		worldManager.loadWorlds();
		Logger.log(Logger.Level.INFO, "Loaded World Manager");

		// Register Permission Manager
		try {
			if(Bukkit.getServicesManager().getRegistration(LuckPerms.class) != null) {
				this.permManager = new LuckPermsHook();
				luckPerms = true;
				Logger.log(Logger.Level.INFO, "Hooked LuckPerms!");
			}
		} catch (NoClassDefFoundError e) {
			Logger.log(Logger.Level.ERROR, "No permission manager found, please make sure one is installed!");
		}



		// GUI
//		registerInventories();
		Logger.log(Logger.Level.OUTLINE, "*******************************");
		Logger.log(Logger.Level.SUCCESS, "Sylphia has finished loading successfully");
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
	}

	private void loadConfig() {
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
		commandManager.getCommandContexts().registerContext(Origin.class, c -> Origin.valueOf(c.popFirstArg()));
		commandManager.getCommandCompletions().registerAsyncCompletion("origins", c -> {
			List<String> values = new ArrayList<>();
			for (Origin origin : Origin.values()) {
				values.add(origin.toString().toLowerCase());
			}
			return values;
		});
		commandManager.registerCommand(new OriginCommand(this));
//		commandManager.registerCommand(new OriginSelector());
	}

	private void registerEvents() {
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(new PlayerJoinLeaveEvent(this), this);
		pm.registerEvents(new PlayerChatEvent(this), this);
		pm.registerEvents(new PlayerConsumeEvent(this), this);
		pm.registerEvents(new PlayerMoveListener(), this);
	}

	public static double getProcessCpuLoad() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
			AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });
			if (list.isEmpty())
				return 0.0;
			Attribute att = (Attribute) list.get(0);
			Double value = (Double) att.getValue();
			if (value == -1.0)
				return 0;
			return ((value * 1000.0) / 10.0);
		} catch (Exception e) {
			return 0;
		}
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

	public PermManager getPermManager() {
		return permManager;
	}


}
