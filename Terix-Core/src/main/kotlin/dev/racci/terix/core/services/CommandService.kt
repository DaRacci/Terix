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
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.freeChanges
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
    private var playerFlag by Delegates.notNull<CommandFlag<Player>>()
    private var originArgument by Delegates.notNull<CommandArgument<CommandSender, Origin>>()

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
                permission(Permission.of("terix.command.origin.set"))
                argument(originArgument, RichDescription.of(MiniMessage.miniMessage().deserialize("The origin to set the target too.")))
                suspendingHandler(supervisor, dispatcher.get()) { context -> setOrigin(context.sender, getTargetOrThrow(context), context.get(originArgument)) }
            }

            this.registerCopy(
                "menu",
                RichDescription.of(MiniMessage.miniMessage().deserialize("Open the origin menu."))
            ) {
                permission(Permission.of("terix.command.origin.menu"))
                mutate { it.flag(playerFlag) }
                suspendingHandler(supervisor, dispatcher.get()) { context -> get<GUIService>().baseGui.value.show(getTargetOrThrow(context)) }
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

//        command("testing") {
//            subcommand("oxygen") {
//                executePlayer { player, _ ->
//                    player.isReverseOxygen = !player.isReverseOxygen
//                    player.msg("oxygen.${if (player.isReverseOxygen) "enabled" else "disabled"}")
//                }
//            }
//
//            subcommand("debug") {
//                arguments {
//                    arg<BooleanArgument>("debug")
//                }
//                execute { _, a ->
//                    val bool = a.getCast<Boolean>(0)
//                    plugin.logger.level = if (bool) Level.OFF else Level.ALL
//                }
//            }
//
//            subcommand("stand") {
//                arguments {
//                    arg<EntitySelectorArgument<LivingEntity>>("entity")
//                }
//                executePlayer { _, anies ->
//                    val entity = anies.getCast<LivingEntity>(0)
//                    Fluid.values().forEach {
//                        entity.addCanStandOnFluid(it)
//                    }
//                }
//            }
//
//            subcommand("remove") {
//                arguments {
//                    arg<EntitySelectorArgument<LivingEntity>>("entity")
//                }
//                executePlayer { _, anies ->
//                    val entity = anies.getCast<LivingEntity>(0)
//                    Fluid.values().forEach {
//                        entity.removeCanStandOnFluid(it)
//                    }
//                }
//            }
//
//            subcommand("discount") {
//                arguments {
//                    arg<EntitySelectorArgument<Villager>>("villager")
//                    arg<DoubleArgument>("value")
//                }
//
//                executePlayer { player, anies ->
//                    val villager = anies.getCast<Villager>(0)
//                    val amount = anies.getCast<Double>(1)
//
//                    if (amount == -1.0) {
//                        villager.recipes.forEach {
//                            it.removePriceMultiplier(player.uniqueId)
//                        }
//                    } else {
//                        villager.recipes.forEach {
//                            it.setPriceMultiplier(player.uniqueId, amount)
//                        }
//                    }
//                }
//            }
//
//            subcommand("attribute") {
//                arguments {
//                    arg<StringArgument>("attribute").replaceSuggestions(ArgumentSuggestions.strings(*Attribute.values().map(Attribute::name).toTypedArray()))
//                    arg<StringArgument>("operation").replaceSuggestions(ArgumentSuggestions.strings(*AttributeModifier.Operation.values().map(AttributeModifier.Operation::name).toTypedArray()))
//                    arg<DoubleArgument>("value")
//                }
//
//                executePlayer { player, anies ->
//                    val attribute = Attribute.valueOf(anies.getCast(0))
//                    val operation = AttributeModifier.Operation.valueOf(anies.getCast(1))
//                    val value = anies.getCast<Double>(2)
//
//                    val uuid = UUID.randomUUID()
//                    val modifier = AttributeModifier(uuid, uuid.toString(), value, operation)
//                    player.getAttribute(attribute)?.addModifier(modifier)
//                }
//            }
//
//            subcommand("clearAttributes") {
//                arguments {
//                    arg<StringArgument>("attribute").replaceSuggestions(
//                        ArgumentSuggestions.strings(
//                            *Attribute.values()
//                                .map(Attribute::name)
//                                .toTypedArray()
//                        )
//                    )
//                }
//                executePlayer { player, anies ->
//                    val inst = player.getAttribute(Attribute.valueOf(anies.getCast(0))) ?: return@executePlayer
//                    inst.modifiers.forEach(inst::removeModifier)
//                }
//            }
//
//            subcommand("origin") {
//                executePlayer { player, _ ->
//                    TerixPlayer.cachedOrigin(player).toString() message player
//                }
//            }
//
//            subcommand("potions") {
//                executePlayer { player, _ ->
//                    player.activePotionEffects.forEach {
//                        player.msg("${it.type.name} : ${it.key}")
//                    }
//                }
//            }
//
//            subcommand("attributes") {
//                executePlayer { player, _ ->
//                    Attribute.values().forEach {
//                        player.getAttribute(it)?.let { inst ->
//                            inst.modifiers.forEach { modi ->
//                                player.msg("${it.name} : ${modi.name} : ${modi.amount} : ${modi.operation.name}")
//                            }
//                        }
//                    }
//                }
//            }
//
//            subcommand("remove") {
//                executePlayer { player, _ ->
//                    PotionEffectType.values().forEach {
//                        player.removePotionEffect(it)
//                    }
//                    Attribute.values().forEach {
//                        player.getAttribute(it)?.let { inst ->
//                            inst.modifiers.forEach(inst::removeModifier)
//                        }
//                    }
//                }
//            }
//        }
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
                val exception = e.unsafeCast<InvalidCommandSenderException>()
                MiniMessage.miniMessage().deserialize("Invalid syntax: ${exception.currentChain.getOrNull(0)?.name} - ${exception.command?.arguments?.joinToString(", ")}")
            }
            .withHandler(MinecraftExceptionHandler.ExceptionType.COMMAND_EXECUTION) { _, e ->
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
            .withArgument(PlayerArgument.newBuilder<Player>("player").asOptional().build()).build()

        originArgument = manager.argumentBuilder(Origin::class.java, "origin")
            .withSuggestionsProvider { _, currentInput ->
                println("Current input: $currentInput")
                OriginServiceImpl.getService().registeredOrigins
            }.withParser { _, inputQueue ->
                println("Input Queue: $inputQueue")
                try {
                    ArgumentParseResult.success(OriginServiceImpl.getService().getOrigin(inputQueue.peek()))
                } catch (e: NoSuchElementException) {
                    ArgumentParseResult.failure(e)
                }
            }.build()
    }

    @Throws(InvalidCommandSenderException::class)
    private fun getTargetOrThrow(context: CommandContext<CommandSender>) = context.flags().getValue(playerFlag).orElseGet {
        context.sender as? Player ?: throw InvalidCommandSenderException(context.sender, Player::class.java, emptyList())
    }

    private fun getTargetedPath(
        langPath: String,
        sender: CommandSender,
        target: Player
    ): String {
        var result = if (sender === target) "self" else "other"
        return buildString {
            append(langPath)

            if (lastOrNull() != null) {
                result = result.replaceFirstChar(Char::uppercaseChar)
            }

            append(result)
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

        lang[
            "origin.get.${if (target == sender) "self" else "other"}",
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
            return lang[
                "origin.set.same.${if (target == sender) "self" else "other"}",
                "origin" to { origin.displayName },
                "player" to { target.displayName() }
            ] message sender
        }
        lang[
            "origin.set.${if (target == sender) "self" else "other"}",
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
                "amount" to { terixPlayer.freeChanges },
                "player" to { player.displayName() }
            ]
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
            val newAmount = terixPlayer.freeChanges - amount

            if (newAmount < 0) {
                terixPlayer.freeChanges = 0
            } else terixPlayer.freeChanges = newAmount

            lang.choices[getTargetedPath("mutate", sender, player)][
                "amount" to { terixPlayer.freeChanges },
                "player" to { player.displayName() }
            ]
        }
    }
}
