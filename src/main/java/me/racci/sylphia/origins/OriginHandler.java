package me.racci.sylphia.origins;

import com.cryptomorin.xseries.NoteBlockMusic;
import dev.dbassett.skullcreator.SkullCreator;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.events.OriginChangeEvent;
import me.racci.sylphia.hook.perms.PermManager;
import me.racci.sylphia.lang.Colours;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.OriginMessage;
import me.racci.sylphia.origins.enums.paths.Path;
import me.racci.sylphia.origins.objects.Origin;
import me.racci.sylphia.origins.objects.Origin.OriginValue;
import me.racci.sylphia.origins.objects.OriginAttribute;
import me.racci.sylphia.utils.Logger;
import me.racci.sylphia.utils.Parser;
import me.racci.sylphia.utils.TextUtil;
import me.racci.sylphia.utils.WorldTime;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.EnumUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
import java.time.Duration;
import java.util.*;

@SuppressWarnings({"unused", "deprecation", "ConstantConditions"})
public class OriginHandler implements Listener {

	private final Sylphia plugin;
	private static final Title.Times times = Title.Times.of(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
	private final HashMap<String, Origin> origins = new HashMap<>();

	public OriginHandler(Sylphia plugin) {
		this.plugin = plugin;
		this.refreshOrigins();
	}

	private static final String spaces = "              ";
	private static final Component passives = TextUtil.parseLegacy(spaces + Lang.getMessage(OriginMessage.LORE_PASSIVES));
	private static final Component abilities = TextUtil.parseLegacy(spaces + Lang.getMessage(OriginMessage.LORE_ABILITIES));
	private static final Component debuffs = TextUtil.parseLegacy(spaces + Lang.getMessage(OriginMessage.LORE_DEBUFFS));
	private static final Component select = TextUtil.parseLegacy(Lang.getMessage(OriginMessage.LORE_SELECT));
	private static final Component indent = TextUtil.parseLegacy(Lang.getMessage(OriginMessage.LORE_INDENT) + " ");

	public void refreshOrigins() {
		this.origins.clear();
		Map<String, YamlConfiguration> file = this.getAllOriginConfigurations();
		if(!file.isEmpty()) {
			for(Map.Entry<String, YamlConfiguration> originEntry : file.entrySet()) {
				if(!this.origins.containsKey(originEntry.getKey())) {
					this.origins.put(originEntry.getKey(), convertToOrigin(originEntry.getKey(), originEntry.getValue()));
				}
			}
		} else {
			Logger.log(Logger.LogLevel.ERROR, "No origins found!");
		}
	}

	private Origin convertToOrigin(String origin, YamlConfiguration file) {

		validateFile(file, plugin.getResource("NewOrigin.yml"));
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


		// Names and colours
		String name = file.getString(Path.NAME.getPath());
		String colour = ChatColor.color(file.getString(Path.COLOUR.getPath()), true);
		nameMap.put(OriginValue.NAME, name);
		nameMap.put(OriginValue.COLOUR, colour);
		nameMap.put(OriginValue.DISPLAY_NAME, colour + name);
		nameMap.put(OriginValue.BRACKET_NAME, colour + "[" + name + "]");

		// Sounds
		soundMap.put(OriginValue.HURT, Sound.valueOf(file.getString(Path.HURT.getPath())));
		soundMap.put(OriginValue.DEATH, Sound.valueOf(file.getString(Path.DEATH.getPath())));

		// Time titles and subtitles
		timeMessageMap.put(OriginValue.DAY_TITLE, ChatColor.color(file.getString(Path.DAY_TITLE.getPath()), true));
		timeMessageMap.put(OriginValue.DAY_SUBTITLE, ChatColor.color(file.getString(Path.DAY_SUBTITLE.getPath()), true));
		timeMessageMap.put(OriginValue.NIGHT_TITLE, ChatColor.color(file.getString(Path.NIGHT_TITLE.getPath()), true));
		timeMessageMap.put(OriginValue.NIGHT_SUBTITLE, ChatColor.color(file.getString(Path.NIGHT_SUBTITLE.getPath()), true));

		// Permissions
		permissionMap.put(OriginValue.PERMISSION_REQUIRED, file.getStringList(Path.REQUIRED.getPath()));
		permissionMap.put(OriginValue.PERMISSION_GIVEN, file.getStringList(Path.GIVEN.getPath()));

		// Effect enables
		effectEnableMap.put(OriginValue.GENERAL, file.getBoolean(Path.GENERAL_ENABLE.getPath()));
		effectEnableMap.put(OriginValue.TIME, file.getBoolean(Path.TIME_ENABLE.getPath()));
		effectEnableMap.put(OriginValue.LIQUID, file.getBoolean(Path.LIQUID_ENABLE.getPath()));
		effectEnableMap.put(OriginValue.DIMENSION, file.getBoolean(Path.LIQUID_ENABLE.getPath()));

		// Toggleable effects
		specialMap.put(OriginValue.SPECIAL_SLOWFALLING, (file.getBoolean(Path.SLOWFALLING.getPath()) ? 0 : 1));
		specialMap.put(OriginValue.SPECIAL_NIGHTVISION, (file.getBoolean(Path.NIGHTVISION.getPath()) ? 0 : 1));
		specialMap.put(OriginValue.SPECIAL_JUMPBOOST, file.getInt(Path.JUMPBOOST.getPath()));

		// Damage enables
		damageEnableMap.put(OriginValue.DAMAGE, file.getBoolean(Path.DAMAGE_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.SUN, file.getBoolean(Path.SUN_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.FALL, file.getBoolean(Path.FALL_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.RAIN, file.getBoolean(Path.RAIN_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.WATER, file.getBoolean(Path.WATER_ENABLE.getPath()));
		damageEnableMap.put(OriginValue.LAVA, file.getBoolean(Path.LAVA_ENABLE.getPath()));

		// Damage amounts
		damageAmountMap.put(OriginValue.SUN, file.getInt(Path.SUN_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.FALL, file.getInt(Path.FALL_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.RAIN, file.getInt(Path.RAIN_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.WATER, file.getInt(Path.WATER_AMOUNT.getPath()));
		damageAmountMap.put(OriginValue.LAVA, file.getInt(Path.LAVA_AMOUNT.getPath())); // Wrong value?
		if (effectEnableMap.get(OriginValue.GENERAL)) {
			attributeMap.putAll(putAttribute(Path.GENERAL_ATTRIBUTES.getPath(), OriginValue.GENERAL, file));
			effectMap.putAll(putEffect(Path.GENERAL_EFFECTS.getPath(), OriginValue.GENERAL, file));
		}
		if (effectEnableMap.get(OriginValue.TIME)) {
			attributeMap.putAll(putAttribute(Path.DAY_ATTRIBUTES.getPath(), OriginValue.DAY, file));
			effectMap.putAll(putEffect(Path.DAY_EFFECTS.getPath(), OriginValue.DAY, file));
			attributeMap.putAll(putAttribute(Path.NIGHT_ATTRIBUTES.getPath(), OriginValue.NIGHT, file));
			effectMap.putAll(putEffect(Path.NIGHT_EFFECTS.getPath(), OriginValue.NIGHT, file));
		}
		if (effectEnableMap.get(OriginValue.LIQUID)) {
			attributeMap.putAll(putAttribute(Path.WATER_ATTRIBUTES.getPath(), OriginValue.WATER, file));
			effectMap.putAll(putEffect(Path.WATER_EFFECTS.getPath(), OriginValue.WATER, file));
			attributeMap.putAll(putAttribute(Path.LAVA_ATTRIBUTES.getPath(), OriginValue.LAVA, file));
			effectMap.putAll(putEffect(Path.LAVA_EFFECTS.getPath(), OriginValue.LAVA, file));
		}
		if(effectEnableMap.get(OriginValue.DIMENSION)) {
			attributeMap.putAll(putAttribute(Path.OVERWORLD_ATTRIBUTES.getPath(), OriginValue.OVERWORLD, file));
			effectMap.putAll(putEffect(Path.OVERWORLD_EFFECTS.getPath(), OriginValue.OVERWORLD, file));
			attributeMap.putAll(putAttribute(Path.NETHER_ATTRIBUTES.getPath(), OriginValue.NETHER, file));
			effectMap.putAll(putEffect(Path.NETHER_EFFECTS.getPath(), OriginValue.NETHER, file));
			attributeMap.putAll(putAttribute(Path.END_ATTRIBUTES.getPath(), OriginValue.END, file));
			effectMap.putAll(putEffect(Path.END_EFFECTS.getPath(), OriginValue.END, file));
		}

		if (!file.isSet(Path.GUI_SKULL.getPath()) || !file.getBoolean(Path.GUI_SKULL.getPath())) {
			if (EnumUtils.isValidEnum(Material.class, file.getString(Path.GUI_MATERIAL.getPath()))) {
				item = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(file.getString(Path.GUI_MATERIAL.getPath())))), 1);
			} else {
				Logger.log(Logger.LogLevel.WARNING, "The material for " + origin + " is invalid!");
			}
		} else {
			item = SkullCreator.itemFromBase64(Objects.requireNonNull(file.getString(Path.GUI_SKULL.getPath())));
		}
		if (item != null) {
			if (file.getBoolean(Path.GUI_ENCHANTED.getPath())) {
				item.addEnchant(Enchantment.DURABILITY, 1, false);
				item.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
			List<Component> itemLore = new ArrayList<>();
			itemLore.add(Component.empty());
			file.getStringList(Path.GUI_LORE_DESCRIPTION.getPath()).forEach(var1x -> TextUtil.parseLegacy(ChatColor.color(var1x, true)));
			itemLore.add(Component.empty());
			itemLore.add(passives);
			file.getStringList(Path.GUI_LORE_PASSIVES.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
			itemLore.add(Component.empty());
			itemLore.add(abilities);
			file.getStringList(Path.GUI_LORE_ABILITIES.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
			itemLore.add(Component.empty());
			itemLore.add(debuffs);
			file.getStringList(Path.GUI_LORE_DEBUFFS.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
			itemLore.add(Component.empty());
			itemLore.add(Component.text("&f&l» &bClick &f&l« &eto change to the {var} &eorigin.")
					.replaceText(TextReplacementConfig.builder()
							.matchLiteral("{var}")
							.replacement(((matchResult, builder) -> TextUtil.parseLegacy(nameMap.get(OriginValue.DISPLAY_NAME))))
							.build()));
			item.lore(itemLore);
			item.setDisplayName(nameMap.get(OriginValue.DISPLAY_NAME));
		}

		Logger.log(Logger.LogLevel.INFO, "Debug: Registered origin " + origin + nameMap);
		return new Origin(nameMap, soundMap, timeMessageMap, permissionMap, effectEnableMap, specialMap, effectMap, attributeMap, damageEnableMap, damageAmountMap, item);
	}

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



	@EventHandler
	public void playerOriginChangeEvent(OriginChangeEvent e) {
		this.applyAll(e.getPlayer(), e.getNewOrigin());
	}

	public void setOrigin(Player player, Origin newOrigin) {
		Origin oldOrigin = getOrigin(player);
		OriginChangeEvent event = new OriginChangeEvent(player, oldOrigin, newOrigin);
		new BukkitRunnable() {
			@Override
			public void run() {
				Bukkit.getPluginManager().callEvent(event);
			}
		}.runTaskAsynchronously(plugin);
		if(!event.isCancelled()) {
			PermManager permManager = plugin.getPermManager();
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if(playerData != null) {
				if(newOrigin != null) {
					if(oldOrigin != null) {
						playerData.setLastOrigin(oldOrigin.toString().toUpperCase());
						permManager.removePermission(player, oldOrigin.getGivenPermission());
					}
					playerData.setOrigin(newOrigin.toString().toUpperCase());
					permManager.addPermission(player, newOrigin.getGivenPermission());
					plugin.getStorageProvider().save(playerData.getPlayer(), false);
					NoteBlockMusic.playMusic(player, player::getLocation, "PIANO,D,2,100 PIANO,B#1 200 PIANO,F 250 PIANO,E 250 PIANO,B 200 PIANO,A 100 PIANO,B 100 PIANO,E");
					Component var1 = TextUtil.parseLegacy(player.getDisplayName()).colorIfAbsent(TextColor.fromHexString(Lang.getMessage(Colours.PLAYER)));
					Component playerComponent = var1.hoverEvent(var1);
					var1 = TextUtil.parseLegacy(newOrigin.getDisplayName());
					Component originComponent = var1.hoverEvent(var1);
					final Component broadcast = Component.text(Lang.getMessage(OriginMessage.SELECT_BROADCAST))
							.replaceText(
									TextReplacementConfig.builder()
											.match("{var}")
											.replacement(((matchResult, builder) -> originComponent))
											.match("{PlayerDisplayName}")
											.replacement(((matchResult, builder) -> playerComponent))
											.build()
							).colorIfAbsent(TextColor.fromCSSHexString(Lang.getMessage(Colours.MISSING)));
					for (Player recipient : Bukkit.getOnlinePlayers()) {
						recipient.sendMessage(broadcast);
					}
					resetAll(player);
				}
			} else {
				event.setCancelled(true);
				Logger.log(Logger.LogLevel.ERROR, "There was an Error setting " + player.getName() + "'s origin to " + newOrigin);
			}
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
		return this.getOriginName(player) != null && origins.containsKey(this.getOriginName(player)) ? getOrigins().get(this.getOriginName(player)) : null;
	}

	/*
	The below takes care of setting effects,
	and totally resetting the players effects.
	*/

	public void resetAll(Player player) {
		for(PotionEffect potion : player.getActivePotionEffects()) {
			if(potion.getDuration() >= 86400L) {
				player.removePotionEffect(potion.getType());
			}
		}
		for(Attribute attribute : Attribute.values()) {
			AttributeInstance instance = player.getAttribute(attribute);
			if(instance == null) {
				break;
			} else if(instance.getBaseValue() != instance.getDefaultValue()) {
				instance.setBaseValue(instance.getDefaultValue());
			}
		}
		setAll(player);
	}
	public void setAll(Player player) {
		Origin origin = getOrigin(player);
		setGeneral(player, origin);
		setTime(player, origin);
		setLiquid(player, origin);
	}

	public void setGeneral(Player player) {
		setGeneral(player, getOrigin(player));
	}
	public void setTime(Player player) {
		setTime(player, getOrigin(player));
	}
	public void setLiquid(Player player) {
		setLiquid(player, getOrigin(player));
	}

	private void setGeneral(Player player, Origin origin) {
		for(PotionEffect potion : ListUtils.emptyIfNull(origin.getEffects())) {
			player.addPotionEffect(potion);
		}
		for(OriginAttribute attribute : ListUtils.emptyIfNull(origin.getAttributes())) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}
	private void setTime(Player player, Origin origin) {
		List<PotionEffect> potions;
		List<OriginAttribute> attributes;
		if (WorldTime.isDay(player)) {
			potions = origin.getDayEffects();
			attributes = origin.getDayAttributes();
		} else {
			potions = origin.getNightEffects();
			attributes = origin.getNightAttributes();
		}
		for (PotionEffect potion : ListUtils.emptyIfNull(potions)) {
			player.addPotionEffect(potion);
		}
		for (OriginAttribute attribute : ListUtils.emptyIfNull(attributes)) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}
	private void setLiquid(Player player, Origin origin) {
		List<PotionEffect> potions = null;
		List<OriginAttribute> attributes = null;
		if (player.isInWaterOrBubbleColumn()) {
			potions = origin.getWaterEffects();
			attributes = origin.getWaterAttributes();
		} else if (player.isInLava()) {
			potions = origin.getLavaEffects();
			attributes = origin.getLavaAttributes();
		}
		for (PotionEffect potion : ListUtils.emptyIfNull(potions)) {
			player.addPotionEffect(potion);
		}
		for (OriginAttribute attribute : ListUtils.emptyIfNull(attributes)) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}

	/*
	The below takes care of refreshing effects.
	*/

	public void refreshAll(Player player) {
		Origin origin = getOrigin(player);
		refreshTime(player, origin);
		refreshLiquid(player, origin);
	}
	public void refreshTime(Player player) {
		refreshTime(player, getOrigin(player));
	}
	public void refreshLiquid(Player player) {
		refreshLiquid(player, getOrigin(player));
	}


	private void refreshTime(Player player, Origin origin) {
		List<PotionEffect> potions;
		List<PotionEffect> removePotions;
		List<OriginAttribute> attributes;
		List<OriginAttribute> removeAttributes;
		String subtitle;
		if (WorldTime.isDay(player)) {
			potions = origin.getDayEffects();
			attributes = origin.getDayAttributes();
			subtitle = origin.getDayTitle();
			removePotions = origin.getNightEffects();
			removeAttributes = origin.getNightAttributes();
		} else {
			potions = origin.getNightEffects();
			attributes = origin.getNightAttributes();
			subtitle = origin.getNightTitle();
			removePotions = origin.getDayEffects();
			removeAttributes = origin.getDayAttributes();
		}
		removePotions(player, removePotions);
		removeAttributes(player, removeAttributes);
		for (PotionEffect potion : ListUtils.emptyIfNull(potions)) {
			player.addPotionEffect(potion);
		}
		for (OriginAttribute attribute : ListUtils.emptyIfNull(attributes)) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
		NoteBlockMusic.playMusic(player, player::getLocation, "PIANO,D,2,100 PIANO,B#1 200 PIANO,F 250 PIANO,E 250 PIANO,B 200 PIANO,A 100 PIANO,B 100 PIANO,E");
		final Title title = Title.title(Component.empty(), TextUtil.parseLegacy(subtitle), times);
		player.showTitle(title);
	}
	private void refreshLiquid(Player player, Origin origin) {
		//
	}

	private static void addPotions(Player player, List<PotionEffect> potions) {
		for (PotionEffect potion : ListUtils.emptyIfNull(potions)) {
			if(addPotionChecker(player, potion)) {
				player.addPotionEffect(potion);
			}
		}
	}
	private static void addAttributes(Player player, List<OriginAttribute> attributes) {
		for (OriginAttribute attribute : ListUtils.emptyIfNull(attributes)) {
			if (addAttributeChecker(player, attribute)) {
				player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
			}
		}
	}
	private static void removePotions(Player player, List<PotionEffect> potions) {
		for (PotionEffect potion : ListUtils.emptyIfNull(potions)) {
			if(removePotionChecker(player, potion)) {
				player.removePotionEffect(potion.getType());
			}
		}
	}
	private static void removeAttributes(Player player, List<OriginAttribute> attributes) {
		for (OriginAttribute attribute : ListUtils.emptyIfNull(attributes)) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}

	private static Boolean addAttributeChecker(Player player, OriginAttribute attribute) {
		return (player.getAttribute(attribute.getAttribute()).getBaseValue() < attribute.getValue());
	}
	private static Boolean addPotionChecker(Player player, PotionEffect potion) {
		return (!player.hasPotionEffect(potion.getType()) || !player.getPotionEffect(potion.getType()).hasIcon() || (player.getPotionEffect(potion.getType()).getDuration() < 86400L && player.getPotionEffect(potion.getType()).getAmplifier() <= potion.getAmplifier()));
	}
	private static Boolean removeAttributeChecker(Player player, Attribute attribute) {
		return (player.getAttribute(attribute).getBaseValue() != player.getAttribute(attribute).getDefaultValue());
	}
	private static Boolean removePotionChecker(Player player, PotionEffect potion) {
		return (player.hasPotionEffect(potion.getType()) && (player.getPotionEffect(potion.getType()).hasIcon() || player.getPotionEffect(potion.getType()).getDuration() >= 86400L));
	}


	public void applyAll(Player player, Origin origin) {

	}
}
