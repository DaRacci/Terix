package dev.racci.terix.core.services

import dev.jorel.commandapi.CommandAPICommand
import dev.jorel.commandapi.CommandPermission
import dev.jorel.commandapi.arguments.ArgumentSuggestions
import dev.jorel.commandapi.arguments.BooleanArgument
import dev.jorel.commandapi.arguments.DoubleArgument
import dev.jorel.commandapi.arguments.EntitySelector
import dev.jorel.commandapi.arguments.EntitySelectorArgument
import dev.jorel.commandapi.arguments.StringArgument
import dev.racci.minix.api.annotations.MappedExtension
import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.formatted
import dev.racci.minix.api.extensions.message
import dev.racci.minix.api.extensions.msg
import dev.racci.minix.api.services.DataService
import dev.racci.minix.api.services.DataService.Companion.inject
import dev.racci.minix.api.utils.collections.CollectionUtils.getCast
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.core.data.Config
import dev.racci.terix.core.data.Lang
import dev.racci.terix.core.extensions.arguments
import dev.racci.terix.core.extensions.command
import dev.racci.terix.core.extensions.execute
import dev.racci.terix.core.extensions.executePlayer
import dev.racci.terix.core.extensions.fulfilled
import dev.racci.terix.core.extensions.nightVision
import dev.racci.terix.core.extensions.origin
import dev.racci.terix.core.extensions.safelyAddPotion
import dev.racci.terix.core.extensions.safelyRemovePotion
import dev.racci.terix.core.extensions.subcommand
import dev.racci.terix.core.extensions.usedChoices
import org.bukkit.Fluid
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.command.CommandSender
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.entity.Villager
import org.bukkit.potion.PotionEffectType
import org.koin.core.component.get
import org.koin.core.component.inject
import java.util.UUID
import java.util.logging.Level
import kotlin.time.Duration

@MappedExtension(Terix::class, "Command Service", [OriginService::class, GUIService::class, SpecialService::class])
class CommandService(override val plugin: Terix) : Extension<Terix>() {
    private val lang by inject<DataService>().inject<Lang>()

    override suspend fun handleEnable() {
        command("origin") {
            aliases += "origins"
            permission = CommandPermission.fromString("terix.origin")

            addGetCommands()
            addSetCommands()
            addMenuCommands()
            addToggleCommands()
            addDatabaseCommands()
        }

        command("testing") {
            subcommand("oxygen") {
                executePlayer { player, _ ->
                    player.isReverseOxygen = !player.isReverseOxygen
                    player.msg("oxygen.${if (player.isReverseOxygen) "enabled" else "disabled"}")
                }
            }

            subcommand("debug") {
                arguments {
                    arg<BooleanArgument>("debug")
                }
                execute { _, a ->
                    val bool = a.getCast<Boolean>(0)
                    plugin.logger.level = if (bool) Level.OFF else Level.ALL
                }
            }

            subcommand("stand") {
                arguments {
                    arg<EntitySelectorArgument<LivingEntity>>("entity")
                }
                executePlayer { player, anies ->
                    val entity = anies.getCast<LivingEntity>(0)
                    Fluid.values().forEach {
                        entity.addCanStandOnFluid(it)
                    }
                }
            }

            subcommand("remove") {
                arguments {
                    arg<EntitySelectorArgument<LivingEntity>>("entity")
                }
                executePlayer { player, anies ->
                    val entity = anies.getCast<LivingEntity>(0)
                    Fluid.values().forEach {
                        entity.removeCanStandOnFluid(it)
                    }
                }
            }

            subcommand("discount") {
                arguments {
                    arg<EntitySelectorArgument<Villager>>("villager")
                    arg<DoubleArgument>("value")
                }

                executePlayer { player, anies ->
                    val villager = anies.getCast<Villager>(0)
                    val amount = anies.getCast<Double>(1)

                    if (amount == -1.0) {
                        villager.recipes.forEach {
                            it.removePriceMultiplier(player.uniqueId)
                        }
                    } else {
                        villager.recipes.forEach {
                            it.setPriceMultiplier(player.uniqueId, amount)
                        }
                    }
                }
            }

            subcommand("attribute") {
                arguments {
                    arg<StringArgument>("attribute").replaceSuggestions(ArgumentSuggestions.strings(*Attribute.values().map(Attribute::name).toTypedArray()))
                    arg<StringArgument>("operation").replaceSuggestions(ArgumentSuggestions.strings(*AttributeModifier.Operation.values().map(AttributeModifier.Operation::name).toTypedArray()))
                    arg<DoubleArgument>("value")
                }

                executePlayer { player, anies ->
                    val attribute = Attribute.valueOf(anies.getCast(0))
                    val operation = AttributeModifier.Operation.valueOf(anies.getCast(1))
                    val value = anies.getCast<Double>(2)

                    val uuid = UUID.randomUUID()
                    val modifier = AttributeModifier(uuid, uuid.toString(), value, operation)
                    player.getAttribute(attribute)?.addModifier(modifier)
                }
            }

            subcommand("clearAttributes") {
                arguments {
                    arg<StringArgument>("attribute").replaceSuggestions(
                        ArgumentSuggestions.strings(
                            *Attribute.values()
                                .map(Attribute::name)
                                .toTypedArray()
                        )
                    )
                }
                executePlayer { player, anies ->
                    val inst = player.getAttribute(Attribute.valueOf(anies.getCast(0))) ?: return@executePlayer
                    inst.modifiers.forEach(inst::removeModifier)
                }
            }

            subcommand("origin") {
                executePlayer { player, _ ->
                    player.origin().toString() message player
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
                    get<DataService>().configurations.refresh(Lang::class)
                    lang.generic.reloadLang.value message sender
                }
            }
        }
    }

