package dev.racci.terix.core.services

import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator
import cloud.commandframework.kotlin.extension.commandBuilder
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler
import cloud.commandframework.paper.PaperCommandManager
import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.racci.minix.api.coroutine.launch
import dev.racci.minix.api.coroutine.launchAsync
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.core.extension.command
import dev.racci.terix.core.extension.execute
import dev.racci.terix.core.extension.executePlayer
import dev.racci.terix.core.extension.formatted
import dev.racci.terix.core.extension.fulfilled
import dev.racci.terix.core.extension.getCast
import dev.racci.terix.core.extension.origin
import dev.racci.terix.core.extension.subcommand
import dev.racci.terix.core.data.PlayerData
import kotlinx.collections.immutable.persistentListOf
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityCombustEvent
import org.bukkit.potion.PotionEffectType
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.core.component.inject

class CommandService(override val plugin: Terix) : Extension<Terix>() {
    private val guiService by inject<GUIService>()
    private val langService by inject<LangService>()
    private val originService by inject<OriginService>()
    private val specialService by inject<SpecialService>()
    private val commandCoordinator by lazy {
        AsynchronousCommandExecutionCoordinator
            .newBuilder<CommandSender>()
            .withAsynchronousParsing()
            .build()
    }
    private val commandManager by lazy { PaperCommandManager.createNative(plugin, commandCoordinator) }
    private val exceptionHandler by lazy {
        MinecraftExceptionHandler<CommandSender>()
            .withArgumentParsingHandler()
            .withInvalidSenderHandler()
            .withInvalidSyntaxHandler()
            .withNoPermissionHandler()
            .withCommandExecutionHandler()
            .withDecorator {

            }
    }

    override val name = "Command Service"
    override val dependencies = persistentListOf(LangService::class, OriginService::class, GUIService::class, SpecialService::class)

    override suspend fun handleEnable() {

        commandManager.commandBuilder("origin") {
            permission = "terix.origin"

        }

        command("origin") {
            aliases += "origins"
            permission = CommandPermission.fromString("terix.origin")

            addGetCommands()
            addSetCommands()
            addMenuCommands()
            addToggleCommands()
        }

        command("testing") {

            subcommand("fire") {
                withArguments(StringArgument("arg"))
                executePlayer { player, args ->
                    when (args.getCast<String>(0)) {
                        "0" -> { { player.fireTicks = 20 }.runSync() }
                        "1" -> { try { { player.fireTicks = 20 }.runAsync() } catch (e: Exception) { e.printStackTrace() } }
                        "3" -> { try { { EntityCombustEvent(player, 20) }.runAsync() } catch (e: Exception) { e.printStackTrace() } }
                        "4" -> { { player.toNMS().setSecondsOnFire(1, true) }.runSync() }
                        "6" -> { { player.toNMS().setSecondsOnFire(1, false) }.runSync() }
                        "7" -> { try { { player.toNMS().setSecondsOnFire(1, false) }.runAsync() } catch (e: Exception) { e.printStackTrace() } }
                    }
                }
            }

            subcommand("potions") {
                executePlayer { player, _ ->
                    player.activePotionEffects.forEach {
                        player.msg("${it.type.name} : ${it.key}")
                    }
                }
            }

            subcommand("attributes") {
                executePlayer { player, _ ->
                    Attribute.values().forEach {
                        player.getAttribute(it)?.let { inst ->
                            inst.modifiers.forEach { modi ->
                                player.msg("${it.name} : ${modi.name} : ${modi.amount} : ${modi.operation.name}")
                            }
                        }
                    }
                }
            }

            subcommand("remove") {
                executePlayer { player, _ ->
                    PotionEffectType.values().forEach {
                        player.removePotionEffect(it)
                    }
                    Attribute.values().forEach {
                        player.getAttribute(it)?.let { inst ->
                            inst.modifiers.forEach(inst::removeModifier)
                        }
                    }
                }
            }
        }

        command("terix") {
            permission = CommandPermission.fromString("terix.admin")

            subcommand("reload") {
                permission = CommandPermission.fromString("terix.admin.reload")

                execute { sender, _ ->
                    langService.handleReload()
                    langService[
                        "generic.reload.lang",
                    ] message sender
                }
            }
        }
    }

    private fun CommandAPICommand.addGetCommands() {
        subcommand("get") {
            permission = CommandPermission.fromString("terix.origin.get")
            withArguments(EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))

            execute { sender, args -> plugin.launchAsync { getOrigin(sender, args.getCast(0)!!) } }
        }

