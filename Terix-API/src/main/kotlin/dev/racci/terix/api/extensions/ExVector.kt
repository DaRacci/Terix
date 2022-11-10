package dev.racci.terix.api.extensions

import org.bukkit.util.Vector

public fun downVector(): Vector = Vector(0.0, -90.0, 0.0)

public fun upVector(): Vector = Vector(0.0, 90.0, 0.0)

public fun leftVector(): Vector = Vector(-90.0, 0.0, 0.0)

public fun rightVector(): Vector = Vector(90.0, 0.0, 0.0)
