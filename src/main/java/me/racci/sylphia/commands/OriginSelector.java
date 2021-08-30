package me.racci.sylphia.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import me.racci.sylphia.Sylphia;
import org.bukkit.entity.Player;

@CommandAlias("originmenu|originselector")
public class OriginSelector extends BaseCommand {

	private final Sylphia plugin;

	public OriginSelector(Sylphia plugin) {
		this.plugin = plugin;
	}

	@Default
	@CommandPermission("sylphia.command.main")
	@Description("Opens information for the players current origin.")
	public void onOrigin(Player player) {

	}
}