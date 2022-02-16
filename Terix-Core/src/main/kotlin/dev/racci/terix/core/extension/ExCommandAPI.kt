package dev.racci.terix.core.extension

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.ConsoleCommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

fun command(name: String, block: CommandAPICommand.() -> Unit): CommandAPICommand {
    val command = CommandAPICommand(name)
    command.block()
    command.register()
    return command
}

fun CommandAPICommand.subcommand(name: String, block: CommandAPICommand.() -> Unit): CommandAPICommand {
    val command = CommandAPICommand(name)
    command.block()
    subcommands.add(command)
    return this
}

fun CommandAPICommand.execute(block: (CommandSender, Array<out Any>) -> Unit): CommandAPICommand {
    executes(CommandExecutor(block))
    return this
}

fun CommandAPICommand.executePlayer(block: (Player, Array<out Any>) -> Unit): CommandAPICommand {
    executesPlayer(PlayerCommandExecutor(block))
    return this
}

fun CommandAPICommand.executeConsole(block: (ConsoleCommandSender, Array<out Any>) -> Unit): CommandAPICommand {
    executesConsole(ConsoleCommandExecutor(block))
    return this
}
