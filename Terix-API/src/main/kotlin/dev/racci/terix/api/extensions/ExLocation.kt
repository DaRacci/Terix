package dev.racci.terix.api.extensions // ktlint-disable filename

import org.bukkit.Location

public fun Location.below(distance: Double = 1.0): Location = this.clone().add(0.0, -distance, 0.0)

public fun Location.above(distance: Double = 1.0): Location = this.clone().add(0.0, distance, 0.0)
