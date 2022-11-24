package dev.racci.terix.api.extensions // ktlint-disable filename

import org.bukkit.Location
import org.bukkit.block.Block

public inline fun Location.below(distance: Double = 1.0): Location = this.clone().add(0.0, -distance, 0.0)

public inline fun Location.above(distance: Double = 1.0): Location = this.clone().add(0.0, distance, 0.0)

public inline fun Block.below(distance: Int = 1): Block = this.getRelative(0, -distance, 0)

public inline fun Block.above(distance: Int = 1): Block = this.getRelative(0, distance, 0)
