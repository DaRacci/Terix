@file:Suppress("unused")
package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.racci.raccilib.skedule.skeduleAsync
import me.racci.raccilib.utils.strings.LegacyUtils
import me.racci.sylphia.Sylphia
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("sylphia|sylphian")
class SylphiaCommand(private val plugin: Sylphia) : BaseCommand() {

    private val storageManager = plugin.storageProvider
    private val playerManager = plugin.playerManager

    @Default
    @CommandPermission("sylphia.command.main")
    @Description("Base command for Sylphia")
    fun onSylphia(sender: CommandSender) {
        sender.sendMessage("fuck")
    }


    @Subcommand("save")
    @CommandPermission("sylphia.command.save")
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

    @Subcommand("reload")
    @CommandPermission("sylphia.command.reload")
    @Description("Reloads all parts of Sylphia")
    fun onReload(sender: CommandSender) {
        plugin.handleReload()
        sender.sendMessage(Component.text()
            .append(LegacyUtils.parseLegacy(Lang.Messages.get(Command.RELOAD))).build())
    }
}