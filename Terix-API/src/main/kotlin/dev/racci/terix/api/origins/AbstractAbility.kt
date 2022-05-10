package dev.racci.terix.api.origins

import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.Terix
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO: Look into some sort of persistence system for abilities through player metadata
abstract class AbstractAbility : WithPlugin<Terix>, KoinComponent {
    final override val plugin: Terix by inject()

    protected val cooldownCache = mutableMapOf<UUID, Instant>()
    protected val abilityCache = object : HashSet<UUID>() {
        /** Returns true if the player is off cooldown and was added to the set. */
        override fun add(element: UUID): Boolean {
            if (isOnCooldown(element)) return false

            cooldownCache[element] = now() + cooldown
            return super.add(element)
        }

        override fun remove(element: UUID): Boolean {
            if (cooldownCache[element]?.let { it < now() } == true) {
                cooldownCache.remove(element) // If the players cooldown has expired, remove it from the map.
            }
            return super.remove(element)
        }

        init {
            scheduler {
                if (onlinePlayers.isEmpty() || this.isEmpty()) return@scheduler

                val uuidList = onlinePlayers.map(Player::getUniqueId)
                for (player in this) {
                    if (player in uuidList) continue
                    remove(player)
                }
            }.runAsyncTaskTimer(plugin, Duration.ZERO, 15.seconds)
        }
    }

    /** The duration before the ability can be activated again. */
    protected open val cooldown: Duration = 20.ticks

    /** Returns if this player is able to activate this ability. */
    protected open fun isAble(player: Player): Boolean = true

    /* Called when the ability is activated the given player. */
    open suspend fun onActivate(player: Player) {}

    /* Called when the ability is deactivated the given player. */
    open suspend fun onDeactivate(player: Player) {}

    /** Returns true if the player had their ability activated, false otherwise. */
    suspend fun toggle(player: Player): Boolean = if (isActivated(player.uniqueId)) {
        deactivate(player)
        false
    } else {
        activate(player)
    }

    /** Returns true if the player has their ability activated, false otherwise. */
    suspend fun activate(player: Player): Boolean {
        if (!isAble(player) || isOnCooldown(player.uniqueId)) return false

        abilityCache += player.uniqueId
        onActivate(player)
        return true
    }

    /** Returns true if the player has their ability deactivated, false otherwise. */
    suspend fun deactivate(player: Player): Boolean {
        if (!isActivated(player.uniqueId)) return false

        abilityCache -= player.uniqueId
        onDeactivate(player)
        return true
    }

    fun isActivated(uuid: UUID): Boolean = uuid in abilityCache

    fun isOnCooldown(uuid: UUID): Boolean = (cooldownCache[uuid] ?: Instant.DISTANT_PAST) > now()
}
