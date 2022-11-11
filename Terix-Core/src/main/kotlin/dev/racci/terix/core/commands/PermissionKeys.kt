package dev.racci.terix.core.commands

import cloud.commandframework.permission.Permission

internal object TerixPermissions {
    val selectionBypassCooldown = Permission.of("terix.selection.bypass-cooldown")

    val commandOriginGet = Permission.of("terix.command.origin.get")
    val commandOriginSet = Permission.of("terix.command.origin.set")

    val commandChoiceGet = Permission.of("terix.command.choice.get")
    val commandChoiceSet = Permission.of("terix.command.choice.set")

    val commandGrantAdd = Permission.of("terix.command.grant.add")
    val commandGrantRevoke = Permission.of("terix.command.grant.revoke")

    val menu = Permission.of("terix.menu")
}