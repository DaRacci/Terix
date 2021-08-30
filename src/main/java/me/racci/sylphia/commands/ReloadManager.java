package me.racci.sylphia.commands;

import me.racci.sylphia.Sylphia;
import me.racci.sylphia.lang.CommandMessage;
import me.racci.sylphia.lang.Lang;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ReloadManager {

    private final Sylphia plugin;
    private final Lang lang;

    public ReloadManager(Sylphia plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLang();
    }

    public void reload(CommandSender sender) {
        // Load config
        plugin.reloadConfig();
        plugin.saveDefaultConfig();
        plugin.getOptionLoader().loadOptions();
        // Load language files
        lang.loadLangFile();
        lang.loadEmbeddedMessages(plugin.getCommandManager());
        lang.loadLanguages(plugin.getCommandManager());
        sender.sendMessage(Sylphia.getPrefix() + ChatColor.GREEN + Lang.getMessage(CommandMessage.RELOAD));
    }

}