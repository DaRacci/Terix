package dev.racci.terix.core.services

import cloud.commandframework.arguments.CommandArgument
import cloud.commandframework.arguments.flags.CommandFlag
import cloud.commandframework.arguments.parser.ArgumentParseResult
import cloud.commandframework.arguments.standard.IntegerArgument
import cloud.commandframework.bukkit.parsers.PlayerArgument
import cloud.commandframework.context.CommandContext
import cloud.commandframework.exceptions.InvalidCommandSenderException
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.kotlin.coroutines.extension.suspendingHandler
import cloud.commandframework.kotlin.extension.buildAndRegister
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.minecraft.extras.RichDescription
import cloud.commandframework.paper.PaperCommandManager
import cloud.commandframework.permission.Permission
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.commands.arguments.OriginArgument
import dev.racci.terix.core.commands.arguments.OriginInfoArgument
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.freeChanges
import dev.racci.terix.core.origins.OriginInfo
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.get
import org.koin.core.component.inject
import kotlin.properties.Delegates

@MappedExtension(Terix::class, "Command Service", [OriginService::class, GUIService::class])
class CommandService(override val plugin: Terix) : Extension<Terix>() {
    private val lang: Lang by inject<DataService>().inject()
    private var registered = false

    private var manager by Delegates.notNull<PaperCommandManager<CommandSender>>()
    private var playerFlag by Delegates.notNull<CommandFlag.Builder<Player>>()
    private var originArgument by Delegates.notNull<CommandArgument.Builder<CommandSender, Origin>>()

    override suspend fun handleEnable() {
        logger.debug { "Registering commands" }
        if (!registered) {
            logger.debug { "Creating Manager" }
            this.loadManager()
            registered = true
        }

        manager.buildAndRegister(
            "origin",
            RichDescription.of(MiniMessage.miniMessage().deserialize("Parent origin commands.")),
            arrayOf("origins", "o")
        ) {
            this.registerCopy(
                "get",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Get the origin of a player."))
            ) {
                mutate { it.flag(playerFlag) }
                permission(Permission.of("terix.command.origin.get"))
                suspendingHandler(supervisor, dispatcher.get()) { context -> getOrigin(context.sender, getTargetOrThrow(context)) }
            }

            this.registerCopy(
                "set",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Set the origin of a player."))
            ) {
                mutate { it.flag(playerFlag) }
                argument(
                    OriginArgument(
                        "origin",
                        true,
                        RichDescription.of(MiniMessage.miniMessage().deserialize("The origin to set the player to.")),
                        null
                    )
                )
                permission(Permission.of("terix.command.origin.set"))
                suspendingHandler(supervisor, dispatcher.get()) { context -> setOrigin(context.sender, getTargetOrThrow(context), context.get("origin")) }
            }

            this.registerCopy(
                "menu",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Open the origin menu."))
            ) {
                permission(Permission.of("terix.command.origin.menu"))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, plugin.minecraftDispatcher) { context -> get<GUIService>().baseGui.value.show(getTargetOrThrow(context)) }
            }

            this.registerCopy(
                "choice",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Parent choice commands."))
            ) {
                permission(Permission.of("terix.command.origin.choice"))

                this.registerCopy(
                    "get",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Gets the remaining choices of a player."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(Permission.of("terix.command.origin.choice.get"))
                    suspendingHandler(supervisor, dispatcher.get()) { context -> getChoices(context.sender, getTargetOrThrow(context)) }
                }

                this.registerCopy(
                    "add",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Adds a choice to a player."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(Permission.of("terix.command.origin.choice.add"))
                    argument(IntegerArgument.of("amount"))
                    suspendingHandler(supervisor, dispatcher.get()) { context -> addChoices(context.sender, getTargetOrThrow(context), context.get("amount")) }
                }

                this.registerCopy(
                    "remove",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Removes a choice from a player."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(Permission.of("terix.command.origin.choice.remove"))
                    argument(IntegerArgument.of("amount"))
                    suspendingHandler(supervisor, dispatcher.get()) { context -> removeChoices(context.sender, getTargetOrThrow(context), context.get("amount")) }
                }
            }

            this.registerCopy(
                "grant",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Grants an origin to a player."))
            ) {
                mutate { it.flag(playerFlag) }
                permission(Permission.of("terix.command.origin.grant"))
                argument(
                    OriginArgument(
                        "origin",
                        true,
                        RichDescription.of(MiniMessage.miniMessage().deserialize("The origin to grant the player.")),
                        null
                    )
                )
                suspendingHandler(supervisor, dispatcher.get()) { context -> grantOrigin(context.sender, getTargetOrThrow(context), context.get("origin")) }
            }

            this.registerCopy(
                "describe",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Describes an origin."))
            ) {
                argument(
                    OriginArgument(
                        "origin",
                        true,
                        RichDescription.of(MiniMessage.miniMessage().deserialize("The origin to describe.")),
                        null
                    )
                )
                argument(
                    OriginInfoArgument(
                        "info",
                        true,
                        RichDescription.of(MiniMessage.miniMessage().deserialize("The info to describe.")),
                        null
                    )
                )
                suspendingHandler(supervisor, dispatcher.get()) { context -> describeOrigin(context.sender, context.get("origin"), context.get("info")) }
            }
        }

