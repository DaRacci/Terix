package me.racci.sylphia.utils;

import me.racci.sylphia.origins.objects.OriginAttribute;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Parser {

	private Parser() {
	}

	public static Attribute parseAttribute(String string) {
		if (!EnumUtils.isValidEnum(Attribute.class, string)) {
			return null;
		} else {
			return Attribute.valueOf(string);
		}
	}

	public static OriginAttribute parseOriginAttribute(String string) {
		if (string == null) {
			return null;
		} else {
			String[] attribute = string.split(":");
			if (!EnumUtils.isValidEnum(Attribute.class, attribute[0])) {
				Logger.log(Logger.LogLevel.WARNING, "The given attribute for " + attribute[0] + " is invalid!");
				return null;
			} else {
				if (!NumberUtils.isParsable(attribute[1])) {
					Logger.log(Logger.LogLevel.WARNING, "The given double for " + attribute[0] + " is invalid!");
					return null;
				} else {
					return new OriginAttribute(Attribute.valueOf(attribute[0]), Double.parseDouble(attribute[1]));
				}
			}
		}
	}


	public static PotionEffect parseEffect(String string) {
		String[] potion = string.split(":");
		if(PotionEffectType.getByName(potion[0]) != null) {
			if(NumberUtils.isParsable(potion[1])) {
				return new PotionEffect(PotionEffectType.getByName(potion[0]), Integer.MAX_VALUE, Integer.parseInt(potion[1]), true, false, false);
			} else {
				Logger.log(Logger.LogLevel.WARNING, "The given value for " + potion[0] + " is invalid!");
				return null;
			}
		} else {
			Logger.log(Logger.LogLevel.WARNING, "The given Potion " + potion[0] + " is invalid!");
			return null;
		}
	}

}
