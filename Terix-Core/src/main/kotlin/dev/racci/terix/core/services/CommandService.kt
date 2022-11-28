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
import dev.racci.minix.api.utils.PropertyFinder
import dev.racci.minix.api.utils.adventure.PartialComponent
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.data.Lang
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.services.StorageService
import dev.racci.terix.core.commands.TerixPermissions
import dev.racci.terix.core.commands.arguments.OriginArgument
import dev.racci.terix.core.commands.arguments.OriginInfoArgument
import dev.racci.terix.core.origins.OriginInfo
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.Optional
import kotlin.properties.Delegates

@MappedExtension(Terix::class, "Command Service", [OriginService::class, GUIService::class])
public class CommandService(override val plugin: Terix) : Extension<Terix>() {
    private val lang: Lang by inject<DataService>().inject()
    private var registered = false

    private var manager by Delegates.notNull<PaperCommandManager<CommandSender>>()
    private var playerFlag by Delegates.notNull<CommandFlag.Builder<Player>>()
    private var originArgument by Delegates.notNull<CommandArgument.Builder<CommandSender, Origin>>()

    override suspend fun handleEnable() {
        if (!registered) {
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
                permission(TerixPermissions.commandOriginGet)
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
                permission(TerixPermissions.commandOriginSet)
                suspendingHandler(supervisor, dispatcher.get()) { context -> setOrigin(context.sender, getTargetOrThrow(context), context.get("origin")) }
            }

            this.registerCopy(
                "menu",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Open the origin menu."))
            ) {
                permission(TerixPermissions.menu)
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, plugin.minecraftDispatcher) { context -> get<GUIService>().openMenu(getTargetOrThrow(context)) }
            }

            this.registerCopy(
                "choice",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Parent choice commands."))
            ) {
                permission(TerixPermissions.commandChoiceGet.or(TerixPermissions.commandChoiceSet))

                this.registerCopy(
                    "get",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Gets the remaining choices of a player."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(TerixPermissions.commandChoiceGet)
                    suspendingHandler(supervisor, dispatcher.get()) { context -> getChoices(context.sender, getTargetOrThrow(context)) }
                }

                registerCopy(
                    "add",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Adds a choice to a player."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(TerixPermissions.commandChoiceSet)
                    argument(IntegerArgument.of("amount"))
                    suspendingHandler(supervisor, dispatcher.get()) { context -> addChoices(context.sender, getTargetOrThrow(context), context.get("amount")) }
                }.registerCopy(
                    "remove",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Removes a choice from a player."))
                ) { suspendingHandler(supervisor, dispatcher.get()) { context -> removeChoices(context.sender, getTargetOrThrow(context), context.get("amount")) } }
            }

            this.registerCopy(
                "grant",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Parent grant commands."))
            ) {
                permission(TerixPermissions.commandGrantAdd.or(TerixPermissions.commandGrantRevoke))

                this.registerCopy(
                    "add",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Explicitly grants an origin skipping any requirements."))
                ) {
                    mutate { it.flag(playerFlag) }
                    permission(TerixPermissions.commandGrantAdd)
                    argument(OriginArgument("origin", true, RichDescription.of(MiniMessage.miniMessage().deserialize("The target origin."))))
                    suspendingHandler(supervisor, dispatcher.get()) { context -> grantOrigin(context.sender, getTargetOrThrow(context), context.get("origin")) }
                }.registerCopy(
                    "revoke",
                    RichDescription.of(MiniMessage.miniMessage().deserialize("Removes the explicit access given to a player."))
                ) { suspendingHandler(supervisor, dispatcher.get()) { context -> ungrantOrigin(context.sender, getTargetOrThrow(context), context.get("origin")) } }
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
    private fun getTargetOrThrow(context: CommandContext<CommandSender>) = context.flags().getValue<Player>("player").or {
        Optional.ofNullable(context.sender as? Player ?: throw InvalidCommandSenderException(context.sender, Player::class.java, emptyList()))
    }.map(TerixPlayer::get).get()

    private fun dualMessageContext(
        startNode: PropertyFinder<PartialComponent>,
        langPath: String,
        sender: CommandSender,
        target: Player,
        vararg placeholders: Pair<String, () -> Any>
    ) {
        startNode[getTargetedPath(langPath, sender, target)].get(*placeholders) message sender
        if (sender !== target) {
            startNode[getTargetedPath(langPath, sender, target)].get(*placeholders) message target
        }
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

        val event = PlayerOriginChangeEvent(target, currentOrigin, origin, true, skipRequirement = true)
        event.callEvent()
        if (event.result != PlayerOriginChangeEvent.Result.SUCCESS) {
            lang.origin.cancelledCommand["reason" to { event.result.name }] message sender
        }
    }

    private fun getChoices(
        sender: CommandSender,
        player: TerixPlayer
    ) {
        lang.choices[getTargetedPath("get", sender, player)][
            "choices" to { player.databaseEntity.freeChanges },
            "player" to { player.displayName() }
        ] message sender
    }

    private fun addChoices(
        sender: CommandSender,
        player: TerixPlayer,
        amount: Int
    ) {
        if (amount < 1) {
            return lang.generic.error[
                "message" to { "Amount must be greater than 0." }
            ] message sender
        }

        StorageService.transaction {
            player.databaseEntity.freeChanges += amount

            lang.choices[getTargetedPath("mutate", sender, player)][
                "choices" to { player.databaseEntity.freeChanges },
                "player" to { player.displayName() }
            ] message sender
        }
    }

    private fun removeChoices(
        sender: CommandSender,
        player: TerixPlayer,
        amount: Int
    ) {
        if (amount < 1) {
            return lang.generic.error[
                "message" to { "Amount must be greater than 0." }
            ] message sender
        }

        StorageService.transaction {
            val freeChanges = (player.databaseEntity.freeChanges - amount).coerceAtLeast(0)
            player.databaseEntity.freeChanges = freeChanges

            lang.choices[getTargetedPath("mutate", sender, player)][
                "choices" to { freeChanges },
                "player" to { player.displayName() }
            ] message sender
        }
    }

    private fun grantOrigin(
        sender: CommandSender,
        player: TerixPlayer,
        origin: Origin
    ) {
        StorageService.transaction {
            if (!player.databaseEntity.grants.add(origin.name)) {
                return@transaction lang.origin[getTargetedPath("grant.already", sender, player)][
                    "origin" to { origin.displayName },
                    "player" to { player.displayName() }
                ] message sender
            }

            dualMessageContext(
                lang.origin,
                "grant",
                sender,
                player,
                "origin" to { origin.displayName },
                "player" to { player.displayName() }
            )
        }
    }

    private fun ungrantOrigin(
        sender: CommandSender,
        player: TerixPlayer,
        origin: Origin
    ) {
        StorageService.transaction {
            if (!player.databaseEntity.grants.remove(origin.name)) {
                return@transaction lang.origin[getTargetedPath("grant.missing", sender, player)][
                    "origin" to { origin.displayName },
                    "player" to { player.displayName() }
                ] message sender
            }

            dualMessageContext(
                lang.origin,
                "grant.removed",
                sender,
                player,
                "origin" to { origin.displayName },
                "player" to { player.displayName() }
            )
        }
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
