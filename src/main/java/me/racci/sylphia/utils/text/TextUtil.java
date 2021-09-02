package me.racci.sylphia.utils.text;

import co.aikar.commands.annotation.Optional;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Prefix;
import me.racci.sylphia.utils.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
	static Sylphia plugin;
	private static final String regex = "\\{(\\S+)}";
	private static final Pattern pattern = Pattern.compile((regex), Pattern.CASE_INSENSITIVE);


	private TextUtil() {
	}

	public static String replace(String source, String os, String ns) {
		if (source == null) {
			return null;
		}
		int i = 0;
		if ((i = source.indexOf(os, i)) >= 0) {
			char[] sourceArray = source.toCharArray();
			char[] nsArray = ns.toCharArray();
			int oLength = os.length();
			StringBuilder buf = new StringBuilder (sourceArray.length);
			buf.append (sourceArray, 0, i).append(nsArray);
			i += oLength;
			int j = i;
			// Replace all remaining instances of oldString with newString.
			while ((i = source.indexOf(os, i)) > 0) {
				buf.append (sourceArray, j, i - j).append(nsArray);
				i += oLength;
				j = i;
			}
			buf.append (sourceArray, j, sourceArray.length - j);
			source = buf.toString();
			buf.setLength(0);
		}
		return source;
	}

	public static String replace(String source, String os1, String ns1, String os2, String ns2) {
		return replace(replace(source, os1, ns1), os2, ns2);
	}

	public static String replace(String source, String os1, String ns1, String os2, String ns2, String os3, String ns3) {
		return replace(replace(replace(source, os1, ns1), os2, ns2), os3, ns3);
	}

	public static String replace(String source, String os1, String ns1, String os2, String ns2, String os3, String ns3, String os4, String ns4) {
		return replace(replace(replace(replace(source, os1, ns1), os2, ns2), os3, ns3), os4, ns4);
	}

	public static String replace(String source, String os1, String ns1, String os2, String ns2, String os3, String ns3, String os4, String ns4, String os5, String ns5) {
		return replace(replace(replace(replace(replace(source, os1, ns1), os2, ns2), os3, ns3), os4, ns4), os5, ns5);
	}

	public static String replace(String source, String os1, String ns1, String os2, String ns2, String os3, String ns3, String os4, String ns4, String os5, String ns5, String os6, String ns6) {
		return replace(replace(replace(replace(replace(replace(source, os1, ns1), os2, ns2), os3, ns3), os4, ns4), os5, ns5), os6, ns6);
	}

	public static String replaceNonEscaped(String source, String os, String ns) {
		String replaced = replace(source, "\\" + os, "\uE000"); // Replace escaped characters with intermediate char
		replaced = replace(replaced, os, ns); // Replace normal chars
		return replace(replaced, "\uE000", os); // Replace intermediate with original
	}


	public static String parsePlaceholder(Player player, String source) {
		Matcher matcher = pattern.matcher(source);
		while (matcher.find()) {
			String var1 = matcher.group(1);
			Logger.log(Logger.Level.INFO, "Result " + var1);
			try {
				String var3 = updatePlaceholder(player, Placeholder.valueOf((var1.toUpperCase())));
				Logger.log(Logger.Level.INFO, "Result " + var3);
				source = source.replace(Pattern.quote(var1), Matcher.quoteReplacement(var3));
			} catch (Exception e) {
				Logger.log(Logger.Level.ERROR, "There was an error parsing the placeholder! " + var1);
				e.printStackTrace();
			}
		}
		return source;
	}

	public static Component parsePlaceholder(Player player, String source, Boolean comp) {
		if(!comp) {
			Logger.log(Logger.Level.ERROR, "Please do not use the boolean if you don't want a Component!");
			return null;
		}
		return Component.text().append(parseLegacy(parsePlaceholder(player, source))).build();
	}

	public static String updatePlaceholder(Player player, Placeholder placeholder) {
		if(player == null) {
			switch(placeholder.ordinal()) {
				case 1:
					return Lang.getMessage(Prefix.SYLPHIA);
				case 2:
					return Lang.getMessage(Prefix.ORIGINS);
				case 3:
					return Lang.getMessage(Prefix.ERROR);
				case 4:
				case 5:
				case 6:
				case 7:
					throw new IllegalStateException(placeholder + " requires a player but player was null");
				default:
					throw new IllegalStateException("Unexpected value: " + placeholder);
			}
		} else {
		switch(placeholder.ordinal()) {
			case 1:
				return Lang.getMessage(Prefix.SYLPHIA);
			case 2:
				return Lang.getMessage(Prefix.ORIGINS);
			case 3:
				return Lang.getMessage(Prefix.ERROR);
			case 4:
				return player.getName();
			case 5:
				return player.getDisplayName();
			case 6:
				return plugin.getOriginHandler().getOrigin(player).getName();
			case 7:
				return plugin.getOriginHandler().getOrigin(player).getDisplayName();
			default:
				throw new IllegalStateException("Unexpected value: " + placeholder);
			}
		}
	}

	public static Component parseLegacy(String string) {
		return LegacyComponentSerializer.legacySection().deserialize(string);
	}

	public static Component broadcast(String string, @Optional Player player) {
		return parsePlaceholder(player, string, true);
	}
	public enum Placeholder {
		// Prefixes
		PREFIXSYLPHIA,
		PREFIXORIGINS,
		PREFIXERROR,
		// Player
		PLAYER,
		PLAYERDISPLAYNAME,
		// Origins
		ORIGIN,
		ORIGINDISPLAYNAME,
	}
}
