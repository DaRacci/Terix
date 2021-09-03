package me.racci.sylphia.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.origins.objects.Origin;
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
	public String onPlaceholderRequest(Player player, String params) {
		if(params.equalsIgnoreCase("origin")) {
			return player == null ? null : plugin.getOriginHandler().getOrigin(player).getDisplayName();
		}
		if(params.equalsIgnoreCase("lastorigin")) {
			return player == null ? null : Origin.valueOf(plugin.getPlayerManager().getPlayerData(player.getUniqueId()).getLastOrigin()).getDisplayName();
		}
		return null;
	}
}