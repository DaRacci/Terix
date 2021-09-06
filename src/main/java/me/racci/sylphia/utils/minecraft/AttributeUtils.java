package me.racci.sylphia.utils.minecraft;

import me.racci.sylphia.origins.objects.OriginAttribute;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AttributeUtils {

	private static final ArrayList<Attribute> playerAttributes = new ArrayList<>();
	static {
		for(PrivateAttribute attribute : PrivateAttribute.values()) {
			playerAttributes.add(attribute.getAttribute());
		}
	}


	private AttributeUtils() { }

	@Nullable
	public static OriginAttribute parseOriginAttribute(@NotNull String string) {
		String[] attribute = string.split(":");
		if(isValid(attribute[0])) {
			if (NumberUtils.isParsable(attribute[1])) {
				return new OriginAttribute(Attribute.valueOf(attribute[0]), Double.parseDouble(attribute[1]));
			} else {
				Logger.log(Logger.Level.WARNING, "Invalid Double " + attribute[1] + " for " + attribute[0]);
			}
		} else {
			Logger.log(Logger.Level.WARNING, "Invalid Attribute " + attribute[0]);
		}
		return null;
	}

	public static boolean isValid(String string) {
		return EnumUtils.isValidEnumIgnoreCase(Attribute.class, string);
	}

	public static boolean isDefault(@NotNull AttributeInstance attribute) {
		return getDefault(attribute.getAttribute()) == attribute.getBaseValue();
	}
	public static double getDefault(Attribute attribute) {
		return switch(attribute) {
			case GENERIC_MAX_HEALTH -> 20.0;
			case GENERIC_MOVEMENT_SPEED -> 0.1;
			case GENERIC_ATTACK_DAMAGE -> 2.0;
			case GENERIC_ATTACK_SPEED -> 4.0;
			case GENERIC_KNOCKBACK_RESISTANCE, GENERIC_ATTACK_KNOCKBACK, GENERIC_ARMOR_TOUGHNESS, GENERIC_ARMOR, GENERIC_LUCK, GENERIC_FOLLOW_RANGE, GENERIC_FLYING_SPEED, HORSE_JUMP_STRENGTH, ZOMBIE_SPAWN_REINFORCEMENTS -> 0.0;
		};
	}

	public static List<Attribute> getPlayerAttributes() {
		return playerAttributes;
	}

	public static AttributeInstance getHigherAttribute(@NotNull AttributeInstance var1, @NotNull AttributeInstance var2) {
		return (var1.getBaseValue()>var2.getBaseValue()?var1:var2);
	}
	public static AttributeInstance getLowerAttribute(@NotNull AttributeInstance var1, @NotNull AttributeInstance var2) {
		return (var1.getBaseValue()<var2.getBaseValue()?var1:var2);
	}


}
enum PrivateAttribute {
	GENERIC_MAX_HEALTH(20.0, 0.0, 1024.0, Attribute.GENERIC_MAX_HEALTH),
	GENERIC_MOVEMENT_SPEED(0.1, 0.0, 1024.0, Attribute.GENERIC_MOVEMENT_SPEED),
	GENERIC_ATTACK_DAMAGE(2.0, 0.0, 2048.0, Attribute.GENERIC_ATTACK_DAMAGE),
	GENERIC_ATTACK_SPEED(4.0, 0.0, 1024.0, Attribute.GENERIC_ATTACK_SPEED),
	GENERIC_KNOCKBACK_RESISTANCE(0.0, 0.0, 1.0, Attribute.GENERIC_KNOCKBACK_RESISTANCE),
	GENERIC_ATTACK_KNOCKBACK(0.0, 0.0, 5.0, Attribute.GENERIC_ATTACK_KNOCKBACK),
	GENERIC_ARMOR_TOUGHNESS(0.0, 0.0, 20.0, Attribute.GENERIC_ARMOR_TOUGHNESS),
	GENERIC_ARMOR(0.0, 0.0, 30.0, Attribute.GENERIC_ARMOR),
	GENERIC_LUCK(0.0, -1024.0, 1024.0, Attribute.GENERIC_LUCK);

	private final Double defaultLevel;
	private final Double minLevel;
	private final Double maxLevel;
	private final Attribute attribute;

	PrivateAttribute(Double defaultLevel, Double minLevel, Double maxLevel, Attribute attribute) {
		this.defaultLevel = defaultLevel;
		this.minLevel = minLevel;
		this.maxLevel = maxLevel;
		this.attribute = attribute;
	}

	public Double getDefaultLevel() {
		return defaultLevel;
	}
	public Double getMinLevel() {
		return minLevel;
	}
	public Double getMaxLevel() {
		return maxLevel;
	}
	public Attribute getAttribute() {
		return attribute;
	}
}