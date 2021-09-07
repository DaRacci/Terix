package me.racci.sylphia.origins.objects;

import me.racci.raccilib.Logger;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.events.OriginChangeEvent;
import me.racci.sylphia.hook.perms.PermManager;
import me.racci.sylphia.lang.Colours;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Origins;
import me.racci.sylphia.origins.OriginAttribute;
import me.racci.sylphia.origins.OriginValue;
import me.racci.sylphia.origins.enums.paths.Path;
import me.racci.sylphia.utils.minecraft.AttributeUtils;
import me.racci.sylphia.utils.minecraft.PotionUtils;
import me.racci.sylphia.utils.minecraft.WorldTime;
import me.racci.sylphia.utils.text.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;


public class OriginHandler implements Listener {



	private final Sylphia plugin;
	private static final HashMap<Origins, String> requiredPermList = new HashMap<>();
	private static final Title.Times times = Title.Times.of(Duration.ofMillis(500), Duration.ofMillis(3000), Duration.ofMillis(1000));
	private final HashMap<String, Origins> origins = new HashMap<>();

	public OriginHandler(Sylphia plugin) {
		this.plugin = plugin;
		this.refreshOrigins();
	}

	private static final String spaces = "              ";
	private static final Component passives = TextUtil.parseLegacy(spaces + Lang.getMessage(Origins.LORE_PASSIVES));
	private static final Component abilities = TextUtil.parseLegacy(spaces + Lang.getMessage(Origins.LORE_ABILITIES));
	private static final Component debuffs = TextUtil.parseLegacy(spaces + Lang.getMessage(Origins.LORE_DEBUFFS));
	private static final Component select = TextUtil.parseLegacy(ChatColor.color(Lang.getMessage(Origins.LORE_SELECT), true));
	private static final String indent = Lang.getMessage(Origins.LORE_INDENT) + " ";
/*
----------------------------------------------------------------------------------------------------
									Registering the origin
----------------------------------------------------------------------------------------------------
*/
	public void refreshOrigins() {
		this.originsMap.clear();
		Map<String, YamlConfiguration> file = this.getAllOriginConfigurations();
		if(!file.isEmpty()) {
			for(Map.Entry<String, YamlConfiguration> originEntry : file.entrySet()) {
				if(!this.originsMap.containsKey(originEntry.getKey())) {
					this.originsMap.put(originEntry.getKey(), convertToOrigin(originEntry.getKey(), originEntry.getValue()));
				}
			}
		} else {
			Logger.log(Logger.Level.ERROR, "No origins found!");
		}
	}

