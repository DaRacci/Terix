package dev.racci.terix.core.services

import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.utils.clone
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.core.extension.command
import dev.racci.terix.core.extension.execute
import dev.racci.terix.core.extension.getCast
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.subcommand
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.get
import org.koin.core.component.inject

class CommandService(override val plugin: Terix) : Extension<Terix>() {
    private val langService by inject<LangService>()
    private val originService by inject<OriginService>()

    override val name = "Command Service"
    override val dependencies = persistentListOf(LangService::class, OriginService::class)

    override suspend fun handleEnable() {

        command("origin") {
            aliases += "origins"
            permission = CommandPermission.fromString("terix.origin")

            subcommand("get") {
                permission = CommandPermission.fromString("terix.origin.get")
                withArguments(EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))

                execute { sender, args ->
                    getOrigin(sender, args.getCast(0))
                }
            }.clone().apply { arguments.clear() ; subcommands.add(this) } // Super scuffed lmao

            subcommand("set") {
                permission = CommandPermission.fromString("terix.origin.set")
                withArguments(
                    EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
                    StringArgument("origin").replaceSuggestions { originService.registeredOrigins.toTypedArray() }
                )

                execute { sender, args ->
                    setOrigin(sender, args.getCast(0)!!, args.getCast(1)!!)
                }
            }
        }
    }

    private fun getOrigin(
        sender: CommandSender,
        target: Player? = sender as? Player
    ) {
        val origin = (target)?.origin() ?: run {
            langService[
                "lang.error",
                "message" to { "This command must have a target or be send by a Player." }
            ] message sender
            return
        }
        langService[
            "origin.get.${if (target == sender) "self" else "other"}",
            "origin" to { origin.displayName },
            "target" to { target.displayName() },
        ] message sender
    }

    private fun setOrigin(
        sender: CommandSender,
        target: Player,
        originString: String
    ) {
        val origin = get<OriginService>()[originString, true] ?: run {
            langService[
                "lang.error",
                "message" to { "Invalid origin: $originString." },
            ] message sender
            return
        }
        langService[
            "origin.set.${if (target == sender) "self" else "other"}",
            "old_origin" to { target.origin().displayName },
            "new_origin" to { origin.displayName },
            "target" to { target.displayName() },
        ] message sender
        PlayerOriginChangeEvent(target, target.origin(), origin).callEvent()
    }
}
