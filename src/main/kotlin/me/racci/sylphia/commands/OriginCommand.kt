package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import me.racci.raccilib.utils.strings.LegacyUtils
import me.racci.sylphia.Sylphia
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import me.racci.sylphia.origins.Origin
import me.racci.sylphia.origins.OriginManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

@CommandAlias("origin|origins|sylphia")
class OriginCommand(private val plugin: Sylphia) : BaseCommand() {
    private val originManager: OriginManager = plugin.originManager!!

    @Default
    @CommandPermission("sylphia.command.main")
    @Description("Opens information for the players current origin.")
    fun onOrigin(player: Player) {
        player.sendMessage("fuck")
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
    fun onGet(sender: CommandSender, player: Player = sender as Player) {
        val origin: Origin = originManager.getOrigin(player)!!
        val message: TextComponent = Component.text()
            .append(LegacyUtils.parseLegacy(Lang.Message.get(Prefix.ORIGINS))!!).build()
            .append(LegacyUtils.parseLegacy(" " + player.displayName)!!.colorIfAbsent(TextColor.color(230, 230, 230)))
            .append(Component.text(" is the "))
            .append(
                LegacyUtils.parseLegacy(origin.displayName)
                    !!.clickEvent(ClickEvent.openUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")).hoverEvent(
                    HoverEvent.showText(Component.text("haha coons"))
                )
            )
            .append(Component.text(" Origins!"))
            .colorIfAbsent(TextColor.color(42, 231, 220))
            .toBuilder().build()
        sender.sendMessage(message)
    }

    @Subcommand("save")
    @CommandPermission("sylphia.command.save")
    @Description("Saves online players")
    fun onSave(sender: CommandSender) {
        object : BukkitRunnable() {
            override fun run() {
                for (player in Bukkit.getOnlinePlayers()) {
                    plugin.storageProvider?.save(player, false)
                }
                object : BukkitRunnable() {
                    override fun run() {
                        sender.sendMessage(Lang.Message.get(Prefix.SYLPHIA) + Lang.Message.get(Command.SAVE_SAVED))
                    }
                }.runTask(plugin)
            }
        }.runTaskAsynchronously(plugin)
    }

    @Subcommand("help")
    @CommandPermission("aureliumskills.help")
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        help.showHelp()
    }

}