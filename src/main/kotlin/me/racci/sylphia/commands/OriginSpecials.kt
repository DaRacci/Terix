@file:Suppress("unused")
package me.racci.sylphia.commands

import co.aikar.commands.BaseCommand
import co.aikar.commands.annotation.*
import me.racci.raccilib.skedule.SynchronizationContext
import me.racci.raccilib.skedule.skeduleAsync
import me.racci.raccilib.utils.strings.colour
import me.racci.raccilib.utils.strings.replace
import me.racci.raccilib.utils.worlds.WorldTime
import me.racci.sylphia.Sylphia
import me.racci.sylphia.enums.Special
import me.racci.sylphia.lang.Command
import me.racci.sylphia.lang.Lang
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.sound
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

class SpecialCommands(internal val plugin: Sylphia) {

    private val originManager = plugin.originManager!!
    private val playerManager = plugin.playerManager!!


    @CommandAlias("nightvision")
    inner class NightVisionCommand(private val plugin: Sylphia) : BaseCommand() {

        @Default
        @CommandPermission("sylphia.commands.nightvision")
        @Description("Toggles night vision")
        fun onToggle(player: Player) {
            if(originManager.getOrigin(player)!!.nightVision) {
                val playerData = playerManager.getPlayerData(player.uniqueId)!!
                when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                    0 -> {
                        player.addPotionEffect(PotionEffect(PotionEffectType.NIGHT_VISION, Int.MAX_VALUE, 0, true, false, false))
                        playerData.setOriginSetting(Special.NIGHTVISION, 2)
                        player.playSound(sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 0.5f, 1f))
                    }
                    1, 2 -> {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION)
                        playerData.setOriginSetting(Special.NIGHTVISION, 0)
                        player.playSound(sound(Key.key("entity.experience_orb.pickup"), Sound.Source.PLAYER, 0.5f, 1f)) // TODO Add different sound for disable
                    }
                    else -> TODO("Make it give a correct value and report it to the player and console")
                }
            } else {
                player.sendMessage(replace(Lang.Message.get(Command.TOGGLE_NO_PERMISSION), "{var}", colour("&2Night Vision")!!))
                player.playSound(sound(Key.key("block.note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
            }
        }

        @Subcommand("on")
        fun onToggleOn(player: Player) {
            skeduleAsync(plugin) {
                if(originManager.getOrigin(player)!!.nightVision) {
                    val playerData = playerManager.getPlayerData(player.uniqueId)!!
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        2 -> {
                            player.sendMessage(Lang.Message.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block_note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 2)
                            player.sendMessage(replace(Lang.Message.get(Command.TOGGLE_SPECIFIC),
                                "{var1}", colour("&2Night Vision")!!,
                                "{var2}", colour("&aon")!!))
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
                if(originManager.getOrigin(player)!!.nightVision) {
                    val playerData = playerManager.getPlayerData(player.uniqueId)!!
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        0 -> {
                            player.sendMessage(Lang.Message.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block_note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 0)
                            player.sendMessage(replace(Lang.Message.get(Command.TOGGLE_SPECIFIC),
                                "{var1}", colour("&2Night Vision")!!,
                                "{var2}", colour("&coff")!!))
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
                if(originManager.getOrigin(player)!!.nightVision) {
                    val playerData = playerManager.getPlayerData(player.uniqueId)!!
                    when(playerData.getOriginSetting(Special.NIGHTVISION)) {
                        1 -> {
                            player.sendMessage(Lang.Message.get(Command.TOGGLE_CURRENT))
                            player.playSound(sound(Key.key("block_note_block.bass"), Sound.Source.PLAYER, 1f, 1f))
                        }
                        else -> {
                            playerData.setOriginSetting(Special.NIGHTVISION, 1)
                            player.sendMessage(replace(Lang.Message.get(Command.TOGGLE_SPECIFIC),
                                "{var1}", colour("&2Night Vision")!!,
                                "{var2}", colour("&5night")!!))
                            player.playSound(sound(Key.key("entity.bat_takeoff"), Sound.Source.PLAYER, 0.5f, 1f))
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