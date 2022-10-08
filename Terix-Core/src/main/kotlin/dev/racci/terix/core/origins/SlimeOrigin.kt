package dev.racci.terix.core.origins

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import dev.racci.minix.api.data.enums.LiquidType
import dev.racci.minix.api.events.PlayerEnterLiquidEvent
import dev.racci.minix.api.events.player.PlayerLiquidExitEvent
import dev.racci.minix.api.extensions.cancel
import dev.racci.minix.api.extensions.dropItem
import dev.racci.minix.api.utils.now
import dev.racci.minix.api.utils.unsafeCast
import dev.racci.minix.nms.aliases.toNMS
import dev.racci.terix.api.Terix
import dev.racci.terix.api.annotations.OriginEventSelector
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import dev.racci.terix.api.origins.sounds.SoundEffect
import dev.racci.terix.api.origins.states.State
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import java.time.Duration
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

// TODO -> Top food is a bottle of water.
// TODO -> Water melon, Honey, and soup / stew (No rabbit stew) is good too.
// TODO -> CAke.
// TODO -> More damage from fire and lava (25-50%).
// TODO -> More damage from swords.
class SlimeOrigin(override val plugin: Terix) : Origin() {

    override val name = "Slime"
    override val colour = TextColor.fromHexString("#61f45a")!!

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

    override suspend fun handleRegister() {
        sounds.hurtSound = SoundEffect("entity.slime.hurt")
        sounds.deathSound = SoundEffect("entity.slime.death")
        sounds.ambientSound = SoundEffect("entity.slime.ambient")

        potions {
            State.CONSTANT += dslMutator {
                type = PotionEffectType.JUMP
                amplifier = 4
                duration = kotlin.time.Duration.INFINITE
                ambient = true
            }
        }
        attributes {
            Attribute.GENERIC_MAX_HEALTH *= 0.8
        }
        damage {
            EntityDamageEvent.DamageCause.FALL
            EntityDamageEvent.DamageCause.FALL += { cause ->
                if (cause.entity.unsafeCast<Player>().toNMS().random.nextBoolean()) {
                    cause.cancel()
                }
            }
        }
        item {
            material = Material.SLIME_BALL
            lore = "<green>Slime lol"
        }
    }

    // TODO -> Fix this shit code
    @OriginEventSelector(EventSelector.PLAYER)
    suspend fun PlayerEnterLiquidEvent.handle() {
        if (newType != LiquidType.WATER) return
        val cache = playerCache[player]
        if (cache["in-water"] == true) return
        cache["in-water"] = true
        while (playerHealthCache[player.uniqueId] < 5.0) {
            if (LiquidType.convert(player.location.block) != LiquidType.WATER) break

            val modifier = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.modifiers.find { it.name == "ORIGIN_SLIME_HEALTH" }
            val bonus = (modifier?.amount ?: 0.0) + 1.0

            if (bonus <= 5.0) {
                modifier?.let { player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.removeModifier(it) }
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.addModifier(
                    AttributeModifier(
                        "ORIGIN_SLIME_HEALTH",
                        bonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
                player.health += 1
            }

            playerHealthCache.put(player.uniqueId, bonus)
            delay(1.seconds)
        }
    }

    @OriginEventSelector(EventSelector.PLAYER)
    suspend fun PlayerLiquidExitEvent.onExitLiquid() {
        if (previousType != LiquidType.WATER) return
        val cache = playerCache[player]
        if (cache["in-water"] == false) return
        cache["in-water"] = false

        while (playerHealthCache[player.uniqueId] > 0.0) {
            if (LiquidType.convert(player.location.block) == LiquidType.WATER) break

            val modifier = player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.modifiers.find { it.name == "ORIGIN_SLIME_HEALTH" }
            val bonus = (modifier?.amount ?: 0.0) - 1.0

            player.health = if (player.health - 1.0 <= 0.5) 0.5 else player.health - 1

            modifier?.let { player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.removeModifier(it) }
            if (bonus > 0.0) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH)!!.addModifier(
                    AttributeModifier(
                        "ORIGIN_SLIME_HEALTH",
                        bonus,
                        AttributeModifier.Operation.ADD_NUMBER
                    )
                )
            }

            playerHealthCache.put(player.uniqueId, bonus)
            delay(1.seconds)
        }
    }

    private val lastDamage = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(15))
        .build<Player, Instant>()

    @OriginEventSelector(EventSelector.ENTITY)
    fun onDamage(event: EntityDamageEvent) {
        val last = lastDamage.getIfPresent(event.entity.unsafeCast<Player>())
        val now = now()

        if (last != null && (last + 15.seconds) > now()) return

        lastDamage.put(event.entity.unsafeCast<Player>(), now)
        event.entity.location.dropItem(ItemStack(Material.SLIME_BALL))
    }
}
