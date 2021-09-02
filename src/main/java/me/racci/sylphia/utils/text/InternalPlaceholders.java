package me.racci.sylphia.utils.text;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Prefix;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InternalPlaceholders {
	Sylphia plugin;

	public static String parsePlaceholder(String source) {
		if (source == null) {
			return null;
		}
		final String regex = "\\{(\\S+)}";
		final Pattern pattern = Pattern.compile((regex), Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(source);
		while (matcher.find()) {
			String var1 = matcher.group();
			int int1 = (new Random()).nextInt(2147483647);
			String var2 = "|" + int1 + "|";
			source = source.replaceFirst(Pattern.quote(var1), Matcher.quoteReplacement(var2));
		}
		return source;
	}

	 public Object getValue(Player player, TextUtil.Placeholder placeholder) {
		return getValue(player, placeholder, false);
	}

	public Object getValue(Player player, TextUtil.Placeholder placeholder, Boolean component) {
		String var1 = null;
		if (placeholder == null) {
			return null;
		} else {
			switch (placeholder.ordinal()) {
				case 1:
					var1 = Lang.getMessage(Prefix.SYLPHIA);
					break;
				case 2:
					var1 = Lang.getMessage(Prefix.ORIGINS);
					break;
				case 3:
					var1 = Lang.getMessage(Prefix.ERROR);
					break;
				case 4:
					var1 = player.getName();
					break;
				case 5:
					var1 = player.getDisplayName();
					break;
				case 6:
					var1 = plugin.getOriginHandler().getOrigin(player).getName();
					break;
				case 7:
					var1 = plugin.getOriginHandler().getOrigin(player).getDisplayName();
					break;
				case 8:
				case 9:
				case 10:
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + placeholder);
			}
			// Returning Component for Kyori builder
			if(component && var1 != null) {
				return Component.text()
						.append(TextUtil.parseLegacy(var1)).build();
			// Return normal String
			} else {
				return var1;
			}
		}
	}


//	public enum Placeholder {
//		// Prefixes
//		PREFIX_SYLPHIA,
//		PREFIX_ORIGINS,
//		PREFIX_ERROR,
//		// Player
//		PLAYER,
//		PLAYER_DISPLAY,
//		// Origins
//		ORIGIN,
//		ORIGIN_DISPLAY,
//	}
}
