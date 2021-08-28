package me.racci.sylphia.origins;

import dev.dbassett.skullcreator.SkullCreator;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.events.PlayerOriginChangeEvent;
import me.racci.sylphia.hook.perms.PermManager;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Prefix;
import me.racci.sylphia.origins.enums.options.Path;
import me.racci.sylphia.origins.objects.Origin;
import me.racci.sylphia.origins.objects.Origin.OriginValue;
import me.racci.sylphia.origins.objects.OriginAttribute;
import me.racci.sylphia.utils.Logger;
import me.racci.sylphia.utils.Parser;
import me.racci.sylphia.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;


@SuppressWarnings({"deprecation", "ConstantConditions"})
public class OriginHandler implements Listener {
	private final Sylphia plugin;
	final HashMap<String, Origin> origins = new HashMap<>();

	public OriginHandler(Sylphia plugin) {
		this.plugin = plugin;
		this.refreshOrigins();
	}

	public void refreshOrigins() {
		this.origins.clear();
		Map<String, YamlConfiguration> file = this.getAllOriginConfigurations();
		if(!file.isEmpty()) {
			for(Entry<String, YamlConfiguration> originEntry : file.entrySet()) {
				if(!this.origins.containsKey(originEntry.getKey())) {
					this.origins.put(originEntry.getKey(), convertToOrigin(originEntry.getKey(), originEntry.getValue()));
				}
			}
		} else {
			Logger.log(Logger.LogLevel.ERROR, "No origins found!");
		}
	}

