package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.CommandAlias
import co.aikar.commands.annotation.CommandCompletion
import co.aikar.commands.annotation.CommandPermission
import co.aikar.commands.annotation.Conditions
import co.aikar.commands.annotation.Default
import co.aikar.commands.annotation.Description
import co.aikar.commands.annotation.Flags
import co.aikar.commands.annotation.Optional
import co.aikar.commands.annotation.Subcommand
import co.aikar.commands.annotation.Syntax
import com.github.shynixn.mccoroutine.launchAsync
import me.racci.raccicore.utils.strings.LegacyUtils
import me.racci.raccicore.utils.strings.colour
import me.racci.raccicore.utils.strings.replace
import me.racci.sylphia.Sylphia
import me.racci.sylphia.events.OriginChangeEvent
import me.racci.sylphia.events.OriginResetEvent
import me.racci.sylphia.factories.Origin
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Origins
import me.racci.sylphia.lang.Prefix
import me.racci.sylphia.managers.Item
import me.racci.sylphia.managers.ItemManager
import me.racci.sylphia.origins.OriginManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player


@CommandAlias("origin|origins")
class OriginCommand(private val plugin: Sylphia) : BaseCommand() {

    @Default
    @CommandPermission("plugin.command.main")
    @Description("Opens information for the players current origin.")
    fun onOrigin(sender: CommandSender) {
        sender.sendMessage("${(sender as Player).isInRain} : ${sender.isInDaylight}")
    }

    /*
    .colorIfAbsent(TextColor.color(42, 231, 220))
	.color(TextColor.color(42, 231, 220) // Changes the color and features of everything in the component
	.append(Component.text("This was added in an append statement and with colour!", TextColor.color(42, 231, 220)) // Changes only the single component
	.colorIfAbsent(TextColor.color(108, 12, 123)) // Colours text if it didn't have formatting already
	 */

    @Subcommand("get")
    @CommandPermission("plugin.command.get")
    @CommandCompletion("@players")
    @Description("Gets the origin of either yourself or the targeted player")
    fun onGet(sender: CommandSender, player: Player = sender as Player) {
        val origin = OriginManager.getOrigin(player.uniqueId)
        if(origin != null) {
            sender.sendMessage(Component.text()
                .append(
                    LegacyUtils.parseLegacy(replace(Lang[Origins.COMMAND_GET_HAS],
                    "{PlayerDisplayName}", player.displayName,
                    "{origin}", origin.identity.displayName))).build())
        } else {
            sender.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(replace(Lang[Origins.COMMAND_GET_NULL],
                    "{PlayerDisplayName}", player.displayName))).build())
        }
    }

    @Subcommand("set")
    @CommandPermission("plugin.command.set")
    @CommandCompletion("@players @origins")
    @Description("Sets the targeted players origin")
    fun onSet(sender: CommandSender, @Flags("other") player: Player, origin: Origin) {
        var commandSender: Player? = null
        if(sender is Player) commandSender = sender
        plugin.launchAsync {
            Bukkit.getPluginManager().callEvent(
                OriginChangeEvent(
                    player,
                    OriginManager.getOrigin(player.uniqueId),
                    origin,
                    commandSender
                )
            )
        }
    }

    @Subcommand("reset")
    @CommandPermission("plugin.command.reset")
    @CommandCompletion("@players")
    @Description("Sets the targeted players origin to Lost/Nothing")
    fun onReset(sender: CommandSender, @Optional player: Player = sender as Player) {
        plugin.launchAsync {
            Bukkit.getPluginManager().callEvent(
                OriginResetEvent(player, sender as Player)
            )
        }
    }


    @Subcommand("doReload")
    @CommandPermission("plugin.command.doReload")
    @Description("Reloads only the origins")
    fun onReload(sender: CommandSender) {
        plugin.launchAsync {
            OriginManager.reload()
            Bukkit.getOnlinePlayers().forEach {player1x->
                if(OriginManager.getOrigin(player1x.uniqueId) == null) return@forEach
                OriginManager.removeAll(player1x)
            }
            sender.sendMessage(colour("${Lang[Prefix.ORIGINS]} &bReloaded Origins!"))
        }
    }

    @Subcommand("token")
    @CommandPermission("plugin.command.token")
    @Syntax("[amount] [player]")
    @CommandCompletion("@range:1-64")
    @Description("Gives either yourself or the targeted player an Origin token")
    fun onToken(sender: CommandSender, @Optional @Conditions("limits:min=1,max=64") amount: Int = 1, @Optional player: Player = sender as Player) {
        player.inventory.addItem(ItemManager[Item.ORIGIN_TOKEN]?.asQuantity(amount) ?: return)
        player.playSound(player.location, Sound.BLOCK_BEEHIVE_EXIT, 1f, 1f)
        if(player != sender) {
            sender.sendMessage(Component.text()
                .append(LegacyUtils.parseLegacy(replace(Lang[Origins.COMMAND_TOKEN_SENDER],
                    "{PlayerDisplayName}", player.displayName,
                    "{amount}", amount.toString()))).build())
        }
        sender.sendMessage(Component.text()
            .append(LegacyUtils.parseLegacy(replace(Lang[Origins.COMMAND_TOKEN_SENDER],
                "{PlayerDisplayName}", player.displayName,
                "{amount}", amount.toString()))).build())
    }


    // Broadcast
//    Component.text()
    //    .append(LegacyUtils.parseLegacy(replace(Lang.Message.get(Origins.SELECT_BROADCAST),
    //    "{PlayerDisplayName}", player.displayName,
    //    "{var}", origin.displayName))!!).build())



    @Subcommand("help")
    @CommandPermission("plugin.command.help")
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        help.showHelp()
    }

}