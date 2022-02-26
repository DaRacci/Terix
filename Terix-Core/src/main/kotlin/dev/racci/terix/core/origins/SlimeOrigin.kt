package dev.racci.terix.core.origins

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.racci.minix.api.events.LiquidType
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.PlayerExitLiquidEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.parse
import dev.racci.terix.api.Terix
import dev.racci.terix.api.origins.AbstractOrigin
import dev.racci.terix.api.origins.enums.Trigger
import kotlinx.coroutines.delay
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

class SlimeOrigin(override val plugin: Terix) : AbstractOrigin() {

    override val name = "Slime"
    override val colour: NamedTextColor = NamedTextColor.LIGHT_PURPLE
    override val hurtSound = Key.key("minecraft", "entity.slime.hurt")
    override val deathSound = Key.key("minecraft", "entity.slime.death")

    private val playerHealthCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(15))
        .removalListener { key: UUID?, value: Double?, cause ->
            if (cause == RemovalCause.EXPIRED && (value ?: 0.0) > 0.0) {
                putBack(1, key!!, value!!)
            }
        }.build { _: UUID -> 0.0 }

    private val playerCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofSeconds(30))
        .removalListener<Player, MutableMap<String, Boolean>> { player, map, cause ->
            if (cause == RemovalCause.EXPIRED && player?.isOnline == true) {
                putBack(2, player, map!!)
            }
        }.build<Player, MutableMap<String, Boolean>> { mutableMapOf() }

    private fun putBack(id: Int, key: Any, value: Any): Unit =
        when (id) {
            1 -> playerHealthCache.put(key as UUID, value as Double)
            2 -> playerCache.put(key as Player, value as MutableMap<String, Boolean>)
            else -> Unit
        }

    override suspend fun onRegister() {
        potions {
            Trigger.ON causes {
                type = PotionEffectType.JUMP
                amplifier = 4
                durationInt = Int.MAX_VALUE
                ambient = true
            }
        }
        attributes {
            Attribute.GENERIC_MAX_HEALTH setBase 16.0
        }
        damage {
            EntityDamageEvent.DamageCause.FALL triggers { if (Random.nextBoolean()) { cancel() } }
        }
        item {
            named(displayName)
            material(Material.SLIME_BALL)
            lore {
                this[1] = "<green>Slime lol".parse()
            }
        }
    }

    override suspend fun onEnterLiquid(event: PlayerEnterLiquidEvent) {
        if (event.newType != LiquidType.WATER) return
        val cache = playerCache[event.player]
        if (cache["in-water"] == true) return
        cache["in-water"] = true
        while (playerHealthCache[event.player.uniqueId] < 5.0) {

            if (LiquidType.convert(event.player.location.block) != LiquidType.WATER) break

            val modifier = event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.modifiers.find { it.name == "ORIGIN_SLIME_HEALTH" }
            val bonus = (modifier?.amount ?: 0.0) + 1.0

            if (bonus <= 5.0) {
                modifier?.let { event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.removeModifier(it) }
                event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.addModifier(
                    AttributeModifier(
                        "ORIGIN_SLIME_HEALTH",
                        bonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                event.player.health += 1
            }

            playerHealthCache.put(event.player.uniqueId, bonus)
            delay(1.seconds)
        }
    }

    override suspend fun onExitLiquid(event: PlayerExitLiquidEvent) {

        if (event.previousType != LiquidType.WATER) return
        val cache = playerCache[event.player]
        if (cache["in-water"] == false) return
        cache["in-water"] = false

        while (playerHealthCache[event.player.uniqueId] > 0.0) {

            if (LiquidType.convert(event.player.location.block) == LiquidType.WATER) break

            val modifier = event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.modifiers.find { it.name == "ORIGIN_SLIME_HEALTH" }
            val bonus = (modifier?.amount ?: 0.0) - 1.0

            event.player.health = if (event.player.health - 1.0 <= 0.5) 0.5 else event.player.health - 1

            modifier?.let { event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.removeModifier(it) }
            if (bonus > 0.0) {
                event.player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.addModifier(
                    AttributeModifier(
                        "ORIGIN_SLIME_HEALTH",
                        bonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            playerHealthCache.put(event.player.uniqueId, bonus)
            delay(1.seconds)
        }
    }
}