        subcommand("get") {
            permission = CommandPermission.fromString("terix.origin.get")

            executePlayer { player, _ -> plugin.launchAsync { getOrigin(player, player) } }
        }
    }

    private fun CommandAPICommand.addSetCommands() {
        subcommand("set") {
            permission = CommandPermission.fromString("terix.origin.set")
            withArguments(
                EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
                StringArgument("origin").replaceSuggestions { originService.registeredOrigins.toTypedArray() }
            )

            execute { sender, args -> plugin.launchAsync { setOrigin(sender, args.getCast(0), args.getCast(1)!!) } }
        }

        subcommand("set") {
            permission = CommandPermission.fromString("terix.origin.set")
            withArguments(StringArgument("origin").replaceSuggestions { originService.registeredOrigins.toTypedArray() })

            executePlayer { player, args -> plugin.launchAsync { setOrigin(player, player, args.getCast(0)!!) } }
        }
    }

    private fun CommandAPICommand.addMenuCommands() {
        subcommand("menu") {
            permission = CommandPermission.fromString("terix.origin.menu")

            executePlayer { player, _ -> guiService.baseGui.value.show(player) }
        }

        subcommand("menu") {
            permission = CommandPermission.fromString("terix.origin.menu.others")
            withArguments(EntitySelectorArgument("target", EntitySelectorArgument.EntitySelector.ONE_PLAYER))

            // TODO: Message response
            execute { _, args -> guiService.baseGui.value.show(args.getCast(0)!!) }
        }
    }

    private fun CommandAPICommand.addToggleCommands() {
        subcommand("toggle") {
            permission = CommandPermission.fromString("terix.origin.toggle")

            addSpecialCommand(
                "nightvision",
                { it.origin().nightVision },
                { sender, args -> plugin.launchAsync { nightvision(sender, args.getCast(0)) } }
            )
        }
    }

    private fun CommandAPICommand.addSpecialCommand(
        name: String,
        requirements: (Player) -> Boolean,
        execute: (Player, Array<out Any>) -> Unit
    ) {
        subcommand(name) {
            withArguments(StringArgument("trigger").replaceSuggestions { specialService.specialStatesFormatted })
            setRequirements { sender -> sender is Player && requirements(sender) }

            executePlayer(execute)
        }

        subcommand(name) {
            setRequirements { sender -> sender is Player && requirements(sender) }

            executePlayer(execute)
        }
    }

    private fun getOrigin(
        sender: CommandSender,
        target: Player? = sender as? Player
    ) {
        val origin = (target)?.origin() ?: run {
            langService[
                "generic.error",
                "message" to { "This command must have a target or be sent by a Player." }
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
        target: Player? = sender as? Player,
        originString: String
    ) {
        target ?: run {
            langService[
                "generic.error",
                "message" to { "This command must have a target or be sent by a Player." }
            ] message sender
            return
        }
        val origin = originService[originString, true] ?: run {
            langService[
                "generic.error",
                "message" to { "Invalid origin: $originString." },
            ] message sender
            return
        }
        if (origin == target.origin()) {
            langService[
                "origin.set.same.${if (target == sender) "self" else "other"}",
                "origin" to { origin.displayName },
                "target" to { target.displayName() },
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

    private fun nightvision(
        player: Player,
        nightVisionString: String? = null
    ) {
        val nightVision = nightVisionString?.let {
            val ordinal = specialService.specialStatesFormatted.indexOf(it)
            specialService.specialStates.value.getOrNull(ordinal)
        }
        val playerData = transaction { PlayerData[player] }
        if (nightVision != null && !specialService.isValidTrigger(nightVision)) {
            langService[
                "generic.error",
                "message" to { "Invalid trigger: $nightVision." },
            ] message player
            return
        }
        val current = transaction { playerData.nightVision }
        val new = nightVision ?: specialService.getToggle(player, current)
        transaction { playerData.nightVision = new }
        log.debug { new.fulfilled(player) }
        new.fulfilled(player).ifTrue {
            PotionEffectBuilder {
                type = PotionEffectType.NIGHT_VISION
                durationInt = Int.MAX_VALUE
                amplifier = 0
                particles = false
                ambient = false
                particles = false
                icon = false
                originKey(player.origin(), new)
            }.build().apply { plugin.launch { player.addPotionEffect(this@apply) } }
        }
        langService[
            "origin.nightvision",
            "old_nightvision" to { new.formatted() },
            "new_nightvision" to { new.formatted() },
        ] message player
    }
}
