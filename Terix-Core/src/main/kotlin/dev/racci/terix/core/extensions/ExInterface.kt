package dev.racci.terix.core.extensions // ktlint-disable filename

import dev.racci.terix.core.utils.InterfaceUtils
import org.bukkit.Material
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView

public fun MutableChestPaneView.mask(
    rawMask: String,
    replacements: List<Material> = InterfaceUtils.defaultReplacements
): Unit = InterfaceUtils.mask(this, rawMask, replacements)

public fun String?.toVec(
    rows: Int = -1,
    xOffset: Int = 0,
    yOffset: Int = 1
): Vector2 = InterfaceUtils.toVec(this, rows, xOffset, yOffset)

public fun Int.fromIndex(
    rowOffset: Int = 0,
    colOffset: Int = 0
): Vector2 = InterfaceUtils.toVec(this, rowOffset, colOffset)