        manager.buildAndRegister(
            "terix",
            RichDescription.of(MiniMessage.miniMessage().deserialize("Parent terix commands.")),
            emptyArray()
        ) {
            this.registerCopy(
                "reload",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Reload the plugin."))
            ) {
                permission(Permission.of("terix.command.reload"))
                suspendingHandler(supervisor, dispatcher.get()) { context ->
                    val start = now()
                    lang.generic.reloadLang["time" to { (start - now()).inWholeMilliseconds }] message context.sender
                }
            }
        }
    }

    override suspend fun handleUnload() {
        manager.deleteRootCommand("origin")
    }

    private fun loadManager() {
        val coordinator = AsynchronousCommandExecutionCoordinator
            .newBuilder<CommandSender>()
            .withExecutor(dispatcher.get().executor)
            .withAsynchronousParsing()
            .build()

        manager = PaperCommandManager.createNative(plugin, coordinator)

        MinecraftExceptionHandler<CommandSender>()
            .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SYNTAX) { _, e ->
                val exception = e.castOrThrow<InvalidCommandSenderException>()
                MiniMessage.miniMessage().deserialize("Invalid syntax: ${exception.currentChain.getOrNull(0)?.name} - ${exception.command?.arguments?.joinToString(", ")}")
            }
            .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION) { _, e ->
                logger.error(e) { "Error while executing command" }
                MiniMessage.miniMessage().deserialize("An error occurred while executing this command: ${e.message}")
            }
            .withHandler(MinecraftExceptionHandler.ExceptionType.NO_PERMISSION) { _, e ->
                MiniMessage.miniMessage().deserialize("You do not have permission to execute this command: ${e.message}")
            }
            .withHandler(MinecraftExceptionHandler.ExceptionType.INVALID_SENDER) { _, e ->
                MiniMessage.miniMessage().deserialize("You cannot execute this command: ${e.message}")
            }
            .withDecorator { component ->
                MiniMessage.miniMessage().deserialize(lang.prefixes.firstNotNullOf { it.value }).append(component)
            }.apply(manager) { it }

        playerFlag = manager.flagBuilder("player")
            .withDescription(RichDescription.of(MiniMessage.miniMessage().deserialize("The target player else the command sender.")))
            .withPermission(Permission.of("terix.target.others"))
            .withAliases("p")
            .withArgument(PlayerArgument.newBuilder<Player>("player").asOptional().build())

        originArgument = manager.argumentBuilder(Origin::class.java, "origin")
            .withSuggestionsProvider { _, _ ->
                OriginServiceImpl.getService().registeredOrigins
            }.withParser { _, inputQueue ->
                try {
                    ArgumentParseResult.success(OriginServiceImpl.getService().getOrigin(inputQueue.peek().lowercase()))
                } catch (e: NoSuchElementException) {
                    ArgumentParseResult.failure(e)
                }
            }
    }

    @Throws(InvalidCommandSenderException::class)
    private fun getTargetOrThrow(context: CommandContext<CommandSender>) = context.flags().getValue<Player>("player").orElseGet {
        context.sender as? Player ?: throw InvalidCommandSenderException(context.sender, Player::class.java, emptyList())
    }

    private fun getTargetedPath(
        langPath: String,
        sender: CommandSender,
        target: Player
    ): String {
        return buildString {
            append(langPath)

            if (lastOrNull() != null && last() != '.') {
                append('.')
            }

            if (sender === target) {
                append("self")
            } else append("other")
        }
    }

    private fun getOrigin(
        sender: CommandSender,
        target: Player? = sender as? Player
    ) {
        val origin = (target)?.let { TerixPlayer.cachedOrigin(it) } ?: run {
            return lang.generic.error[
                "message" to { "This command must have a target or be sent by a Player." }
            ] message sender
        }

        lang.origin[getTargetedPath("get", sender, target)][
            "origin" to { origin.displayName },
            "player" to { target.displayName() }
        ] message sender
    }

    private fun setOrigin(
        sender: CommandSender,
        target: Player,
        origin: Origin
    ) {
        val currentOrigin = TerixPlayer.cachedOrigin(target)

        if (origin == currentOrigin) {
            return lang.origin[getTargetedPath("set.same", sender, target)][
                "origin" to { origin.displayName },
                "player" to { target.displayName() }
            ] message sender
        }
        lang.origin[getTargetedPath("set", sender, target)][
            "old_origin" to { currentOrigin.displayName },
            "new_origin" to { origin.displayName },
            "player" to { target.displayName() }
        ] message sender
        PlayerOriginChangeEvent(target, currentOrigin, origin, true).callEvent()
    }

    private fun getChoices(
        sender: CommandSender,
        player: Player
    ) {
        lang.choices[getTargetedPath("get", sender, player)][
            "choices" to { player.freeChanges },
            "player" to { player.displayName() }
        ] message sender
    }

    private fun addChoices(
        sender: CommandSender,
        player: Player,
        amount: Int
    ) {
        if (amount < 1) {
            return lang.generic.error[
                "message" to { "Amount must be greater than 0." }
            ] message sender
        }

        transaction(Terix.database) {
            val terixPlayer = TerixPlayer[player.uniqueId]
            terixPlayer.freeChanges += amount

            lang.choices[getTargetedPath("mutate", sender, player)][
                "choices" to { terixPlayer.freeChanges },
                "player" to { player.displayName() }
            ] message sender
        }
    }

    private fun removeChoices(
        sender: CommandSender,
        player: Player,
        amount: Int
    ) {
        if (amount < 1) {
            return lang.generic.error[
                "message" to { "Amount must be greater than 0." }
            ] message sender
        }

        transaction(Terix.database) {
            val terixPlayer = TerixPlayer[player.uniqueId]
            val freeChanges = (terixPlayer.freeChanges - amount).coerceAtLeast(0)
            terixPlayer.freeChanges = freeChanges

            lang.choices[getTargetedPath("mutate", sender, player)][
                "choices" to { freeChanges },
                "player" to { player.displayName() }
            ] message sender
        }
    }

    private fun grantOrigin(
        sender: CommandSender,
        player: Player,
        origin: Origin
    ) {
    }

    private fun describeOrigin(
        sender: CommandSender,
        origin: Origin,
        info: OriginInfo.Info
    ) {
        lang.origin.descriptor.head[
            "origin" to { origin.displayName },
            "category" to { info.name }
        ] message sender

        info.get(origin).children().forEach(sender::sendMessage)

        lang.origin.descriptor.footer.get() message sender
    }
}
