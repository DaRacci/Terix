package me.racci.sylphia.factories

import me.racci.raccicore.utils.math.MathUtils
import me.racci.sylphia.enums.Condition
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import java.util.*
import kotlin.collections.HashMap

object AttributeFactory {

    fun newCondition(
        condition: Condition,
        modifiers: HashMap<Attribute, Double>) : AttributeCondition {
        val map = EnumMap<Attribute, AttributeModifier>(Attribute::class.java)
        for(modifier in modifiers) {
            val multiplier = MathUtils.getMultiplierFromPercent(modifier.value)
            map[modifier.key] =
                AttributeModifier(condition.name, multiplier, AttributeModifier.Operation.ADD_SCALAR)
        }
        return AttributeCondition(map)
    }
}


data class AttributeCondition(var modifiers: EnumMap<Attribute, AttributeModifier>)
