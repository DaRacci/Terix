package dev.racci.terix.core.utils

import dev.racci.minix.api.builders.ItemBuilderDSL
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.incendo.interfaces.core.util.Vector2
import org.incendo.interfaces.kotlin.paper.MutableChestPaneView
import org.incendo.interfaces.kotlin.paper.asElement
import kotlin.jvm.Throws

public object InterfaceUtils {
    public val defaultReplacements: List<Material> = listOf(
        Material.PINK_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE
    )

    public fun mask(
        chestView: MutableChestPaneView,
        rawMask: String,
        replacements: List<Material> = defaultReplacements
    ) {
        val mask = rawMask.trim().toMutableList()
        mask.retainAll(Char::isDigit)

        for (i in mask.indices) {
            val char = mask[i]

//            val row = Math.floorDiv(i, 9)
//            val col = (i) % 9
            val material = replacements.getOrNull(char.toString().toInt()) ?: continue

            chestView[toVec(i)] = ItemBuilderDSL.from(material) {
                name = Component.empty()
            }.asElement()
        }
    }

    /** Gets a Vector2 from a string in the format of "x;y". */
    @Throws(IllegalArgumentException::class, NumberFormatException::class)
    public fun toVec(
        string: String?,
        rows: Int = -1,
        xOffset: Int = 0,
        yOffset: Int = 1
    ): Vector2 {
        if (string == null) throw IllegalArgumentException("String cannot be null!")

        var (x, y) = string.split(";").map(String::toInt)
        x = if (rows > 0 && (x == -1 || rows < x)) rows - 1 else x
        x -= xOffset
        y -= yOffset

        return Vector2.at(y, x)
    }

    public fun toVec(
        index: Int,
        rowOffset: Int = 0,
        colOffset: Int = 0
    ): Vector2 {
        val x = (index - rowOffset) / 9 + rowOffset
        val y = (index - colOffset) % 9 + colOffset

        return Vector2.at(y, x)
    }
}
