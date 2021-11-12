package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Subcommand
import com.github.shynixn.mccoroutine.launchAsync
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.sylphia.Sylphia
import me.racci.sylphia.Sylphia.Companion.storageManager
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Bukkit.reload
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

@CommandAlias("plugin|sylphian")
class SylphiaCommand(private val plugin: Sylphia) : BaseCommand() {

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
        plugin.launchAsync {
            val player = sender as Player
            for(player1x in Bukkit.getOnlinePlayers()) {
                storageManager.save(player1x.uniqueId, false)
            }
            player.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(Lang[Command.SAVE_SAVED])).build())
            player.playSound(player.location, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
        }
    }

    @Subcommand("doReload")
    @CommandPermission("plugin.command.doReload")
    @Description("Reloads all parts of Sylphia")
    fun onReload(sender: CommandSender) {
        plugin.launchAsync {reload()}
        sender.sendMessage(Component.text()
            .append(LegacyUtils.parseLegacy(Lang[Command.RELOAD])).build())
    }
}