package me.racci.sylphia.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.*;
import me.racci.sylphia.Sylphia;
import me.racci.sylphia.data.PlayerData;
import me.racci.sylphia.enums.originsettings.OriginSettings;
import me.racci.sylphia.enums.punishments.Punishments;
import me.racci.sylphia.lang.CommandMessage;
import me.racci.sylphia.lang.Lang;
import me.racci.sylphia.lang.Prefix;
import me.racci.sylphia.origins.OriginHandler;
import me.racci.sylphia.origins.objects.Origin;
import me.racci.sylphia.utils.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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
	public void onOrigin(Player player) {
		player.sendMessage(Lang.getMessage(Prefix.ORIGINS));
		// Open GUI Menu for current origin.
	}

	/*
	.color(TextColor.color(42, 231, 220) // Changes the color and features of everything in the component
	.append(Component.text("This was added in an append statement and with colour!", TextColor.color(42, 231, 220)) // Changes only the single component
	.colorIfAbsent(TextColor.color(108, 12, 123)) // Colours text if it didn't have formatting already
	 */

	@Subcommand("get")
	@CommandPermission("sylphia.command.get")
	@CommandCompletion("@players")
	@Description("Gets the origin of either yourself or the targeted player")
	public void onGet(CommandSender sender, @Optional Player player) {
		if(player == null) {
			player = (  Player)sender;
		}

		Origin origin = plugin.getOriginHandler().getOrigin(player);
		final TextComponent message = Component.text()
				.append(TextUtil.parseLegacy(Lang.getMessage(Prefix.ORIGINS_BOLD))).build()
				.append(TextUtil.parseLegacy(" " + player.getDisplayName()).colorIfAbsent(TextColor.color(230, 230, 230)))
				.append(Component.text(" is the "))
				.append(TextUtil.parseLegacy(origin.getDisplayName()).clickEvent(ClickEvent.openUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).hoverEvent(HoverEvent.showText(Component.text("haha coons"))))
				.append(Component.text(" Origin!"))
				.colorIfAbsent(TextColor.color(42, 231, 220))
				.toBuilder().build();
		sender.sendMessage(message);
	}



	@Subcommand("reset")
	@CommandPermission("sylphia.command.set")
	@CommandCompletion("@players")
	public void onReset(CommandSender sender, Player player) {}


	@Subcommand("set")
	@CommandPermission("sylphia.command.set")
	@CommandCompletion("@players")
	public void onSet(CommandSender sender, Player player, String[] args) {
		if(player != null) {
			OriginHandler originHandler = plugin.getOriginHandler();
			Origin origin = originHandler.getOrigins().get(args[0].toUpperCase());
			String var1 = "{prefixorigins} {playerdisplayname} has become the {origin} Origin!";
			final TextComponent Final = Component.text()
					.append(TextUtil.parseLegacy(Lang.getMessage(Prefix.ORIGINS))).build()
					.append(TextUtil.parseLegacy(player.getDisplayName())).colorIfAbsent(NamedTextColor.GRAY).toBuilder().build()
					.append(Component.text(" has become the "))
					.append(TextUtil.parseLegacy(origin.getDisplayName())).clickEvent(ClickEvent.runCommand("/origin info " + origin.getName()))
					.append(Component.text(" Origin!"))
					.colorIfAbsent(NamedTextColor.AQUA).toBuilder().build();
			player.sendMessage(Final);
			if(originHandler.getOrigins().containsKey(args[0].toUpperCase())) {
				originHandler.setOrigin(player, originHandler.getOrigins().get(args[0].toUpperCase()));
			}
		}
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
			sender.sendMessage("GeneralEnabled: " + origin.isGeneralEffects().toString());
			sender.sendMessage("Night/DayEnabled: " + origin.isTimeEffects().toString());
			sender.sendMessage("Water/LavaEnabled: " + origin.isLiquidEffects().toString());
			sender.sendMessage("Slowfalling: " + origin.isSlowFalling().toString());
			sender.sendMessage("Nightvision: " + origin.isNightVision().toString());
			sender.sendMessage("Jumpboost: " + origin.getJumpBoost().toString());
			sender.sendMessage("Effects: " + origin.getEffects());
			sender.sendMessage("Attributes: " + origin.getAttributes().toString());
			sender.sendMessage("DayEffects: " + origin.getDayEffects());
			sender.sendMessage("DayAttributes: " + origin.getDayAttributes());
			sender.sendMessage("NightEffects: " + origin.getNightEffects());
			sender.sendMessage("NightAttributes: " + origin.getNightAttributes());
			sender.sendMessage("WaterEffects: " + origin.getWaterEffects());
			sender.sendMessage("WaterAttributes: " + origin.getWaterAttributes());
			sender.sendMessage("LavaEffects: " + origin.getLavaEffects());
			sender.sendMessage("LavaAttributes: " + origin.getLavaAttributes());
			sender.sendMessage("DamageEnabled: " + origin.isDamage().toString());
			sender.sendMessage("SunEnabled: " + origin.isSun().toString());
			sender.sendMessage("FallEnabled: " + origin.isFall().toString());
			sender.sendMessage("RainEnabled: " + origin.isRain().toString());
			sender.sendMessage("WaterEnabled: " + origin.isWater().toString());
			sender.sendMessage("FireEnabled: " + origin.isLava().toString());
			sender.sendMessage("SunAmount: " + origin.getSun());
			sender.sendMessage("FallAmount: " + origin.getFall());
			sender.sendMessage("RainAmount: " + origin.getRain());
			sender.sendMessage("WaterAmount: " + origin.getWater());
			sender.sendMessage("LavaAmount: " + origin.getFall());
			sender.sendMessage("Material: " + origin.getItem());
		}
	}
//
//	@Subcommand("debug test")
//	@CommandCompletion("@players")
//	public void onDebugTest(CommandSender sender, String[] args) {
//		OriginHandler originalHandler = plugin.getOriginHandler();
//		if(args[0].equalsIgnoreCase("1")) {
//			originalHandler.refreshTime((Player)sender);
//		} else if(args[0].equalsIgnoreCase("2")) {
//			originalHandler.applyGeneral((Player)sender);
//		} else if(args[0].equalsIgnoreCase("3")) {
//			originalHandler.applyLiquid((Player)sender);
//		} else if(args[0].equalsIgnoreCase("4")) {
//			originalHandler.applyTime((Player) sender);
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
