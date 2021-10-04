package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.racci.raccicore.skedule.SynchronizationContext
import me.racci.raccicore.skedule.skeduleAsync
import me.racci.raccicore.utils.strings.colour
import me.racci.raccicore.utils.strings.replace
import me.racci.raccicore.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.enums.Special
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import me.racci.sylphia.lang.Prefix
import me.racci.sylphia.originManager
import me.racci.sylphia.playerManager
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class SpecialCommands : BaseCommand() {

    @CommandAlias("nightvision")
    inner class NightVisionCommand(private val plugin: Sylphia) {

        @Default
        @CommandPermission("plugin.commands.nightvision")
        @Description("Toggles night vision")
        fun onToggle(player: Player) {
            println(originManager.getOrigin(player.uniqueId)?.special?.nightVision)
            if(originManager.getOrigin(player)?.special?.nightVision == true) {
                val playerData = playerManager.getPlayerData(player.uniqueId) ?: return
                when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                    0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                        playerData.setOriginSetting(Special.NIGHTVISION, 2)
                        player.playSound(sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 0.5f, 1f))
                    }
                    1, 2 -> {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        playerData.setOriginSetting(Special.NIGHTVISION, 0)
                        player.playSound(sound(Key.key("item.shield.block"), Sound.Source.PLAYER, 0.5f, 1f))
                    }
                    else -> {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        playerData.setOriginSetting(Special.NIGHTVISION, 0)
                        player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 0.5f, 1f))
                        player.sendMessage("${Lang.Messages.get(Prefix.ERROR)} &cThere was an error with the saved value for your file, this has been corrected automatically and has your night vision to off!")
                    }

                }
            } else {
                player.sendMessage(replace(Lang.Messages.get(Command.TOGGLE_NO_PERMISSION), "{special}",
                    colour("&2Night Vision")
                ))
                player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
            }
        }

        @Subcommand("on")
        fun onToggleOn(player: Player) {
            skeduleAsync(plugin) {
                if(originManager.getOrigin(player)?.special?.nightVision == true) {
                    val playerData = playerManager.getPlayerData(player.uniqueId) ?: return@skeduleAsync
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        2 -> {
                            player.sendMessage(Lang.Messages.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 2)
                            player.sendMessage(replace(Lang.Messages.get(Command.TOGGLE_SPECIFIC),
                                "{special}", colour("&2Night Vision"),
                                "{option}", colour("&aon")
                            ))
                            player.playSound(sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 0.5f, 1f))
                            switchContext(SynchronizationContext.SYNC)
                            player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                        }
                    }
                }
            }
        }

        @Subcommand("off")
        fun onToggleOff(player: Player) {
            skeduleAsync(plugin) {
                if(originManager.getOrigin(player)?.special?.nightVision == true) {
                    val playerData = playerManager.getPlayerData(player.uniqueId) ?: return@skeduleAsync
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        0 -> {
                            player.sendMessage(Lang.Messages.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 0)
                            player.sendMessage(replace(Lang.Messages.get(Command.TOGGLE_SPECIFIC),
                                "{special}", colour("&2Night Vision"),
                                "{option}", colour("&coff")
                            ))
                            player.playSound(sound(Key.key("item.shield.block"), Sound.Source.PLAYER, 0.5f, 1f))
                            switchContext(SynchronizationContext.SYNC)
                            player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        }
                    }
                }
            }
        }

        @Subcommand("night")
        fun onToggleNight(player: Player) {
            skeduleAsync(plugin) {
                if(originManager.getOrigin(player)?.special?.nightVision == true) {
                    val playerData = playerManager.getPlayerData(player.uniqueId) ?: return@skeduleAsync
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        1 -> {
                            player.sendMessage(Lang.Messages.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 1)
                            player.sendMessage(replace(Lang.Messages.get(Command.TOGGLE_SPECIFIC),
                                "{special}", colour("&2Night Vision"),
                                "{option}", colour("&5night")
                            ))
                            player.playSound(sound(Key.key("entity.bat.takeoff"), Sound.Source.PLAYER, 0.5f, 1f))
                            if(WorldTime.isNight(player)) {
                                switchContext(SynchronizationContext.SYNC)
                                player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                            } else {
                                switchContext(SynchronizationContext.SYNC)
                                player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                            }
                        }
                    }
                }
            }
        }
    }
}