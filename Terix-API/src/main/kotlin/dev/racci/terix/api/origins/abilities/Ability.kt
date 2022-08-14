package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.Terix
import dev.racci.terix.api.sentryScoped
import kotlinx.datetime.Instant
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO: Look into some sort of persistence system for abilities through player metadata
abstract class Ability(abilityType: AbilityType) : WithPlugin<Terix>, KoinComponent {
    final override val plugin: Terix by inject()

    protected val cooldownCache = mutableMapOf<UUID, Instant>()
    protected var abilityCache: HashSet<UUID>? = null

    /** The duration before the ability can be activated again. */
    protected open val cooldown: Duration = 20.ticks

    open val name: String = this::class.simpleName ?: throw IllegalStateException("Ability name is null")

    /** Returns if this player is able to activate this ability. */
    protected open fun isAble(player: Player): Boolean {
        val currentTime = now()
        val nextActivation = cooldownCache[player.uniqueId]

        return nextActivation == null || currentTime >= nextActivation
    }

    /** Called when the ability is activated the given player. */
    open suspend fun onActivate(player: Player) {}

    /** Called when the ability is deactivated the given player. */
    open suspend fun onDeactivate(player: Player) {}

    /** Returns true if the player had their ability activated, false otherwise. */
    suspend fun toggle(player: Player): Boolean {
        when {
            isActivated(player.uniqueId) -> deactivate(player)
            isAble(player) -> activate(player, false)
        }

        return isActivated(player.uniqueId)
    }

    /** Returns true if the player has their ability activated, false otherwise. */
    suspend fun activate(
        player: Player,
        force: Boolean
    ): Boolean {
        if (!force && !isAble(player) || isOnCooldown(player.uniqueId)) return false

        if (abilityCache != null) abilityCache!! += player.uniqueId

        sentryScoped(player, CATEGORY, "$name.activate", context = plugin.minecraftDispatcher) {
            onActivate(player)
        }
        return true
    }

    /** Returns true if the player has their ability deactivated or if the ability isn't toggleable, false otherwise. */
    suspend fun deactivate(player: Player): Boolean {
        if (!isActivated(player.uniqueId)) return false

        if (abilityCache != null) abilityCache!! -= player.uniqueId

        sentryScoped(player, CATEGORY, "$name.deactivate", context = plugin.minecraftDispatcher) {
            onDeactivate(player)
        }
        return true
    }

    fun isActivated(uuid: UUID): Boolean = abilityCache != null && uuid in abilityCache!!

    fun isOnCooldown(uuid: UUID): Boolean = (cooldownCache[uuid] ?: Instant.DISTANT_PAST) > now()

    init {
        if (abilityType == AbilityType.TOGGLE) {
            abilityCache = object : HashSet<UUID>() {
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
        }
    }

    companion object {
        const val CATEGORY = "terix.origin.abilities"
    }

    enum class AbilityType { TOGGLE, TRIGGER, TARGET }
}
