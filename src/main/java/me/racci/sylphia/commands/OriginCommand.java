package me.racci.sylphia.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.enums.originsettings.OriginSettings;
import me.racci.sylphia.enums.punishments.Punishments;
import me.racci.sylphia.handlers.OriginHandler;
import me.racci.sylphia.lang.CommandMessage;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.objects.Origin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

@CommandAlias("origin|origins|sylphia")
public class OriginCommand extends BaseCommand {
 
	private final Sylphia plugin;
	private final ReloadManager reloadManager;

	public OriginCommand(Sylphia plugin) {
		this.plugin = plugin;
		this.reloadManager = new ReloadManager(plugin);
	}

	@Default
	@CommandPermission("sylphia.command.main")
	@Description("Opens information for the players current origin.")
	public void onSkills(Player player) {
		player.sendMessage(Lang.getMessage(CommandMessage.NO_PROFILE));
	}

	@Subcommand("save")
	@CommandPermission("sylphia.command.save")
	@Description("Saves online players")
	public void onSave(CommandSender sender) {
		new BukkitRunnable() {
			@Override
			public void run() {
				for (Player player : Bukkit.getOnlinePlayers()) {
					plugin.getStorageProvider().save(player, false);
				}
				new BukkitRunnable() {
					@Override
					public void run() {
						sender.sendMessage(Sylphia.getPrefix() + Lang.getMessage(CommandMessage.SAVE_SAVED));
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}

	@Subcommand("reload")
	@CommandPermission("aureliumskills.reload")
	@Description("Reloads the config, messages, menus, loot tables, action bars, boss bars, and health and luck stats.")
	public void reload(CommandSender sender) {
		reloadManager.reload(sender);
	}

	@Subcommand("debug getplayerdata")
	@CommandCompletion("@players")
	public void onDebugPlayer(CommandSender sender, String[] args) {
		if(args.length == 0) {
			PlayerData playerData = plugin.getPlayerManager().getPlayerData((Player)sender);
			if(playerData != null) {
				sender.sendMessage("Username: " + playerData.getPlayer().getName());
				sender.sendMessage("UUID: " + playerData.getPlayer().getUniqueId());
				sender.sendMessage("Origin: " + playerData.getOrigin());
				sender.sendMessage("LastOrigin: " + playerData.getLastOrigin());
				sender.sendMessage("Nightvision: " + playerData.getOriginSetting(OriginSettings.NIGHTVISION));
				sender.sendMessage("Jumpboost: " + playerData.getOriginSetting(OriginSettings.JUMPBOOST));
				sender.sendMessage("Slowfalling: " + playerData.getOriginSetting(OriginSettings.SLOWFALLING));
				sender.sendMessage("Bans: " + playerData.getPunishment(Punishments.BANS));
				sender.sendMessage("Kicks: " + playerData.getPunishment(Punishments.KICKS));
				sender.sendMessage("Mutes: " + playerData.getPunishment(Punishments.MUTES));
				sender.sendMessage("Warns: " + playerData.getPunishment(Punishments.WARNS));
				sender.sendMessage("PurchasedUnban: " + playerData.isUnban());
				sender.sendMessage("Silentmode: " + playerData.isSilentmode());
				sender.sendMessage("PunishmentMap: " + playerData.getPunishmentsMap());
				sender.sendMessage("OriginSettingMap: " + playerData.getOriginSettingMap());
			}
		}
	}

	@Subcommand("debug getorigindata")
	@CommandCompletion("@origins")
	public void onDebugOrigin(CommandSender sender, String[] args) {
		if(args.length == 0) {
			OriginHandler originHandler = plugin.getOriginHandler();
			Origin origin = originHandler.getOrigin((Player)sender);
			sender.sendMessage("Origin: " + origin.getName());
			sender.sendMessage("Displayname: " + origin.getDisplayName());
			sender.sendMessage("BracketName: " + origin.getBracketName());
			sender.sendMessage("Colour: " + origin.getColour());
			sender.sendMessage("DayMessage: " + origin.getDayMessage());
			sender.sendMessage("NightMessage: " + origin.getNightMessage());
			sender.sendMessage("HurtSound: " + origin.getHurtSound().toString());
			sender.sendMessage("DeathSound: " + origin.getDeathSound().toString());
			sender.sendMessage("GeneralEnabled: " + origin.isGeneralEnabled().toString());
			sender.sendMessage("Night/DayEnabled: " + origin.isNightDayEnabled().toString());
			sender.sendMessage("Water/LavaEnabled: " + origin.isWaterLavaEnabled().toString());
			sender.sendMessage("Slowfalling: " + origin.getSlowFalling().toString());
			sender.sendMessage("Nightvision: " + origin.getNightVision().toString());
			sender.sendMessage("Jumpboost: " + origin.getJumpBoost().toString());
			sender.sendMessage("Effects: " + origin.getGeneralEffects());
			sender.sendMessage("Attributes: " + origin.getGeneralAttributes().toString());
			sender.sendMessage("DayEffects: " + origin.getDayEffects());
			sender.sendMessage("DayAttributes: " + origin.getDayAttributes());
			sender.sendMessage("NightEffects: " + origin.getNightEffects());
			sender.sendMessage("NightAttributes: " + origin.getNightAttributes());
			sender.sendMessage("WaterEffects: " + origin.getWaterEffects());
			sender.sendMessage("WaterAttributes: " + origin.getWaterAttributes());
			sender.sendMessage("LavaEffects: " + origin.getLavaEffects());
			sender.sendMessage("LavaAttributes: " + origin.getLavaAttributes());
			sender.sendMessage("DamageEnabled: " + origin.isDamageEnabled().toString());
			sender.sendMessage("SunEnabled: " + origin.isSunDamageEnabled().toString());
			sender.sendMessage("FallEnabled: " + origin.isFallDamageEnabled().toString());
			sender.sendMessage("RainEnabled: " + origin.isRainDamageEnabled().toString());
			sender.sendMessage("WaterEnabled: " + origin.isWaterLavaEnabled().toString());
			sender.sendMessage("FireEnabled: " + origin.isLavaDamageEnabled().toString());
			sender.sendMessage("SunAmount: " + origin.getSunDamage());
			sender.sendMessage("FallAmount: " + origin.getFallDamage());
			sender.sendMessage("RainAmount: " + origin.getRainDamage());
			sender.sendMessage("WaterAmount: " + origin.getWaterDamage());
			sender.sendMessage("LavaAmount: " + origin.getLavaDamage());
			sender.sendMessage("Material: " + origin.getItem());
		}
	}





//	@Subcommand("setall")
//	@CommandCompletion("@players")
//	@CommandPermission("aureliumskills.origin.setlevel")
//	@Description("Sets all of a player's skills to a level.")
//	public void onSkillSetall(CommandSender sender, @Flags("other") Player player, int level) {
//		Locale locale = plugin.getLang().getLocale(sender);
//		if (level > 0) {
//			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
//			if (playerData == null) return;
//			for (Origin origin : plugin.getSkillRegistry().getOrigins()) {
//				if (OptionL.isEnabled(origin)) {
//					int oldLevel = playerData.getOrigin(origin);
//					playerData.setSkillLevel(origin, level);
//					playerData.setSkillXp(origin, 0);
//				}
//			}
//			sender.sendMessage(Sylphia.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETALL_SET, locale)
//					.replace("{level}", String.valueOf(level))
//					.replace("{player}", player.getName()));
//		} else {
//			sender.sendMessage(Sylphia.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_SETALL_AT_LEAST_ONE, locale));
//		}
//	}


//	@Subcommand("origin reset")
//	@CommandCompletion("@players @skills")
//	@CommandPermission("aureliumskills.origin.reset")
//	@Description("Resets all skills or a specific origin to level 1 for a player.")
//	public void onSkillReset(CommandSender sender, @Flags("other") Player player, @Optional Origin origin) {
//		Locale locale = plugin.getLang().getLocale(sender);
//		if (origin != null) {
//			if (OptionL.isEnabled(origin)) {
//				PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
//				if (playerData == null) return;
//				int oldLevel = playerData.getOrigin(origin);
//				playerData.setSkillLevel(origin, 1);
//				playerData.setSkillXp(origin, 0);
//				// Reload items and armor to check for newly met requirements
//				sender.sendMessage(Sylphia.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_RESET_RESET_SKILL, locale)
//						.replace("{origin}", origin.getDisplayName(locale))
//						.replace("{player}", player.getName()));
//			} else {
//				sender.sendMessage(Sylphia.getPrefix(locale) + Lang.getMessage(CommandMessage.UNKNOWN_SKILL, locale));
//			}
//		}
//		else {
//			PlayerData playerData = plugin.getPlayerManager().getPlayerData(player);
//			if (playerData == null) return;
//			for (Origin s : plugin.getSkillRegistry().getOrigins()) {
//				int oldLevel = playerData.getOrigin(s);
//				playerData.setSkillLevel(s, 1);
//				playerData.setSkillXp(s, 0);
//			}
//			sender.sendMessage(Sylphia.getPrefix(locale) + Lang.getMessage(CommandMessage.SKILL_RESET_RESET_ALL, locale)
//					.replace("{player}", player.getName()));
//		}
//	}

	@Subcommand("help")
	@CommandPermission("aureliumskills.help")
	public void onHelp(CommandSender sender, CommandHelp help) {
		help.showHelp();
	}
}
