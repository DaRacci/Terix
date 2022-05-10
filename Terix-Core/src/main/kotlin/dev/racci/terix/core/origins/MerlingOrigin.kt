package dev.racci.terix.core.origins

import com.destroystokyo.paper.MaterialTags
import dev.racci.minix.api.extensions.msg
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.dsl.TimedAttributeBuilder
import dev.racci.terix.api.origins.AbstractOrigin
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffectType
import kotlin.time.Duration.Companion.seconds

class MerlingOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Merling"
    override val colour = NamedTextColor.AQUA!!
    override val hurtSound = Key.key("entity.salmon.hurt")
    override val deathSound = Key.key("entity.salmon.death")

    override suspend fun onRegister() {
        item {
            material = Material.TRIDENT
            lore = """
                <aqua>A mysterious origin.
                <aqua>It's not clear what it is.
            """.trimIndent()
        }
        food {
            MaterialTags.RAW_FISH *= 2
            Material.COOKED_BEEF /= 2
            Material.COOKED_CHICKEN += { player: Player ->
                player.msg("You ate the cooked chicken!")
            }
            Material.COOKED_MUTTON += { builder: PotionEffectBuilder ->
                builder.type = PotionEffectType.BLINDNESS
                builder.duration = 5.seconds
                builder.amplifier = 4
                builder.ambient = false
            }
            Material.COOKED_PORKCHOP += { builder: TimedAttributeBuilder ->
                builder.attribute = Attribute.GENERIC_MAX_HEALTH
                builder.amount = 5.0
                builder.operation = AttributeModifier.Operation.ADD_NUMBER
                builder.duration = 15.seconds
            }
        }
    }
}