	private Origin convertToOrigin(String origin, YamlConfiguration file) {

		validateFile(file, plugin.getResource("DefaultOrigin.yml"));

		LinkedHashMap<OriginValue, String> nameMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, Sound> soundMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, String> timeMessageMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, List<String>> permissionMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, Boolean> effectEnableMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, Integer> specialMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, List<PotionEffect>> effectMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, List<OriginAttribute>> attributeMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, Boolean> damageEnableMap = new LinkedHashMap<>();
		LinkedHashMap<OriginValue, Integer> damageAmountMap = new LinkedHashMap<>();
		ItemStack item = null;

		String name = file.getString(Path.NAME.getPath());
		String colour = ChatColor.color(file.getString(Path.COLOUR.getPath()), true);
		nameMap.put(OriginValue.NAME, name);
		nameMap.put(OriginValue.COLOUR, colour);
		nameMap.put(OriginValue.DISPLAY_NAME, colour + name);
		nameMap.put(OriginValue.BRACKET_NAME, colour + "[" + name + "]");
		soundMap.put(OriginValue.HURT_SOUND, Sound.valueOf(file.getString(Path.HURT_SOUND.getPath())));
		soundMap.put(OriginValue.DEATH_SOUND, Sound.valueOf(file.getString(Path.DEATH_SOUND.getPath())));
		timeMessageMap.put(OriginValue.DAY_MESSAGE, ChatColor.color(file.getString(Path.DAY_MESSAGE.getPath()), true));
		timeMessageMap.put(OriginValue.NIGHT_MESSAGE, ChatColor.color(file.getString(Path.NIGHT_MESSAGE.getPath()), true));
		permissionMap.put(OriginValue.PERMISSION_REQUIRED, file.getStringList(Path.PERMISSION_REQUIRED.getPath()));
		permissionMap.put(OriginValue.PERMISSION_GIVEN, file.getStringList(Path.PERMISSION_GIVEN.getPath()));
		effectEnableMap.put(OriginValue.GENERAL_ENABLED, file.getBoolean(Path.GENERAL_ENABLE.getPath()));
		effectEnableMap.put(OriginValue.TIME_ENABLED, file.getBoolean(Path.TIME_ENABLE.getPath()));
		effectEnableMap.put(OriginValue.LIQUID_ENABLED, file.getBoolean(Path.LIQUID_ENABLE.getPath()));
		specialMap.put(OriginValue.SPECIAL_SLOWFALLING, (file.getBoolean(Path.SLOWFALLING.getPath()) ? 1: 0));
		specialMap.put(OriginValue.SPECIAL_NIGHTVISION, (file.getBoolean(Path.NIGHTVISION.getPath()) ? 1 : 0));
		specialMap.put(OriginValue.SPECIAL_JUMPBOOST, file.getInt(Path.JUMPBOOST.getPath()));
		damageEnableMap.put(OriginValue.DAMAGE_ENABLED, file.getBoolean(Path.DAMAGE_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.SUN_ENABLED, file.getBoolean(Path.SUN_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.FALL_ENABLED, file.getBoolean(Path.FALL_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.RAIN_ENABLED, file.getBoolean(Path.RAIN_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.WATER_ENABLED, file.getBoolean(Path.WATER_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.LAVA_ENABLED, file.getBoolean(Path.LAVA_ENABLE.getPath()));
		damageAmountMap.put(OriginValue.SUN_AMOUNT, file.getInt(Path.SUN_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.FALL_AMOUNT, file.getInt(Path.FALL_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.RAIN_AMOUNT, file.getInt(Path.RAIN_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.WATER_AMOUNT, file.getInt(Path.WATER_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.LAVA_AMOUNT, file.getInt(Path.LAVA_AMOUNT.getPath()));
		if(effectEnableMap.get(OriginValue.GENERAL_ENABLED)) {
			attributeMap.putAll(putAttribute(Path.GENERAL_ATTRIBUTES.getPath(), OriginValue.GENERAL_ATTRIBUTES, file));
			effectMap.putAll(putEffect(Path.GENERAL_EFFECTS.getPath(), OriginValue.GENERAL_EFFECTS, file));
		}

		if(effectEnableMap.get(OriginValue.TIME_ENABLED)) {
			attributeMap.putAll(putAttribute(Path.DAY_ATTRIBUTES.getPath(), OriginValue.DAY_ATTRIBUTES, file));
			effectMap.putAll(putEffect(Path.DAY_EFFECTS.getPath(), OriginValue.DAY_EFFECTS, file));
			attributeMap.putAll(putAttribute(Path.NIGHT_ATTRIBUTES.getPath(), OriginValue.NIGHT_ATTRIBUTES, file));
			effectMap.putAll(putEffect(Path.NIGHT_EFFECTS.getPath(), OriginValue.NIGHT_EFFECTS, file));
		}

		if(effectEnableMap.get(OriginValue.LIQUID_ENABLED)) {
			attributeMap.putAll(putAttribute(Path.WATER_ATTRIBUTES.getPath(), OriginValue.WATER_ATTRIBUTES, file));
			effectMap.putAll(putEffect(Path.WATER_EFFECTS.getPath(), OriginValue.WATER_EFFECTS, file));
			attributeMap.putAll(putAttribute(Path.LAVA_ATTRIBUTES.getPath(), OriginValue.LAVA_ATTRIBUTES, file));
			effectMap.putAll(putEffect(Path.LAVA_EFFECTS.getPath(), OriginValue.LAVA_EFFECTS, file));
		}

		if(!file.isSet(Path.GUI_SKULL.getPath()) || !file.getBoolean(Path.GUI_SKULL.getPath())) {
			if(EnumUtils.isValidEnum(Material.class, file.getString(Path.GUI_MATERIAL.getPath()))) {
				item = new ItemStack(Material.getMaterial(file.getString(Path.GUI_MATERIAL.getPath())), 1);
			} else {
				Logger.log(Logger.LogLevel.WARNING, "The material for " + origin + " is invalid!");
			}
		} else {
			item = SkullCreator.itemFromBase64(file.getString(Path.GUI_SKULL.getPath()));
		}
		if(item != null) {
			if(file.getBoolean(Path.GUI_ENCHANTED.getPath())) {
				item.addEnchant(Enchantment.DURABILITY, 1, false);
				item.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
			if(file.isSet(Path.GUI_LORE.getPath())) {
				List<String> itemLore = file.getStringList(Path.GUI_LORE.getPath());
				itemLore.replaceAll(var0 -> {
					ChatColor.color(var0, true);
					return var0;
				});
				item.setLore(itemLore);
			}
			item.setDisplayName(nameMap.get(OriginValue.DISPLAY_NAME));
		}

		Logger.log(Logger.LogLevel.INFO, "Debug: Registered origin " + origin + nameMap);
		return new Origin(nameMap, soundMap, timeMessageMap, permissionMap, effectEnableMap, specialMap, effectMap, attributeMap, damageEnableMap, damageAmountMap, item);
	}


	// Unsafe method, doesn't check for existing effects of the player
	// Should only be used for first time applying effects to player
//	public void applyGeneral(Player player) {
//		Origin origin = this.getOrigin(player);
//		if(origin.isGeneralEnabled()) {
//			if(origin.getGeneralEffects() != null && !origin.getGeneralEffects().isEmpty()) {
//				for (PotionEffect potionEffect : origin.getGeneralEffects()) {
//					Logger.log(Logger.LogLevel.INFO, potionEffect.toString());
//					player.addPotionEffect(potionEffect);
//				}
//			}
//			if(origin.getGeneralAttributes() != null && !origin.getGeneralAttributes().isEmpty()) {
//				for (OriginAttribute attribute : origin.getGeneralAttributes()) {
//					Logger.log(Logger.LogLevel.INFO, attribute.toString());
//					player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
//				}
//			}
//		}
//	}

	// Unsafe method, doesn't check for existing effects of the player
	// Should only be used for first time applying effects to player
//	public void applyTime(Player player) {
//		Origin origin = this.getOrigin(player);
//		WorldTime worldTime = new WorldTime();
//		List<PotionEffect> potions = null;
//		List<OriginAttribute> attributes = null;
//		if(worldTime.isDay(player)) {
//			potions = origin.getDayEffects();
//			attributes = origin.getDayAttributes();
//		} else if(worldTime.isNight(player)){
//			potions = origin.getNightEffects();
//			attributes = origin.getNightAttributes();
//		}
////		if(potions != null) {
//		player.addPotionEffects(potions);
//
////		}
//		if(attributes != null) {
//			for(OriginAttribute attribute : attributes) {
//				player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
//			}
//		}
//	}


	// Unsafe method, doesn't check for existing effects of the player
	// Should only be used for first time applying effects to player
//	public void applyLiquid(Player player) {
//		Origin origin = this.getOrigin(player);
//		List<PotionEffect> potions = null;
//		List<OriginAttribute> attributes = null;
//		if(origin.isWaterLavaEnabled()) {
//			if(player.isInWaterOrBubbleColumn()) {
//				potions = origin.getWaterEffects();
//				attributes = origin.getWaterAttributes();
//			} else if(player.isInLava()) {
//				potions = origin.getLavaEffects();
//				attributes = origin.getLavaAttributes();
//			}
//			if(potions != null) {
//				for(PotionEffect potion : potions) {
//					player.addPotionEffect(potion);
//				}
//			}
//			if(attributes != null) {
//				for(OriginAttribute attribute : attributes) {
//					player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
//				}
//			}
//		}
//	}

	// Safe method
//	public void refreshTime(Player player) {
//		Origin origin = this.getOrigin(player);
//		WorldTime worldTime = new WorldTime();
//		List<PotionEffect> potions = null;
//		List<OriginAttribute> attributes = null;
//		if(origin.isNightDayEnabled()) {
//			if(worldTime.isDay(player)) {
//				potions = origin.getDayEffects();
//				attributes = origin.getDayAttributes();
//				if(origin.getNightEffects() != null && !origin.getNightEffects().isEmpty()) {
//					origin.getNightEffects().forEach(var0x -> {
//						if(player.hasPotionEffect(var0x.getType()) && !(player.getPotionEffect(var0x.getType()).getDuration() <= 86400)) {
//							player.removePotionEffect(var0x.getType());}
//					});
//				}
//			}
//			} else {
//				potions = origin.getNightEffects();
//				attributes = origin.getNightAttributes();
//				if(origin.getDayEffects() != null && !origin.getDayEffects().isEmpty()) {
//					origin.getDayEffects().forEach(var0x -> {
//						if(player.hasPotionEffect(var0x.getType()) && !(player.getPotionEffect(var0x.getType()).getDuration() <= 86400)) {
//							player.removePotionEffect(var0x.getType());}
//					});
//				}
//			}
//			if(potions != null) {
//				potions.forEach(player::addPotionEffect);
//			}
//			if(attributes != null) {
//				attributes.forEach(var0x -> player.getAttribute(var0x.getAttribute()).setBaseValue(var0x.getValue()));
//			}
//		}
//
	public void applyAll(Player player, Origin origin) {

	}



	//public void setOrigin(Player player, Origin origin) {
	//	Iterator origins = this.origins.values().iterator();

	//	ChangeOriginEvent e;
	//	if (origin == null) {
	//throw an error
	//	} else {
	//set origin in file to origin.getName().toUpperCase()
	//set last origin in file
	//trigger changeoriginevent
	//play sound for player
	//send broadcast
	//	}


	private Map<String, YamlConfiguration> getAllOriginConfigurations() {
		HashMap<String, YamlConfiguration> originMap = new HashMap<>();
		File[] Files = new File(plugin.getDataFolder().getAbsoluteFile() + "/Origins").listFiles();
		if(Files != null) {
			for(File originFile : Files) {
				if(!originFile.isDirectory()) {
					originMap.put(originFile.getName().toUpperCase().replace(".YML", ""), YamlConfiguration.loadConfiguration(originFile));
				}
			}
		}
		return originMap;
	}

	@EventHandler
	public void playerOriginChangeEvent(PlayerOriginChangeEvent e) {
		this.applyAll(e.getPlayer(), e.getNewOrigin());
	}

	// Needs some serious cleaning up
	public void setOrigin(Player player, Origin newOrigin) {
		Origin oldOrigin = getOrigin(player);
		PlayerOriginChangeEvent event = new PlayerOriginChangeEvent(player, oldOrigin, newOrigin);
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(event);
			}
		}.runTaskAsynchronously(plugin);
		if(!event.isCancelled()) {
			PermManager permManager = plugin.getPermManager();
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			String originBCN = null;
			if(oldOrigin != null) {
				playerData.setLastOrigin(oldOrigin.toString().toUpperCase());
//				permManager.removePermission(player, oldOrigin.getGivenPerms());
			}
			if(newOrigin != null) {
				playerData.setOrigin(newOrigin.toString().toUpperCase());
//				permManager.addPermission(player, newOrigin.getGivenPerms());
				return;
			}
			plugin.getStorageProvider().save(playerData.getPlayer(), false);
//			playMusic(player, player::getLocation,"PIANO,D,2,100 PIANO,B#1 200 PIANO,F 250 PIANO,E 250 PIANO,B 200 PIANO,A 100 PIANO,B 100 PIANO,E");
			final TextComponent message = Component.text()
					.append(TextUtil.parseLegacy(Lang.getMessage(Prefix.ORIGINS_BOLD))).build()
					.append(TextUtil.parseLegacy(" " + player.getDisplayName()).colorIfAbsent(TextColor.color(230, 230, 230)))
					.append(Component.text(" is the "))
					.append(TextUtil.parseLegacy(newOrigin.getDisplayName()).clickEvent(ClickEvent.openUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).hoverEvent(HoverEvent.showText(Component.text("why hover?"))))
					.append(Component.text(" Origin!"))
					.colorIfAbsent(TextColor.color(42, 231, 220))
					.toBuilder().build();
			for(Player recipient : Bukkit.getOnlinePlayers()) {
				recipient.sendMessage(message);
			}


			// Make broadcast
			// Title with name and subtitle with short description
			// Nice sound required (maybe a harmony)
			//


		}
	}


	public String getOriginName(Player player) {
		PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
		if (playerData != null) {
			if (this.getOrigins().get(playerData.getOrigin()) != null) {
				return playerData.getOrigin().toUpperCase();
			} else {
				return null;
			}
		} return null;
	}

	public Origin getOrigin(Player player) {
		return this.getOriginName(player) != null && this.origins.containsKey(this.getOriginName(player)) ? this.getOrigins().get(this.getOriginName(player)) : null;
	}


	public Map<String, Origin> getOrigins() {
		return this.origins;
	}

	private static void validateFile(YamlConfiguration file, InputStream stream) {
		if (stream != null) {
			FileConfiguration imbConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
			try {
				ConfigurationSection configSection = imbConfig.getConfigurationSection("");
				if (configSection != null) {
					for (String key : configSection.getKeys(true)) {
						if (!configSection.isConfigurationSection(key) && !file.contains(key)) {
							file.set(key, imbConfig.get(key));
							Logger.log(Logger.LogLevel.ERROR, "There was a missing key at " + key + " inside YAML for " + file.getName().replace(".yml", "") + ". Added key to file.");
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<OriginValue, ArrayList<OriginAttribute>> putAttribute(String path, OriginValue originValue, YamlConfiguration file) {
		if (!file.getStringList(path).isEmpty()) {
			ArrayList<OriginAttribute> attributes = new ArrayList<>();
			for (String attributeString : file.getStringList(path)) {
				OriginAttribute originAttribute = Parser.parseOriginAttribute(attributeString);
				if (originAttribute != null) {
					attributes.add(originAttribute);
				}
			}
			Map<OriginValue, ArrayList<OriginAttribute>> map = new LinkedHashMap<>();
			map.put(originValue, attributes);
			return map;
		} else {
			return null;
		}
	}

	private static Map<OriginValue, ArrayList<PotionEffect>> putEffect(String path, OriginValue originValue, YamlConfiguration file) {
		if (!file.getStringList(path).isEmpty()) {
			ArrayList<PotionEffect> potions = new ArrayList<>();
			for (String effectString : file.getStringList(path)) {
				PotionEffect potion = Parser.parseEffect(effectString);
				if (potion != null) {
					potions.add(potion);
				}
			}
			Map<OriginValue, ArrayList<PotionEffect>> map = new LinkedHashMap<>();
			map.put(originValue, potions);
			return map;
		} else {
			return null;
		}
	}

}

