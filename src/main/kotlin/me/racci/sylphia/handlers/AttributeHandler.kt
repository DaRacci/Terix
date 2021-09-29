package me.racci.sylphia.handlers

import me.racci.sylphia.enums.Condition
import me.racci.sylphia.extensions.PlayerExtension.currentOrigin
import me.racci.sylphia.utils.AttributeUtils
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
    private fun getBase(player: Player) : EnumMap<Attribute, Double>? {
        return player.currentOrigin?.parentAttribute
    }
    /**
     * Sets the base attributes for the players' origin.
     *
     * Thread Safe
     *
     * @param player
     */
    fun setBase(player: Player) {
        for(attribute in getBase(player) ?: return) {
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
    fun hasBase(player: Player) : Boolean {
        for(attribute in getBase(player) ?: return true) {
            if(player.getAttribute(attribute.key)!!.baseValue != attribute.value) return false
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
    private fun getCondition(player: Player, condition: Condition) : EnumMap<Attribute, AttributeModifier> {
        return player.currentOrigin?.attributeMap?.get(condition)?.modifiers ?: EnumMap(Attribute::class.java)
    }
    /**
     * Adds the attribute modifiers for the given condition.
     *
     * Thread Safe
     *
     * @param player
     * @param condition
     */
    fun setCondition(player: Player, condition: Condition) {
        for(modifier in getCondition(player, condition)) {
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
    fun removeCondition(player: Player, condition: Condition) {
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
    fun hasCondition(player: Player, condition: Condition) : Boolean {
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
        for(attribute in AttributeUtils.getPlayerAttributes()) {
            player.getAttribute(attribute)!!.modifiers.clear()
            player.getAttribute(attribute)!!.baseValue = AttributeUtils.getDefault(attribute)
        }
        for(attribute in getBase(player).orEmpty()) {
            player.getAttribute(attribute.key)!!.baseValue = attribute.value
        }
    }
}