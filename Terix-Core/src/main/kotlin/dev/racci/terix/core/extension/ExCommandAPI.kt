package dev.racci.terix.core.extension

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.arguments.Argument
import dev.jorel.commandapi.executors.CommandExecutor
import dev.jorel.commandapi.executors.ConsoleCommandExecutor
import dev.jorel.commandapi.executors.PlayerCommandExecutor
import dev.racci.minix.api.annotations.MinixDsl
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

class ArgBuilder {
    val args by lazy { mutableListOf<Argument<*>>() }

    inline fun <reified A : Argument<*>> arg(name: String, vararg data: Any?): A {
        val arg = try {
            A::class.constructors.first { it.parameters.size == (data.size + 1) }.call(name, *data)
        } catch (e: Exception) {
            throw RuntimeException("Could not construct argument of type ${A::class.qualifiedName} with the params ${data.joinToString(", ")}.")
        }
        args += arg
        return arg
    }
}

@MinixDsl
fun CommandAPICommand.arguments(block: ArgBuilder.() -> Unit) {
    val builder = ArgBuilder()
    builder.block()
    arguments.addAll(builder.args)
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
