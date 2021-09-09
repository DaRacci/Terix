package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.CommandHelp
import co.aikar.commands.annotation.*
import me.racci.raccilib.utils.strings.LegacyUtils
import me.racci.sylphia.Sylphia
import me.racci.sylphia.data.PlayerData
import me.racci.sylphia.enums.Special
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import me.racci.sylphia.origins.Origin
import me.racci.sylphia.origins.OriginManager
import me.racci.sylphia.origins.OriginValue
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
class OriginCommand(plugin: Sylphia) : BaseCommand() {
    private val plugin: Sylphia
    private val reloadManager: ReloadManager
    private val originManager: OriginManager
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

    @Subcommand("reload")
    @CommandPermission("aureliumskills.reload")
    @Description("Reloads the config, messages, menus, loot tables, action bars, boss bars, and health and luck stats.")
    fun reload(sender: CommandSender?) {
        reloadManager.reload(sender!!)
    }

    @Subcommand("debug getplayerdata")
    @CommandCompletion("@players")
    fun onDebugPlayer(sender: CommandSender, args: Array<String?>) {
        if (args.isEmpty()) {
            val playerData: PlayerData = plugin.playerManager?.getPlayerData(sender as Player)!!
            sender.sendMessage("Username: " + playerData.player.name)
            sender.sendMessage("UUID: " + playerData.player.uniqueId)
            sender.sendMessage("Origins: " + playerData.origin)
            sender.sendMessage("LastOrigin: " + playerData.lastOrigin)
            sender.sendMessage("Nightvision: " + playerData.getOriginSetting(Special.NIGHTVISION))
            sender.sendMessage("Jumpboost: " + playerData.getOriginSetting(Special.JUMPBOOST))
            sender.sendMessage("Slowfalling: " + playerData.getOriginSetting(Special.SLOWFALLING))
        }
    }