    private fun CommandAPICommand.addGetCommands() {
        subcommand("get") {
            permission = CommandPermission.fromString("terix.origin.getOriginFromName")
            arguments {
                arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER)
            }

            execute { sender, args -> async { getOrigin(sender, args.getCast(0)) } }
        }

        subcommand("get") {
            permission = CommandPermission.fromString("terix.origin.getOriginFromName")

            executePlayer { player, _ -> async { getOrigin(player, player) } }
        }
    }

    private fun CommandAPICommand.addSetCommands() {
        val suggestions = ArgumentSuggestions.strings { OriginServiceImpl.getService().registeredOrigins }
        subcommand("set") {
            permission = CommandPermission.fromString("terix.origin.set")

            arguments {
                arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER)
                arg<StringArgument>("origin").replaceSuggestions(suggestions)
            }

            execute { sender, args -> async { setOrigin(sender, args.getCast(0), args.getCast(1)) } }
        }
    }

    private fun CommandAPICommand.addMenuCommands() {
        subcommand("menu") {
            permission = CommandPermission.fromString("terix.origin.menu")

            executePlayer { player, _ -> GUIService.getService().baseGui.value.show(player) }
        }

        subcommand("menu") {
            permission = CommandPermission.fromString("terix.origin.menu.others")
            arguments {
                arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER)
            }

            // TODO: Message response
            execute { _, args -> GUIService.getService().baseGui.value.show(args.getCast(0)) }
        }
    }

    private fun CommandAPICommand.addToggleCommands() {
        subcommand("toggle") {
            permission = CommandPermission.fromString("terix.origin.toggle")

            addSpecialCommand(
                "nightvision",
                { it.origin().nightVision },
                { sender, args -> async { nightvision(sender, args.getCast(0)) } }
            )
        }
    }

    private fun CommandAPICommand.addSpecialCommand(
        name: String,
        requirements: (Player) -> Boolean,
        execute: (Player, Array<out Any>) -> Unit
    ) {
        subcommand(name) {
            arguments {
                arg<StringArgument>("trigger").replaceSuggestions(ArgumentSuggestions.strings { SpecialService.getService().specialStatesFormatted })
            }
            setRequirements { sender -> sender is Player && requirements(sender) }

            executePlayer(execute)
        }

        subcommand(name) {
            setRequirements { sender -> sender is Player && requirements(sender) }

            executePlayer(execute)
        }
    }

    private fun CommandAPICommand.addDatabaseCommands() {
        subcommand("database") {
            permission = CommandPermission.fromString("terix.origin.database")

            subcommand("choice") {
                arguments {
                    arg<StringArgument>("type").replaceSuggestions(ArgumentSuggestions.strings("add, remove, clear"))
                }

                fun message(sender: CommandSender, target: Player, changed: String) {
                    lang[
                        "database.choices.${if (target == sender) "self" else "other"}",
                        "changed" to { changed },
                        "player" to { target.displayName() },
                        "choices" to { DataService.getService().get<Config>().freeChanges - target.usedChoices }
                    ] message sender
                }

                subcommand("add") {
                    permission = CommandPermission.fromString("terix.origin.database.choice")
                    arguments { arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER) }

                    execute { commandSender, anies ->
                        val target = anies.getCast<Player>(0)
                        target.usedChoices--
                        message(commandSender, target, "now ")
                    }
                }

                subcommand("remove") {
                    permission = CommandPermission.fromString("terix.origin.database.choice")
                    arguments { arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER) }

                    execute { commandSender, anies ->
                        val target = anies.getCast<Player>(0)
                        target.usedChoices++
                        message(commandSender, target, "now ")
                    }
                }

                subcommand("reset") {
                    permission = CommandPermission.fromString("terix.origin.database.choice")
                    arguments { arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER) }

                    execute { commandSender, anies ->
                        val target = anies.getCast<Player>(0)
                        target.usedChoices = 0
                        message(commandSender, target, "now ")
                    }
                }

                subcommand("get") {
                    permission = CommandPermission.fromString("terix.origin.database.choice")
                    arguments { arg<EntitySelectorArgument<Player>>("target", EntitySelector.ONE_PLAYER) }

                    execute { commandSender, anies ->
                        val target = anies.getCast<Player>(0)
                        target.usedChoices = DataService.getService().get<Config>().freeChanges
                        message(commandSender, target, "")
                    }
                }
            }
        }
    }

    private fun getOrigin(
        sender: CommandSender,
        target: Player? = sender as? Player
    ) {
        val origin = (target)?.origin() ?: run {
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
        target: Player? = sender as? Player,
        originString: String
    ) {
        target ?: run {
            return lang.generic.error[
                "message" to { "This command must have a target or be sent by a Player." }
            ] message sender
        }
        val origin = OriginServiceImpl.getService().getOriginOrNull(originString.lowercase()) ?: run {
            return lang.generic.error[
                "message" to { "Invalid origin: $originString." }
            ] message sender
        }
        if (origin == target.origin()) {
            return lang[
                "origin.set.same.${if (target == sender) "self" else "other"}",
                "origin" to { origin.displayName },
                "player" to { target.displayName() }
            ] message sender
        }
        lang[
            "origin.set.${if (target == sender) "self" else "other"}",
            "old_origin" to { target.origin().displayName },
            "new_origin" to { origin.displayName },
            "player" to { target.displayName() }
        ] message sender
        PlayerOriginChangeEvent(target, target.origin(), origin, true).callEvent()
    }

    private fun nightvision(
        player: Player,
        nightVisionString: String? = null
    ) {
        val nightVision = nightVisionString?.let {
            val ordinal = SpecialService.getService().specialStatesFormatted.indexOf(it)
            SpecialService.getService().specialStates.getOrNull(ordinal)
        }
        if (nightVision != null && !SpecialService.getService().isValidTrigger(nightVision)) {
            return lang.generic.error[
                "message" to { "Invalid trigger: $nightVision." }
            ] message player
        }

        val current = player.nightVision
        val new = nightVision ?: SpecialService.getService().getToggle(player, current)
        player.nightVision = new
        log.debug { new.fulfilled(player) }

        val shouldApply = new.fulfilled(player)

        if (shouldApply) {
            PotionEffectBuilder.build {
                type = PotionEffectType.NIGHT_VISION
                duration = Duration.INFINITE
                amplifier = 0
                ambient = true
                originKey(player.origin(), new)
            }.apply(player::safelyAddPotion)
        }

        if (!shouldApply && current.fulfilled(player)) {
            player.safelyRemovePotion(PotionEffectType.NIGHT_VISION)
        }

        lang.origin.nightVision[
            "old_nightvision" to { new.formatted() },
            "new_nightvision" to { new.formatted() }
        ] message player
    }
}
