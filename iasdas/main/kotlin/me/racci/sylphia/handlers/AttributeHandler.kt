package me.racci.terix.handlers

import me.racci.terix.enums.Condition
import me.racci.terix.extensions.PlayerExtension.currentOrigin
import me.racci.terix.factories.Origin
import me.racci.terix.utils.AttributeUtils
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import java.util.*

object AttributeHandler {
    /**
     * Gets the players Parent Attributes / base values
     *
     * Thread Safe
     *
     * @param player
     * @return EnumMap<Attribute, Double>
     */
    private fun getBase(player: Player, origin: Origin? = player.currentOrigin): EnumMap<Attribute, Double>? {
        return origin?.baseAttributes
    }
    /**
     * Sets the base attributes for the players' origin.
     *
     * Thread Safe
     *
     * @param player
     */
    fun setBase(player: Player, origin: Origin? = player.currentOrigin) {
        for (attribute in getBase(player, origin) ?: return) {
            player.getAttribute(attribute.key)!!.baseValue = attribute.value
        }
    }
    /**
     * Returns true if the players base attributes match,
     * the origins base attributes
     *
     * Thread Safe
     *
     * @param [player]
     * @return [Boolean]
     */
    fun hasBase(player: Player, origin: Origin? = player.currentOrigin): Boolean {
        for (attribute in getBase(player, origin) ?: return true) {
            if (player.getAttribute(attribute.key)!!.baseValue != attribute.value) return false
        }
        return true
    }
    /**
     * Gets the matching EnumMap for the condition.
     *
     * Thread Safe
     *
     * @param player
     * @param condition
     * @return EnumMap<Attribute, AttributeModifier>
     */
    private fun getCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin): EnumMap<Attribute, AttributeModifier> {
        return origin?.attributes?.get(condition)?.modifiers ?: EnumMap(Attribute::class.java)
    }
    /**
     * Adds the attribute modifiers for the given condition.
     *
     * Thread Safe
     *
     * @param player
     * @param condition
     */
    fun setCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) {
        if (origin == null) return
        for (modifier in getCondition(player, condition)) {
            player.getAttribute(modifier.key)!!.addModifier(modifier.value)
        }
    }
    /**
     * Removes the attribute modifiers for the given condition.
     *
     * Thread Safe
     *
     * @param player
     * @param condition
     */
    fun removeCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin) {
        if (origin == null) return
        for (modifier in getCondition(player, condition)) {
            player.getAttribute(modifier.key)!!.removeModifier(modifier.value)
        }
    }
    /**
     * Returns true if the players has all attribute modifiers,
     * for the given condition.
     *
     * Thread Safe
     *
     * @param player
     * @param condition
     * @return
     */
    fun hasCondition(player: Player, condition: Condition, origin: Origin? = player.currentOrigin): Boolean {
        if (origin == null) return true
        for (modifier in getCondition(player, condition)) {
            if (player.getAttribute(modifier.key)!!.modifiers
                .map(AttributeModifier::getName)
                .firstOrNull { it == condition.name }
                == null
            ) return false
        }
        return true
    }
    /**
     * Resets all the players attributes and removes all modifiers,
     * Then sets the base attributes for the players origin
     *
     * Thread Safe
     *
     * @param player The Target Player
     */
    fun reset(player: Player) {
        for (attribute in AttributeUtils.getPlayerAttributes()) {
            player.getAttribute(attribute)?.modifiers?.clear()
            player.getAttribute(attribute)?.baseValue = AttributeUtils.getDefault(attribute)
        }
        for (attribute in getBase(player).orEmpty()) {
            player.getAttribute(attribute.key)!!.baseValue = attribute.value
        }
    }
}
