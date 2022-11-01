package dev.racci.terix.core.commands.arguments

import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import cloud.commandframework.minecraft.extras.RichDescription
import dev.racci.terix.core.origins.OriginInfo
import org.bukkit.command.CommandSender
import java.util.Queue
import java.util.function.BiFunction

public class OriginInfoArgument(
    name: String,
    required: Boolean,
    description: RichDescription,
    suggestionsProvider: BiFunction<CommandContext<CommandSender>, String, List<String>>?
) : CommandArgument<CommandSender, OriginInfo.Info>(
    required,
    name,
    OriginInfoTypeParser(),
    "Human",
    OriginInfo.Info::class.java,
    suggestionsProvider,
    description
) {

    public class OriginInfoTypeParser : ArgumentParser<CommandSender, OriginInfo.Info> {
        override fun parse(
            context: CommandContext<CommandSender>,
            inputQueue: Queue<String>
        ): ArgumentParseResult<OriginInfo.Info> {
            val input = inputQueue.peek() ?: return ArgumentParseResult.failure(NoInputProvidedException(OriginInfoTypeParser::class.java, context))
            val typeInfo = OriginInfo.Types[input, false]

            inputQueue.remove()
            return ArgumentParseResult.success(typeInfo)
        }

        override fun suggestions(
            commandContext: CommandContext<CommandSender>,
            input: String
        ): MutableList<String> = OriginInfo.Types.propertyMap.keys.toMutableList()
    }
}