	private Origins convertToOrigin(String origin, YamlConfiguration file) {

		validateFile(file, plugin.getResource("Origin.yml"));
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, String> nameMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Sound> soundMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, String> timeMessageMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, List<String>> permissionMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Boolean> effectEnableMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Integer> specialMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, List<PotionEffect>> effectMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, List<OriginAttribute>> attributeMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Boolean> damageEnableMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Integer> damageAmountMap = new LinkedHashMap<>();
		LinkedHashMap<me.racci.sylphia.origins.OriginValue, Object> guiItem = new LinkedHashMap<>();
		ItemStack item = null;
		Integer slot = null;

		// Names and colours
		String name = file.getString(Path.NAME.getPath());
		String colour = ChatColor.color(file.getString(Path.COLOUR.getPath()), true);
		nameMap.put(me.racci.sylphia.origins.OriginValue.NAME, name);
		nameMap.put(me.racci.sylphia.origins.OriginValue.COLOUR, colour);
		nameMap.put(me.racci.sylphia.origins.OriginValue.DISPLAY_NAME, colour + name);
		nameMap.put(me.racci.sylphia.origins.OriginValue.BRACKET_NAME, colour + "[" + name + "]");

		// Sounds
		soundMap.put(me.racci.sylphia.origins.OriginValue.HURT, Sound.valueOf(file.getString(Path.HURT.getPath())));
		soundMap.put(me.racci.sylphia.origins.OriginValue.DEATH, Sound.valueOf(file.getString(Path.DEATH.getPath())));

		// Time titles and subtitles
		timeMessageMap.put(me.racci.sylphia.origins.OriginValue.DAY_TITLE, ChatColor.color(file.getString(Path.DAY_TITLE.getPath()), true));
		timeMessageMap.put(me.racci.sylphia.origins.OriginValue.DAY_SUBTITLE, ChatColor.color(file.getString(Path.DAY_SUBTITLE.getPath()), true));
		timeMessageMap.put(me.racci.sylphia.origins.OriginValue.NIGHT_TITLE, ChatColor.color(file.getString(Path.NIGHT_TITLE.getPath()), true));
		timeMessageMap.put(me.racci.sylphia.origins.OriginValue.NIGHT_SUBTITLE, ChatColor.color(file.getString(Path.NIGHT_SUBTITLE.getPath()), true));

		// Permissions
		permissionMap.put(me.racci.sylphia.origins.OriginValue.PERMISSION_REQUIRED, file.getStringList(Path.REQUIRED.getPath()));
		permissionMap.put(me.racci.sylphia.origins.OriginValue.PERMISSION_GIVEN, file.getStringList(Path.GIVEN.getPath()));

		// Effect enables
		effectEnableMap.put(me.racci.sylphia.origins.OriginValue.GENERAL, file.getBoolean(Path.GENERAL_ENABLE.getPath()));
		effectEnableMap.put(me.racci.sylphia.origins.OriginValue.TIME, file.getBoolean(Path.TIME_ENABLE.getPath()));
		effectEnableMap.put(me.racci.sylphia.origins.OriginValue.LIQUID, file.getBoolean(Path.LIQUID_ENABLE.getPath()));
		effectEnableMap.put(me.racci.sylphia.origins.OriginValue.DIMENSION, file.getBoolean(Path.LIQUID_ENABLE.getPath()));

		// Toggleable effects
		specialMap.put(me.racci.sylphia.origins.OriginValue.SPECIAL_SLOWFALLING, (file.getBoolean(Path.SLOWFALLING.getPath()) ? 0 : 1));
		specialMap.put(me.racci.sylphia.origins.OriginValue.SPECIAL_NIGHTVISION, (file.getBoolean(Path.NIGHTVISION.getPath()) ? 0 : 1));
		specialMap.put(me.racci.sylphia.origins.OriginValue.SPECIAL_JUMPBOOST, file.getInt(Path.JUMPBOOST.getPath()));

		// Damage enables
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.DAMAGE, file.getBoolean(Path.DAMAGE_ENABLE.getPath()));
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.SUN, file.getBoolean(Path.SUN_ENABLE.getPath()));
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.FALL, file.getBoolean(Path.FALL_ENABLE.getPath()));
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.RAIN, file.getBoolean(Path.RAIN_ENABLE.getPath()));
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.WATER, file.getBoolean(Path.WATER_ENABLE.getPath()));
		damageEnableMap.put(me.racci.sylphia.origins.OriginValue.LAVA, file.getBoolean(Path.LAVA_ENABLE.getPath()));

		// Damage amounts
		damageAmountMap.put(me.racci.sylphia.origins.OriginValue.SUN, file.getInt(Path.SUN_AMOUNT.getPath()));
		damageAmountMap.put(me.racci.sylphia.origins.OriginValue.FALL, file.getInt(Path.FALL_AMOUNT.getPath()));
		damageAmountMap.put(me.racci.sylphia.origins.OriginValue.RAIN, file.getInt(Path.RAIN_AMOUNT.getPath()));
		damageAmountMap.put(me.racci.sylphia.origins.OriginValue.WATER, file.getInt(Path.WATER_AMOUNT.getPath()));
		damageAmountMap.put(me.racci.sylphia.origins.OriginValue.LAVA, file.getInt(Path.LAVA_AMOUNT.getPath())); // Wrong value?
		if (effectEnableMap.get(me.racci.sylphia.origins.OriginValue.GENERAL)) {
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.GENERAL_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.GENERAL, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.GENERAL_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.GENERAL, file)));
		}
		if (effectEnableMap.get(me.racci.sylphia.origins.OriginValue.TIME)) {
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.DAY_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.DAY, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.DAY_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.DAY, file)));
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.NIGHT_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.NIGHT, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.NIGHT_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.NIGHT, file)));
		}
		if (effectEnableMap.get(me.racci.sylphia.origins.OriginValue.LIQUID)) {
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.WATER_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.WATER, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.WATER_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.WATER, file)));
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.LAVA_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.LAVA, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.LAVA_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.LAVA, file)));
		}
		if(effectEnableMap.get(me.racci.sylphia.origins.OriginValue.DIMENSION)) {
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.OVERWORLD_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.OVERWORLD, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.OVERWORLD_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.OVERWORLD, file)));
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.NETHER_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.NETHER, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.NETHER_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.NETHER, file)));
			attributeMap.putAll(MapUtils.emptyIfNull(putAttribute(Path.END_ATTRIBUTES.getPath(), me.racci.sylphia.origins.OriginValue.END, file)));
			effectMap.putAll(MapUtils.emptyIfNull(putEffect(Path.END_EFFECTS.getPath(), me.racci.sylphia.origins.OriginValue.END, file)));
		}

		if(!file.isSet(Path.GUI_SKULL.getPath()) || (file.isBoolean(Path.GUI_SKULL.getPath()) && !file.getBoolean(Path.GUI_SKULL.getPath()))) {
			if (EnumUtils.isValidEnum(Material.class, file.getString(Path.GUI_MATERIAL.getPath()))) {
				item = new ItemStack(Objects.requireNonNull(Material.getMaterial(Objects.requireNonNull(file.getString(Path.GUI_MATERIAL.getPath())))), 1);
			} else {
				Logger.log(Logger.Level.WARNING, "The material for " + origin + " is invalid!");
			}
		} else if (file.isSet(Path.GUI_SKULL.getPath()) && !file.isBoolean(Path.GUI_SKULL.getPath())) {
			item = SkullCreator.itemFromBase64(Objects.requireNonNull(file.getString(Path.GUI_SKULL.getPath())));
		}
		if (item != null) {
			if (file.getBoolean(Path.GUI_ENCHANTED.getPath())) {
				item.addEnchant(Enchantment.DURABILITY, 1, false);
				item.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
			List<Component> itemLore = new ArrayList<>();
			if(file.get(Path.GUI_LORE_DESCRIPTION.getPath()) != null) {
				itemLore.add(Component.empty());
				file.getStringList(Path.GUI_LORE_DESCRIPTION.getPath()).forEach(var1x -> TextUtil.parseLegacy(ChatColor.color(var1x, true)));
				itemLore.add(Component.empty());
			}
			if(file.get(Path.GUI_LORE_PASSIVES.getPath()) != null) {
				itemLore.add(passives);
				file.getStringList(Path.GUI_LORE_PASSIVES.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
				itemLore.add(Component.empty());
			}
			if(file.get(Path.GUI_LORE_ABILITIES.getPath()) != null) {
				itemLore.add(abilities);
				file.getStringList(Path.GUI_LORE_ABILITIES.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
				itemLore.add(Component.empty());
			}
			if(file.get(Path.GUI_LORE_DEBUFFS.getPath()) != null) {
				itemLore.add(debuffs);
				file.getStringList(Path.GUI_LORE_DEBUFFS.getPath()).forEach(var1x -> itemLore.add(TextUtil.parseLegacy(ChatColor.color(indent + var1x, true))));
				itemLore.add(Component.empty());
			}
			itemLore.add(TextUtil.parseLegacy(Lang.getMessage(Origins.LORE_SELECT))
					.replaceText(TextReplacementConfig.builder()
							.matchLiteral("{var}")
							.replacement(((matchResult, builder) -> TextUtil.parseLegacy(nameMap.get(me.racci.sylphia.origins.OriginValue.DISPLAY_NAME))))
							.build()));
			item.lore(itemLore);
			item.setDisplayName(nameMap.get(me.racci.sylphia.origins.OriginValue.DISPLAY_NAME));
			slot = file.getInt(Path.GUI_SLOT.getPath());
			guiItem.put(me.racci.sylphia.origins.OriginValue.ITEM, item);
			guiItem.put(me.racci.sylphia.origins.OriginValue.SLOT, slot);
		} else {
			Logger.log(Logger.Level.WARNING, "There was an error getting a material or skull for " + nameMap.get(me.racci.sylphia.origins.OriginValue.DISPLAY_NAME));
		}

		Origins finalOrigin = new Origins(nameMap, soundMap, timeMessageMap, permissionMap, effectEnableMap, specialMap, effectMap, attributeMap, damageEnableMap, damageAmountMap, guiItem);

		if(permissionMap.get(me.racci.sylphia.origins.OriginValue.PERMISSION_REQUIRED).get(0) != null) {
			requiredPermList.putIfAbsent(finalOrigin, permissionMap.get(me.racci.sylphia.origins.OriginValue.PERMISSION_REQUIRED).get(0));
		}

		Logger.log(Logger.Level.INFO, "Debug: Registered origin " + finalOrigin.getDisplayName());
		return finalOrigin;
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
	public Map<String, Origins> getOrigins() {
		return this.originsMap;
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
							Logger.log(Logger.Level.ERROR, "There was a missing key at " + key + " inside YAML for " + file.getString(Path.NAME.getPath()));
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static Map<me.racci.sylphia.origins.OriginValue, ArrayList<OriginAttribute>> putAttribute(String path, me.racci.sylphia.origins.OriginValue originValue, YamlConfiguration file) {
		if (!file.getStringList(path).isEmpty()) {
			ArrayList<OriginAttribute> attributes = new ArrayList<>();
			for (String attributeString : file.getStringList(path)) {
				OriginAttribute originAttribute = AttributeUtils.parseOriginAttribute(attributeString);
				if (originAttribute != null) {
					attributes.add(originAttribute);
				}
			}
			Map<me.racci.sylphia.origins.OriginValue, ArrayList<OriginAttribute>> map = new LinkedHashMap<>();
			map.put(originValue, attributes);
			return map;
		} else {
			return null;
		}
	}
	private static Map<me.racci.sylphia.origins.OriginValue, ArrayList<PotionEffect>> putEffect(String path, me.racci.sylphia.origins.OriginValue originValue, YamlConfiguration file) {
		if (!file.getStringList(path).isEmpty()) {
			ArrayList<PotionEffect> potions = new ArrayList<>();
			for (String effectString : file.getStringList(path)) {
				PotionEffect potion = PotionUtils.parseOriginPotion(effectString);
				if (potion != null) {
					potions.add(potion);
				}
			}
			Map<me.racci.sylphia.origins.OriginValue, ArrayList<PotionEffect>> map = new LinkedHashMap<>();
			map.put(originValue, potions);
			return map;
		} else {
			return null;
		}
	}

/*
----------------------------------------------------------------------------------------------------
								End of Registering the origin
						Beginning of Attribute and effect application.
----------------------------------------------------------------------------------------------------
*/
	public void removeAll(@NotNull Player player) {
		removeAll(player, getOrigin(player));
	}
	private void removeAll(@NotNull Player player, @NotNull Origins origin) {
		for(PotionEffect potion : player.getActivePotionEffects()) {
			if(PotionUtils.isOriginEffect(potion)) {
				player.removePotionEffect(potion.getType());
			}
		}
		for(Attribute attribute : AttributeUtils.getPlayerAttributes()) {
			AttributeInstance instance = player.getAttribute(attribute);
			if (instance != null && AttributeUtils.isDefault(instance)) {
				instance.setBaseValue(AttributeUtils.getDefault(attribute));
			}
		}
	}

	public void addAll(@NotNull Player player) {
		addAll(player, getOrigin(player));
	}
	private void addAll(@NotNull Player player, @NotNull Origins origin) {
		me.racci.sylphia.origins.OriginValue originValue = getCondition(player);
		player.addPotionEffects(origin.getPotions(originValue));
		origin.getAttributes(originValue).forEach(var1x -> player.getAttribute(var1x.getAttribute()).setBaseValue(var1x.getValue()));
	}
	public void addPotions(@NotNull Player player) {
		addPotions(player, getOrigin(player));
	}
	private void addPotions(@NotNull Player player, @NotNull Origins origin) {
		player.addPotionEffects(origin.getPotions(getCondition(player)));
	}
//	public void addAttributes(@NotNull Player player) {
//		addAttributes(player, getOrigin(player).get);
//	}


	private me.racci.sylphia.origins.OriginValue getCondition(@NotNull Player player) {
		String var1 = switch (player.getWorld().getEnvironment()) {
			case NORMAL -> "O";
			case NETHER -> "N";
			case THE_END -> "E";
			default -> throw new UnsupportedOperationException();
		};
		if (var1.equals("O")) {
			var1 = switch ((WorldTime.isDay(player) ? 1 : 0)) {
				case 0 -> "OD";
				case 1 -> "ON";
				default -> throw new IllegalStateException("Unexpected value: " + (WorldTime.isDay(player) ? 1 : 0));
			};
		}
		var1 = switch (player.isInWaterOrBubbleColumn() || player.isInLava() ? 0 : 1) {
			case 0 -> var1 + "W";
			case 1 -> var1 + "L";
			default -> var1;
		};
		return me.racci.sylphia.origins.OriginValue.valueOf(var1);
	}



	@EventHandler
	public void playerOriginChangeEvent(OriginChangeEvent e) {
		this.applyAll(e.getPlayer(), e.getNewOrigin());
	}

	public void setOrigin(Player player, Origins newOrigin) {
		Origins oldOrigin = getOrigin(player);
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
					Component var1 = TextUtil.parseLegacy(player.getDisplayName()).colorIfAbsent(TextColor.fromHexString(Lang.getMessage(Colours.PLAYER)));
					Component playerComponent = var1.hoverEvent(var1);
					var1 = TextUtil.parseLegacy(newOrigin.getDisplayName());
					Component originComponent = var1.hoverEvent(var1);
					final Component broadcast = Component.text(Lang.getMessage(Origins.SELECT_BROADCAST))
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
					removeAll(player);
				}
			} else {
				event.setCancelled(true);
				Logger.log(Logger.Level.ERROR, "There was an Error setting " + player.getName() + "'s origin to " + newOrigin);
			}
		}
	}



	/*
	The below takes care of setting effects,
	and totally resetting the players effects.
	*/


	public void setAll(Player player) {
		Origins origin = getOrigin(player);
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

	public void setTest(Player player) {
		removeAll(player);
		Origins origin = getOrigin(player);
		OriginValue originValue = this.getCondition(player);
		player.addPotionEffects(origin.getPotions(originValue));
		for(OriginAttribute attribute : ListUtils.emptyIfNull(origin.getAttributes(originValue))) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}

	private void setGeneral(Player player, Origins origin) {
		for(PotionEffect potion : ListUtils.emptyIfNull(origin.getEffects())) {
			player.addPotionEffect(potion);
		}
		for(OriginAttribute attribute : ListUtils.emptyIfNull(origin.getAttributes())) {
			player.getAttribute(attribute.getAttribute()).setBaseValue(attribute.getValue());
		}
	}
	private void setTime(Player player, Origins origin) {
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
	private void setLiquid(Player player, Origins origin) {
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
		Origins origin = getOrigin(player);
		refreshTime(player, origin);
		refreshLiquid(player, origin);
	}
	public void refreshTime(Player player) {
		refreshTime(player, getOrigin(player));
	}
	public void refreshLiquid(Player player) {
		refreshLiquid(player, getOrigin(player));
	}


	private void refreshTime(Player player, Origins origin) {
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
		final Title title = Title.title(Component.empty(), TextUtil.parseLegacy(subtitle), times);
		player.showTitle(title);
	}
	private void refreshLiquid(Player player, Origins origin) {
		throw new UnsupportedOperationException();
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


	public void applyAll(Player player, Origins origin) {
		throw new UnsupportedOperationException();
	}

	public Map<Origins, String> getRequiredPermsList() {
		return requiredPermList;
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
	public Origins getOrigin(Player player) {
		return this.getOriginName(player) != null && originsMap.containsKey(this.getOriginName(player)) ? getOrigins().get(this.getOriginName(player)) : null;
	}
}
