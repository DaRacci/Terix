package me.racci.sylphia.utils.minecraft;

import me.racci.sylphia.utils.Logger;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Color;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PotionUtils {

	private PotionUtils() {
	}

	private static ArrayList<PotionEffectType> potionEffectTypes = new ArrayList<>();
	static {
		for(PrivatePotionEffectType potion : PrivatePotionEffectType.values()) {
			potionEffectTypes.add(potion.getEffect());
		}
	}

	@Nullable
	public static PotionEffect parseOriginPotion(@NotNull String string) {
		String[] potion = string.split(":");
		if(isValid(potion[0])) {
			if(NumberUtils.isParsable(potion[1])) {
				return new PotionEffect(Objects.requireNonNull(PotionEffectType.getByName(potion[0])), Integer.MAX_VALUE, Integer.parseInt(potion[1]), true, false, false);
			} else {
				Logger.log(Logger.Level.WARNING, "Invalid Integer " + potion[1] + " for " + potion[0]);
			}
		} else {
			Logger.log(Logger.Level.WARNING, "Invalid Potion " + potion[0]);
		}
		return null;
	}

	public static boolean isValid(@NotNull String string) {
		return EnumUtils.isValidEnumIgnoreCase(PrivatePotionEffectType.class, string);
	}
	public static boolean isValidLevel(@NotNull PotionEffect potion) {
		return potion.getAmplifier()<= PrivatePotionEffectType.valueOf(potion.getType().toString()).getMax();
	}
	public static boolean isInfinite(@NotNull PotionEffect potion) {
		return potion.getDuration()>=86400;
	}
	public static boolean isOriginEffect(@NotNull PotionEffect potion) {
		return (!potion.hasIcon() || potion.isAmbient() || potion.getDuration() >= 86400);
	}

	public static List<PotionEffectType> getPotionTypes() {
		return potionEffectTypes;
	}

	public static PotionEffect getHigherPotion(@NotNull PotionEffect var1, @NotNull PotionEffect var2) {
		return (var1.getAmplifier() > var2.getAmplifier() ? var1 : var2);
	}
	public static PotionEffect getLowerPotion(@NotNull PotionEffect var1, @NotNull PotionEffect var2) {
		return (var1.getAmplifier() < var2.getAmplifier() ? var1 : var2);
	}

}
enum PrivatePotionEffectType {
	ABSORPTION("Absorption", 100, PotionEffectType.ABSORPTION),
	BAD_OMEN("Bad Omen", 1, PotionEffectType.BAD_OMEN),
	BLINDNESS("Blindness", 6, PotionEffectType.BLINDNESS),
	CONDUIT_POWER("Conduit Power", 6, PotionEffectType.CONDUIT_POWER),
	CONFUSION("Nausea", 1, PotionEffectType.CONFUSION),
	DAMAGE_RESISTANCE("Resistance", 6, PotionEffectType.DAMAGE_RESISTANCE),
	DOLPHINS_GRACE("Dolphins Grace", 1, PotionEffectType.DOLPHINS_GRACE),
	FAST_DIGGING("Haste", 6, PotionEffectType.FAST_DIGGING),
	FIRE_RESISTANCE("Fire Resistance", 1, PotionEffectType.FIRE_RESISTANCE),
	GLOWING("Glowing", 1, PotionEffectType.GLOWING),
	HARM("Instant Damage", 4, PotionEffectType.HARM),
	HEAL("Instant Health", 4, PotionEffectType.HEAL),
	HEALTH_BOOST("Health Boost", 100, PotionEffectType.HEALTH_BOOST),
	HERO_OF_THE_VILLAGE("Hero of the Village", 4, PotionEffectType.HERO_OF_THE_VILLAGE),
	HUNGER("Hunger", 4, PotionEffectType.HUNGER),
	INCREASE_DAMAGE("Strength", 12, PotionEffectType.INCREASE_DAMAGE),
	INVISIBILITY("Invisibility", 1, PotionEffectType.INVISIBILITY),
	JUMP("Jump Boost", 12, PotionEffectType.JUMP),
	LEVITATION("Levitation", 1, PotionEffectType.LEVITATION),
	LUCK("Luck", 100, PotionEffectType.LUCK),
	NIGHT_VISION("Night Vision", 1, PotionEffectType.NIGHT_VISION),
	POISON("Poison", 12, PotionEffectType.POISON),
	REGENERATION("Regeneration", 12, PotionEffectType.REGENERATION),
	SATURATION("Saturation", 4, PotionEffectType.SATURATION),
	SLOW("Slowness", 4, PotionEffectType.SLOW),
	SLOW_DIGGING("Mining Fatigue", 4, PotionEffectType.SLOW_DIGGING),
	SLOW_FALLING("Slow Falling", 1, PotionEffectType.SLOW_FALLING),
	SPEED("Speed", 12, PotionEffectType.SPEED),
	UNLUCK("????", 100, PotionEffectType.UNLUCK),
	WATER_BREATHING("Water Breathing", 1, PotionEffectType.WATER_BREATHING),
	WEAKNESS("Weakness", 12, PotionEffectType.WEAKNESS),
	WITHER("Wither", 12, PotionEffectType.WITHER);


	private final String name;
	private final Color colour;
	private final PotionEffectType potionEffectType;
	private final Integer maxLevel;

	PrivatePotionEffectType(String name, Integer maxLevel, @NotNull PotionEffectType potionEffectType) {
		this.name = name;
		this.colour = potionEffectType.getColor();
		this.maxLevel = maxLevel;
		this.potionEffectType = potionEffectType;
	}
	public String getName() {
		return this.name;
	}
	public Color getColour() {
		return this.colour;
	}
	public PotionEffectType getEffect() {
		return this.potionEffectType;
	}
	public Integer getMax() {
		return this.maxLevel;
	}

}