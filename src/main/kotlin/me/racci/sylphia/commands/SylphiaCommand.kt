package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.storageManager
import me.racci.sylphia.plugin
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("plugin|sylphian")
class SylphiaCommand : BaseCommand() {

    @Default
    @CommandPermission("plugin.command.main")
    @Description("Base command for Sylphia")
    fun onSylphia(sender: CommandSender) {
        sender.sendMessage("fuck")
    }


    @Subcommand("save")
    @CommandPermission("plugin.command.save")
    @Description("Saves all online players")
    fun onSave(sender: CommandSender) {
        skeduleAsync(plugin) {
            val player = sender as Player
            for(player1x in Bukkit.getOnlinePlayers()) {
                storageManager.save(player1x, false)
            }
            player.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(Lang.Messages.get(Command.SAVE_SAVED))).build())
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
    }

    @Subcommand("doReload")
    @CommandPermission("plugin.command.doReload")
    @Description("Reloads all parts of Sylphia")
    fun onReload(sender: CommandSender) {
        plugin.reload()
        sender.sendMessage(Component.text()
            .append(LegacyUtils.parseLegacy(Lang.Messages.get(Command.RELOAD))).build())
    }
}