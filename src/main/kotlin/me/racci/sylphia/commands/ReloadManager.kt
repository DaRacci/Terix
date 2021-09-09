@file:Suppress("unused")
@file:JvmName("ReloadManager")
package me.racci.sylphia.commands

import me.racci.raccilib.utils.strings.colour
import me.racci.sylphia.Sylphia
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import org.bukkit.command.CommandSender

class ReloadManager(private val plugin: Sylphia) {
    fun reload(sender: CommandSender) {
        // Load config
        plugin.reloadConfig()
        plugin.saveDefaultConfig()
        plugin.optionLoader?.loadOptions()
        // Load language files
        plugin.lang?.loadLang(plugin.commandManager!!)
        sender.sendMessage(colour("${Lang.Messages.get(Prefix.SYLPHIA)} &8${Lang.Message.get(Command.RELOAD)}",false)!!)
    }
}