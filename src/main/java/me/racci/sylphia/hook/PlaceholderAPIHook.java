package me.racci.sylphia.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import org.bukkit.entity.Player;

@SuppressWarnings("all")
public class PlaceholderAPIHook extends PlaceholderExpansion {

	private final Sylphia plugin;

	public PlaceholderAPIHook(Sylphia plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public boolean canRegister() {
		return true;
	}

	@Override
	public String getIdentifier() {
		return "sylphia";
	}

	@Override
	public String getAuthor() {
		return "Racci";
	}

	@Override
	public String getVersion() {
		return "Alpha-0.1";
	}

	@Override
	public String onPlaceholderRequest(Player player, String identifier) {
		if (player == null) {
			return "";
		}

		//Gets origin
		if (identifier.equals("origin")) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData != null) {
				return String.valueOf(playerData.getOrigin());
			}
		}

		//Gets last origin
		if (identifier.equals("lastorigin")) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
			if (playerData != null) {
				return String.valueOf(playerData.getLastOrigin());
			}
		}
		return identifier;
	}
}