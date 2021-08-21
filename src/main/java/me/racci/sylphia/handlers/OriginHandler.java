package me.racci.sylphia.handlers;

import dev.dbassett.skullcreator.SkullCreator;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.objects.Origin;
import me.racci.sylphia.objects.OriginAttribute;
import me.racci.sylphia.utils.Logger;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;


@SuppressWarnings({"RedundantOperationOnEmptyContainer", "deprecation", "unused"})
public class OriginHandler implements Listener {
	private final Sylphia plugin;
	final HashMap<String, Origin> origins = new HashMap<>();

	public OriginHandler(Sylphia plugin) {
		this.plugin = plugin;
		this.refreshOrigins();
	}

	public void refreshOrigins() {
		//this.originConfig.initOriginConfig(); // init race config
		this.origins.clear(); //empty the current list
		Map<String, YamlConfiguration> var1 = this.getAllOriginConfigurations(); //collects the origin settings?
		if (var1.isEmpty()) {
			Logger.log(Logger.LogLevel.ERROR, "No origins found!");
		} else {
			for (Entry<String, YamlConfiguration> stringYamlConfigurationEntry : var1.entrySet()) {
				if (!this.origins.containsKey(((Entry) stringYamlConfigurationEntry).getKey().toString())) { //maybe remove toString()
					this.origins.put(((Entry) stringYamlConfigurationEntry).getKey().toString().toUpperCase(), this.convertToOrigin((String) ((Entry) stringYamlConfigurationEntry).getKey(), (YamlConfiguration) ((Entry) stringYamlConfigurationEntry).getValue()));
				}
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	public Origin convertToOrigin(String origin, YamlConfiguration file) {

		String displayName;
		String name;
		String colour;
		String bracketName;
		Sound hurtSound;
		Sound deathSound;
		String dayMessage;
		String nightMessage;
		boolean effectsEnabled;
		boolean nightDayEnabled;
		boolean waterLavaEnabled;
		boolean slowFalling;
		boolean nightVision;
		int jumpBoost;
		boolean damageEnabled;
		boolean sunEnabled = false;
		boolean fallEnabled = false;
		boolean rainEnabled = false;
		boolean waterEnabled = false;
		boolean lavaEnabled = false;
		int sunDamage = 0;
		int fallDamage = 100;
		int rainDamage = 0;
		int waterDamage = 0;
		int lavaDamage = 100;

		ArrayList<PotionEffect> generalEffects = new ArrayList<>();
		ArrayList<OriginAttribute> generalAttributes = new ArrayList<>();
		ArrayList<PotionEffect> dayEffects = new ArrayList<>();
		ArrayList<OriginAttribute> dayAttributes = new ArrayList<>();
		ArrayList<PotionEffect> nightEffects = new ArrayList<>();
		ArrayList<OriginAttribute> nightAttributes = new ArrayList<>();
		ArrayList<PotionEffect> waterEffects = new ArrayList<>();
		ArrayList<OriginAttribute> waterAttributes = new ArrayList<>();
		ArrayList<PotionEffect> lavaEffects = new ArrayList<>();
		ArrayList<OriginAttribute> lavaAttributes = new ArrayList<>();

		String displayNamePath = "Display-Name";
		String namePath = "Name";
		String colourPath = "Origin-Colour";
		String hurtSoundPath = "Hurt-Sound";
		String deathSoundPath = "Death-Sound";
		String dayMessagePath = "Day/Night.Day";
		String nightMessagePath = "Day/Night.Night";
		String effectsEnabledPath = "Effects.Enables.General-Enabled";
		String nightDayEnabledPath = "Effects.Enables.Night/Day-Enabled";
		String waterLavaEnabledPath = "Effects.Enables.Water/Lava-Enabled";
		String slowFallingPath = "Effects.Effects.Slow-falling";
		String nightVisionPath = "Effects.Effects.Night-vision";
		String jumpBoostPath = "Effects.Effects.Jump-boost";
		String damageEnabledPath = "Damage.Enables.General-Enabled";
		String sunEnabledPath = "Damage.Enables.Direct-Sun-Enabled";
		String fallEnabledPath = "Damage.Enables.Fall=Enabled";
		String fallDamagePath = "Damage.Amounts.Fall";
		String rainEnabledPath = "Damage.Enables.Rain-Enabled";
		String rainDamagePath = "Damage.Amounts.Rain";
		String waterEnabledPath = "Damage.Enables.Water-Enabled";
		String waterDamagePath = "Damage.Amounts.Water";
		String lavaEnabledPath = "Damage.Enables.Fire-Enabled";
		String lavaDamagePath = "Damage.Amounts.Lava";
		String generalAttributesPath = "Effects.General.Attributes";
		String dayAttributesPath = "Effects.Day/Night.Day-Attributes";
		String nightAttributesPath = "Effects.Day/Night.Night-Attributes";
		String waterAttributesPath = "Effects.Water/Lava.Water-Attributes";
		String lavaAttributesPath = "Effects.Water/Lava.Lava-Attributes";
		String generalEffectsPath = "Effects.General.Effects";
		String dayEffectsPath = "Effects.Day/Night.Day-Effects";
		String nightEffectsPath = "Effects.Day/Night.Night-Effects";
		String waterEffectsPath = "Effects.Water/Lava.Water-Effects";
		String lavaEffectsPath = "Effects.Water/Lava.Lava-Effects";
		String GUIEnchantedPath = "GUI.Enchanted";
		String GUIHeadPath = "GUI.Skull";
		String GUIItemPath = "GUI.Material";
		String GUILorePath = "GUI.Lore";


		if (!file.isSet(displayNamePath)) {
			file.set(displayNamePath, origin.toUpperCase());
			displayName = origin.toUpperCase();
		} else {
			displayName = ChatColor.color(file.getString(displayNamePath), true);
		}



		if (!file.isSet(namePath)) {
			file.set(namePath, origin.toUpperCase());
			name = origin.toUpperCase();
		} else {
			name = file.getString(namePath);
		}



		if (!file.isSet(colourPath)) {
			file.set(colourPath, "&8");
			colour = "&8";
		} else {
			colour = ChatColor.color(ChatColor.replaceHex(Objects.requireNonNull(file.get(colourPath)).toString()), true);
		}


		bracketName = (colour + "[" + name + "]");



		if (!file.isSet(hurtSoundPath)) {
			file.set(hurtSoundPath, "ENTITY_PLAYER_HURT");
			hurtSound = Sound.ENTITY_PLAYER_HURT;
		} else {
			hurtSound = Sound.valueOf(file.getString(hurtSoundPath));
		}



		if (!file.isSet(deathSoundPath)) {
			file.set(deathSoundPath, "ENTITY_PLAYER_DEATH");
			deathSound = Sound.ENTITY_PLAYER_DEATH;
		} else {
			deathSound = Sound.valueOf(file.getString(deathSoundPath));
		}



		if (!file.isSet(dayMessagePath)) {
			file.set(dayMessagePath, "&eA new day has begun!");
			dayMessage = "&eA new day has begun!";
		} else {
			dayMessage = ChatColor.color(file.getString(dayMessagePath), true);
		}



		if (!file.isSet(nightMessagePath)) {
			file.set(nightMessagePath, "&3The night has come upon us..");
			nightMessage = "&3The night has come upon us..";
		} else {
			nightMessage = ChatColor.color(file.getString(nightMessagePath), true);
		}



		if (!file.isSet(effectsEnabledPath)) {
			file.set(effectsEnabledPath, false);
			effectsEnabled = false;
		} else {
			effectsEnabled = file.getBoolean(effectsEnabledPath);
		}



		if (!file.isSet(nightDayEnabledPath)) {
			file.set(nightDayEnabledPath, false);
			nightDayEnabled = false;
		} else {
			nightDayEnabled = file.getBoolean(nightDayEnabledPath);
		}



		if (!file.isSet(waterLavaEnabledPath)) {
			file.set(waterLavaEnabledPath, false);
			waterLavaEnabled = false;
		} else {
			waterLavaEnabled = file.getBoolean(waterLavaEnabledPath);
		}



		if (!file.isSet(slowFallingPath)) {
			file.set(slowFallingPath, false);
			slowFalling = false;
		} else {
			slowFalling = file.getBoolean(slowFallingPath);
		}



		if (!file.isSet(nightVisionPath)) {
			file.set(nightVisionPath, false);
			nightVision = false;
		} else {
			nightVision = file.getBoolean(nightVisionPath);
		}



		if (!file.isSet(jumpBoostPath)) {
			file.set(jumpBoostPath, 0);
			jumpBoost = 0;
		} else {
			jumpBoost = file.getInt(jumpBoostPath);
		}



		if (!file.isSet(damageEnabledPath)) {
			file.set(damageEnabledPath, false);
			damageEnabled = false;
		} else {
			damageEnabled = file.getBoolean(damageEnabledPath);
		}



		if (damageEnabled) {
			if (!file.isSet(sunEnabledPath)) {
				file.set(sunEnabledPath, false);
			} else {
				sunEnabled = file.getBoolean(sunEnabledPath);
			}

			if (sunEnabled) {
				String sunDamagePath = "Damage.Amounts.Sun";
				if (!file.isSet(sunDamagePath)) {
					file.set(sunDamagePath, 0);
				} else {
					sunDamage = file.getInt(sunDamagePath);
				}
			}


			if (!file.isSet(fallEnabledPath)) {
				file.set(fallEnabledPath, false);
			} else {
				fallEnabled = file.getBoolean(fallEnabledPath);
			}

			if (fallEnabled) {

				if (!file.isSet(fallDamagePath)) {
					file.set(fallDamagePath, 100);
				} else {
					fallDamage = file.getInt(fallDamagePath);
				}
			}


			if (!file.isSet(rainEnabledPath)) {
				file.set(rainEnabledPath, false);
			} else {
				rainEnabled = file.getBoolean(rainEnabledPath);
			}

			if (rainEnabled) {

				if (!file.isSet(rainDamagePath)) {
					file.set(rainDamagePath, 0);
				} else {
					rainDamage = file.getInt(rainDamagePath);
				}
			}


			if (!file.isSet(waterEnabledPath)) {
				file.set(waterEnabledPath, false);
			} else {
				waterEnabled = file.getBoolean(waterEnabledPath);
			}

			if (waterEnabled) {

				if (!file.isSet(waterDamagePath)) {
					file.set(waterDamagePath, 0);
				} else {
					waterDamage = file.getInt(waterDamagePath);
				}
			}


			if (!file.isSet(lavaEnabledPath)) {
				file.set(lavaEnabledPath, false);
			} else {
				lavaEnabled = file.getBoolean(lavaEnabledPath);
			}

			if (lavaEnabled) {

				if (!file.isSet(lavaDamagePath)) {
					file.set(lavaDamagePath, 100);
				} else {
					lavaDamage = file.getInt(lavaDamagePath);
				}
			}
		}


		Iterator<String> attributeIterator;
		String attributeVar0;
		String[] attributeVar1;

		Iterator<String> potionIterator;
		String potionVar0;
		String[] potionVar1;

		final String theDouble = "The Double of ";
		final String theAttribute = "The Attribute of ";
		final String invalidDouble = " isn't a valid Double.";
		final String invalidAttribute = " isn't a valid Attribute.";
		final String forOrigin = " for origin ";

		final String theInteger = "The Integer of ";
		final String thePotion = "The Potion of ";
		final String invalidInteger = " isn't a valid Integer.";
		final String invalidPotion = " isn't a valid Potion.";

		if(effectsEnabled) {
			if(!file.getStringList(generalAttributesPath).isEmpty()) {
				attributeIterator = file.getStringList(generalAttributesPath).iterator();
				while (attributeIterator.hasNext()) {
					attributeVar0 = attributeIterator.next();
					attributeVar1 = attributeVar0.split(":");
					if (EnumUtils.isValidEnum(Attribute.class, attributeVar1[0])) {
						if (NumberUtils.isParsable(attributeVar1[1])) {
							generalAttributes.add(new OriginAttribute(Attribute.valueOf(attributeVar1[0]), Double.parseDouble(attributeVar1[1])));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theDouble + attributeVar1[0] + forOrigin + origin + invalidDouble);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, theAttribute + attributeVar1[0] + forOrigin + origin + invalidAttribute);
					}
				}
			}
			if(!file.getStringList(generalEffectsPath).isEmpty()) {
				potionIterator = file.getStringList(generalEffectsPath).iterator();
				while (potionIterator.hasNext()) {
					potionVar0 = potionIterator.next();
					potionVar1 = potionVar0.split(":");
					if (PotionEffectType.getByName(potionVar1[0]) != null) {
						if (NumberUtils.isParsable(potionVar1[1])) {
							generalEffects.add(new PotionEffect(PotionEffectType.getByName(potionVar1[0]), Integer.MIN_VALUE, Integer.parseInt(potionVar1[1]), true, false));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theInteger + potionVar1[0] + forOrigin + origin + invalidInteger);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, thePotion + potionVar1[0] + forOrigin + origin + invalidPotion);
					}
				}
			}
		} else {
			if (!file.getStringList(generalEffectsPath).isEmpty()) {
				generalEffects.clear();
			}
			if (!file.getStringList(generalAttributesPath).isEmpty()) {
				generalAttributes.clear();
			}
		}

		if(nightDayEnabled) {
			if(!file.getStringList(dayAttributesPath).isEmpty()) {
				attributeIterator = file.getStringList(dayAttributesPath).iterator();
				while (attributeIterator.hasNext()) {
					attributeVar0 = attributeIterator.next();
					attributeVar1 = attributeVar0.split(":");
					if (EnumUtils.isValidEnum(Attribute.class, attributeVar1[0])) {
						if (NumberUtils.isParsable(attributeVar1[1])) {
							dayAttributes.add(new OriginAttribute(Attribute.valueOf(attributeVar1[0]), Double.parseDouble(attributeVar1[1])));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theDouble + attributeVar1[0] + forOrigin + origin + invalidDouble);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, theAttribute + attributeVar1[0] + forOrigin + origin + invalidAttribute);
					}
				}
			}
			if(!file.getStringList(nightAttributesPath).isEmpty()) {
				attributeIterator = file.getStringList(nightAttributesPath).iterator();
				while (attributeIterator.hasNext()) {
					attributeVar0 = attributeIterator.next();
					attributeVar1 = attributeVar0.split(":");
					if (EnumUtils.isValidEnum(Attribute.class, attributeVar1[0])) {
						if (NumberUtils.isParsable(attributeVar1[1])) {
							nightAttributes.add(new OriginAttribute(Attribute.valueOf(attributeVar1[0]), Double.parseDouble(attributeVar1[1])));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theDouble + attributeVar1[0] + forOrigin + origin + invalidDouble);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, theAttribute + attributeVar1[0] + forOrigin + origin + invalidAttribute);
					}
				}
			}
			if(!file.getStringList(dayEffectsPath).isEmpty()) {
				potionIterator = file.getStringList(dayEffectsPath).iterator();
				while (potionIterator.hasNext()) {
					potionVar0 = potionIterator.next();
					potionVar1 = potionVar0.split(":");
					if (PotionEffectType.getByName(potionVar1[0]) != null) {
						if (NumberUtils.isParsable(potionVar1[1])) {
							dayEffects.add(new PotionEffect(PotionEffectType.getByName(potionVar1[0]), Integer.MIN_VALUE, Integer.parseInt(potionVar1[1]), true, false));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theInteger + potionVar1[0] + forOrigin + origin + invalidInteger);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, thePotion + potionVar1[0] + forOrigin + origin + invalidPotion);
					}
				}
			}
			if(!file.getStringList(nightEffectsPath).isEmpty()) {
				potionIterator = file.getStringList(nightEffectsPath).iterator();
				while (potionIterator.hasNext()) {
					potionVar0 = potionIterator.next();
					potionVar1 = potionVar0.split(":");
					if (PotionEffectType.getByName(potionVar1[0]) != null) {
						if (NumberUtils.isParsable(potionVar1[1])) {
							nightEffects.add(new PotionEffect(PotionEffectType.getByName(potionVar1[0]), Integer.MIN_VALUE, Integer.parseInt(potionVar1[1]), true, false));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theInteger + potionVar1[0] + forOrigin + origin + invalidInteger);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, thePotion + potionVar1[0] + forOrigin + origin + invalidPotion);
					}
				}
			}
		} else {
			if (!file.getStringList(dayEffectsPath).isEmpty()) {
				dayEffects.clear();
			}
			if (!file.getStringList(dayAttributesPath).isEmpty()) {
				dayAttributes.clear();
			}
			if (!file.getStringList(nightEffectsPath).isEmpty()) {
				nightEffects.clear();
			}
			if (!file.getStringList(nightAttributesPath).isEmpty()) {
				nightAttributes.clear();
			}
		}

		if(waterLavaEnabled) {
			if(!file.getStringList(waterAttributesPath).isEmpty()) {
				attributeIterator = file.getStringList(waterAttributesPath).iterator();
				while (attributeIterator.hasNext()) {
					attributeVar0 = attributeIterator.next();
					attributeVar1 = attributeVar0.split(":");
					if (EnumUtils.isValidEnum(Attribute.class, attributeVar1[0])) {
						if (NumberUtils.isParsable(attributeVar1[1])) {
							waterAttributes.add(new OriginAttribute(Attribute.valueOf(attributeVar1[0]), Double.parseDouble(attributeVar1[1])));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theDouble + attributeVar1[0] + forOrigin + origin + invalidDouble);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, theAttribute + attributeVar1[0] + forOrigin + origin + invalidAttribute);
					}
				}
			}
			if(!file.getStringList(lavaAttributesPath).isEmpty()) {
				attributeIterator = file.getStringList(lavaAttributesPath).iterator();
				while (attributeIterator.hasNext()) {
					attributeVar0 = attributeIterator.next();
					attributeVar1 = attributeVar0.split(":");
					if (EnumUtils.isValidEnum(Attribute.class, attributeVar1[0])) {
						if (NumberUtils.isParsable(attributeVar1[1])) {
							lavaAttributes.add(new OriginAttribute(Attribute.valueOf(attributeVar1[0]), Double.parseDouble(attributeVar1[1])));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theDouble + attributeVar1[0] + forOrigin + origin + invalidDouble);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, theAttribute + attributeVar1[0] + forOrigin + origin + invalidAttribute);
					}
				}
			}
			if(!file.getStringList(waterEffectsPath).isEmpty()) {
				potionIterator = file.getStringList(waterEffectsPath).iterator();
				while (potionIterator.hasNext()) {
					potionVar0 = potionIterator.next();
					potionVar1 = potionVar0.split(":");
					if (PotionEffectType.getByName(potionVar1[0]) != null) {
						if (NumberUtils.isParsable(potionVar1[1])) {
							waterEffects.add(new PotionEffect(PotionEffectType.getByName(potionVar1[0]), Integer.MIN_VALUE, Integer.parseInt(potionVar1[1]), true, false));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theInteger + potionVar1[0] + forOrigin + origin + invalidInteger);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, thePotion + potionVar1[0] + forOrigin + origin + invalidPotion);
					}
				}
			}
			if(!file.getStringList(lavaEffectsPath).isEmpty()) {
				potionIterator = file.getStringList(lavaEffectsPath).iterator();
				while (potionIterator.hasNext()) {
					potionVar0 = potionIterator.next();
					potionVar1 = potionVar0.split(":");
					if (PotionEffectType.getByName(potionVar1[0]) != null) {
						if (NumberUtils.isParsable(potionVar1[1])) {
							lavaEffects.add(new PotionEffect(PotionEffectType.getByName(potionVar1[0]), Integer.MIN_VALUE, Integer.parseInt(potionVar1[1]), true, false));
						} else {
							Logger.log(Logger.LogLevel.WARNING, theInteger + potionVar1[0] + forOrigin + origin + invalidInteger);
						}
					} else {
						Logger.log(Logger.LogLevel.WARNING, thePotion + potionVar1[0] + forOrigin + origin + invalidPotion);
					}
				}
			}
		} else {
			if (!file.getStringList(waterEffectsPath).isEmpty()) {
				waterEffects.clear();
			}
			if (!file.getStringList(waterAttributesPath).isEmpty()) {
				waterAttributes.clear();
			}
			if (!file.getStringList(lavaEffectsPath).isEmpty()) {
				lavaEffects.clear();
			}
			if (!file.getStringList(lavaAttributesPath).isEmpty()) {
				lavaAttributes.clear();
			}
		}

		ItemStack material = null;
		if(file.isSet(GUIHeadPath)) {
			material = SkullCreator.itemFromBase64(GUIHeadPath);
		} else {
			if(!file.isSet(GUIItemPath)) {
				Logger.log(Logger.LogLevel.ERROR, "The GUI Material for " + origin + " is null and does not have a skull set.");
			} else {
				if (!EnumUtils.isValidEnum(Material.class, file.getString(GUIItemPath))) {
					Logger.log(Logger.LogLevel.ERROR, "The given GUI Material for " + origin + " isn't a valid material, setting it to an Apple");
					material = new ItemStack((Material.APPLE), 1);
				} else {
					material = new ItemStack((Material.getMaterial(file.getString(GUIItemPath))), 1);
				}
			}
		}
		if(material != null) {
			ItemMeta itemMeta = material.getItemMeta();
			if(file.isSet(GUILorePath)) {
				List<String> itemLore = file.getStringList(GUILorePath);
				itemLore.replaceAll(var0 -> {
					ChatColor.color(var0, true);
					return var0;
				});
				itemMeta.setLore(itemLore);
			}
			if(file.isSet(GUIEnchantedPath)) {
				if(file.getBoolean(GUIEnchantedPath)) {
					itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
					itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
				} else {
					Logger.log(Logger.LogLevel.WARNING, "The given value for GUI.Enchanted in " + origin + " isn't a valid boolean.");
				}
			}
			itemMeta.displayName(Component.text(displayName));
		}

		List<String> requiredPerms = file.getStringList("Permissions.Required");
		List<String> givenPerms = file.getStringList("Permissions.Given");

		return new Origin(name, displayName, bracketName, colour, hurtSound, deathSound, requiredPerms, givenPerms, effectsEnabled, nightDayEnabled, waterLavaEnabled, generalEffects, waterEffects, lavaEffects, dayEffects, nightEffects, generalAttributes, waterAttributes, lavaAttributes, dayAttributes, nightAttributes, slowFalling, nightVision, jumpBoost, damageEnabled, sunEnabled, fallEnabled, rainEnabled, waterEnabled, lavaEnabled, sunDamage, fallDamage, rainDamage, waterDamage, lavaDamage, material, dayMessage, nightMessage);
	}


	public void applyGeneralEffects(Player player) {
		Origin origin = this.getOrigin(player);
		if(origin.isGeneralEnabled()) {
			if(origin.getGeneralEffects() != null && !origin.getGeneralEffects().isEmpty()) {
				for (PotionEffect potionEffect : origin.getGeneralEffects()) {
					player.addPotionEffect(potionEffect);
				}
			}
			if(origin.getGeneralAttributes() != null && !origin.getGeneralAttributes().isEmpty()) {
				for (OriginAttribute attribute : origin.getGeneralAttributes()) {
					Attribute var1 = attribute.getAttribute();
					double var2 = attribute.getValue();
					//noinspection ConstantConditions
					player.getAttribute(var1).setBaseValue(var2);
				}
			}
		}
	}
//	public void applyTimeEffects(Player player) {
//		Origin origin = this.getOrigin(player);
//		WorldTime worldTime = new WorldTime();
//		if(origin.isNightDayEnabled()) {
//			if(worldTime.isDay(player)) {
//				if(origin.getDayEffects() != null && !origin.getDayEffects().isEmpty()) {
//
//				}
//			}
//		}
//	}



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

	public Map<String, YamlConfiguration> getAllOriginConfigurations() {
		HashMap<String, YamlConfiguration> origins = new HashMap<>();
		File[] Files;
		Files = new File(this.plugin.getDataFolder().getAbsolutePath() + "/Origins").listFiles();
		if (Files != null) {
			int amount = Files.length;
			for (File origin : Files) {
				if (!origin.isDirectory()) {
					origins.put(origin.getName().toLowerCase().replace(".yml", ""), YamlConfiguration.loadConfiguration(origin));
				}
			}
		}
		return origins;
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

}