    @Subcommand("debug getorigindata")
    @CommandCompletion("nameMap|soundMap|timeMap|permissionMap|effectEnableMap|specialMap|normalEffectMaps|conditionEffectMap|conditionAttributeMap|damageEnableMap|DamageAmountMap|Misc")
    fun onDebugOrigin(sender: CommandSender, args: Array<String>) {
        val origin: Origin = originManager.getOrigin(sender as Player)!!
        if (args[0].equals("nameMap", ignoreCase = true)) {
            sender.sendMessage("Name: " + origin.name)
            sender.sendMessage("Colour: " + origin.colour + "This colour")
            sender.sendMessage("Displayname: " + origin.displayName)
            sender.sendMessage("BracketName: " + origin.bracketName)
        } else if (args[0].equals("soundMap", ignoreCase = true)) {
            sender.sendMessage("HurtSound: " + origin.hurtSound.toString())
            sender.sendMessage("DeathSound: " + origin.deathSound.toString())
        } else if (args[0].equals("timeMap", ignoreCase = true)) {
            sender.sendMessage("DayTitle: " + origin.dayTitle)
            sender.sendMessage("DaySubtitle: " + origin.daySubtitle)
            sender.sendMessage("NightTitle: " + origin.nightTitle)
            sender.sendMessage("NightSubtitle: " + origin.nightSubtitle)
        } else if (args[0].equals("permissionMap", ignoreCase = true)) {
            sender.sendMessage("Required: " + origin.requiredPermissions)
            sender.sendMessage("Give: " + origin.givenPermissions)
        } else if (args[0].equals("effectEnableMap", ignoreCase = true)) {
            sender.sendMessage("Enabled: " + origin.enableEffects)
            sender.sendMessage("Time: " + origin.enableTime)
            sender.sendMessage("Liquid: " + origin.enableLiquid)
            sender.sendMessage("Dimension" + origin.enableDimension)
        } else if (args[0].equals("SpecialMap", ignoreCase = true)) {
            sender.sendMessage("Slowfalling: " + origin.slowFalling)
            sender.sendMessage("Nightvision: " + origin.nightVision)
            sender.sendMessage("Jumpboost: " + origin.jumpBoost)
        } else if (args[0].equals("normalEffectMaps", ignoreCase = true)) {
            sender.sendMessage("Effects: " + origin.effectMap)
            sender.sendMessage("Attributes: " + origin.attributeMap)
        } else if (args[0].equals("conditionEffectMap", ignoreCase = true)) {
            sender.sendMessage("OD: " + origin.conditionAttributeMap[OriginValue.OD])
            sender.sendMessage("OD: " + origin.conditionEffectMap[OriginValue.OD])
            sender.sendMessage("ON: " + origin.conditionAttributeMap[OriginValue.ON])
            sender.sendMessage("ON: " + origin.conditionEffectMap[OriginValue.ON])
            sender.sendMessage("ODW: " + origin.conditionAttributeMap[OriginValue.ODW])
            sender.sendMessage("ODW: " + origin.conditionEffectMap[OriginValue.ODW])
            sender.sendMessage("ODL: " + origin.conditionAttributeMap[OriginValue.ODL])
            sender.sendMessage("ODL: " + origin.conditionEffectMap[OriginValue.ODL])
            sender.sendMessage("ONW: " + origin.conditionAttributeMap[OriginValue.ONW])
            sender.sendMessage("ONW: " + origin.conditionEffectMap[OriginValue.ONW])
            sender.sendMessage("ONL: " + origin.conditionAttributeMap[OriginValue.ONL])
            sender.sendMessage("ONL: " + origin.conditionEffectMap[OriginValue.ONL])
            sender.sendMessage("N: " + origin.conditionAttributeMap[OriginValue.N])
            sender.sendMessage("N: " + origin.conditionEffectMap[OriginValue.N])
            sender.sendMessage("NL: " + origin.conditionAttributeMap[OriginValue.NL])
            sender.sendMessage("NL: " + origin.conditionEffectMap[OriginValue.NL])
            sender.sendMessage("E: " + origin.conditionAttributeMap[OriginValue.E])
            sender.sendMessage("E: " + origin.conditionEffectMap[OriginValue.E])
            sender.sendMessage("EW: " + origin.conditionAttributeMap[OriginValue.EW])
            sender.sendMessage("EW: " + origin.conditionEffectMap[OriginValue.EW])
            sender.sendMessage("EL: " + origin.conditionAttributeMap[OriginValue.EL])
            sender.sendMessage("EL: " + origin.conditionEffectMap[OriginValue.EL])
        } else if (args[0].equals("damageEnableMap", ignoreCase = true)) {
            sender.sendMessage("DamageEnabled: " + origin.enableDamage)
            sender.sendMessage("SunEnabled: " + origin.enableSun)
            sender.sendMessage("FallEnabled: " + origin.enableFall)
            sender.sendMessage("RainEnabled: " + origin.enableRain)
            sender.sendMessage("WaterEnabled: " + origin.enableWater)
            sender.sendMessage("FireEnabled: " + origin.enableLava)
        } else if (args[0].equals("damageAmountMap", ignoreCase = true)) {
            sender.sendMessage("SunAmount: " + origin.sunAmount)
            sender.sendMessage("FallAmount: " + origin.fallAmount)
            sender.sendMessage("RainAmount: " + origin.rainAmount)
            sender.sendMessage("WaterAmount: " + origin.waterAmount)
            sender.sendMessage("LavaAmount: " + origin.lavaAmount)
        } else if (args[0].equals("Misc", ignoreCase = true)) {
            sender.sendMessage("Material: " + origin.item)
        }
    }

    @Subcommand("help")
    @CommandPermission("aureliumskills.help")
    fun onHelp(sender: CommandSender?, help: CommandHelp) {
        help.showHelp()
    }

    init {
        this.plugin = plugin
        originManager = plugin.originManager!!
        reloadManager = ReloadManager(plugin)
    }
}