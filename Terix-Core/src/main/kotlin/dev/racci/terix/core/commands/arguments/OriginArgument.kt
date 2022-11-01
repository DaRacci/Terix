package dev.racci.terix.core.commands.arguments

import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.parser.ArgumentParser
import cloud.commandframework.captions.Caption
import cloud.commandframework.captions.CaptionVariable
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.parsing.NoInputProvidedException
import cloud.commandframework.exceptions.parsing.ParserException
import cloud.commandframework.minecraft.extras.RichDescription
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.services.OriginServiceImpl
import org.bukkit.command.CommandSender
import java.util.Queue
import java.util.function.BiFunction

public class OriginArgument(
    name: String,
    required: Boolean,
    description: RichDescription,
    suggestionsProvider: BiFunction<CommandContext<CommandSender>, String, List<String>>?
) : CommandArgument<CommandSender, Origin>(
    required,
    name,
    OriginParser(),
    "Human",
    Origin::class.java,
    suggestionsProvider,
    description
) {

    public class OriginParser : ArgumentParser<CommandSender, Origin> {
        override fun parse(
            context: CommandContext<CommandSender>,
            inputQueue: Queue<String>
        ): ArgumentParseResult<Origin> {
            val input = inputQueue.peek() ?: return ArgumentParseResult.failure(NoInputProvidedException(OriginParser::class.java, context))
            val origin = OriginService.getOriginOrNull(input) ?: return ArgumentParseResult.failure(OriginParseException(input, context))

            inputQueue.remove()
            return ArgumentParseResult.success(origin)
        }

        override fun suggestions(
            commandContext: CommandContext<CommandSender>,
            input: String
        ): MutableList<String> = OriginServiceImpl.getService().registeredOrigins.toMutableList()

        public class OriginParseException(
            input: String,
            context: CommandContext<CommandSender>
        ) : ParserException(
            OriginParser::class.java,
            context,
            Caption.of("argument.parse.failure.origin"),
            CaptionVariable.of("input", input)
        )
    }
}
