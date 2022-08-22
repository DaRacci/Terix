package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.coroutine.minecraftDispatcher
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.async
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.onlinePlayers
import dev.racci.minix.api.extensions.scheduler
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.utils.getKoin
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.OriginService
import dev.racci.terix.api.Terix
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.events.PlayerAbilityDeactivateEvent
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.sentryScoped
import kotlinx.datetime.Instant
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataTypeRegistry
import org.bukkit.craftbukkit.v1_19_R1.persistence.DirtyCraftPersistentDataContainer
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class Ability(abilityType: AbilityType) : WithPlugin<Terix>, KoinComponent {
    final override val plugin: Terix by inject()

    protected val cooldownCache = mutableMapOf<UUID, Instant>()
    protected var abilityCache: CooldownSet? = null

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
    open suspend fun onActivate(player: Player) = Unit

    /** Called when the ability is deactivated the given player. */
    open suspend fun onDeactivate(player: Player) = Unit

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
        if (!force && !isAble(player) || isOnCooldown(player.uniqueId) || !PlayerAbilityActivateEvent(player, this).callEvent()) return false

        when {
            abilityCache == null -> Unit
            force -> abilityCache!!.forceAdd(player.uniqueId)
            else -> abilityCache!! += player.uniqueId
        }
        this.addToPersistentData(player)

        sentryScoped(player, SCOPE, "$name.activate", context = plugin.minecraftDispatcher) {
            onActivate(player)
        }
        return true
    }

    /** Returns true if the player has their ability deactivated or if the ability isn't toggleable, false otherwise. */
    suspend fun deactivate(player: Player): Boolean {
        if (!isActivated(player.uniqueId) || !PlayerAbilityDeactivateEvent(player, this).callEvent()) return false

        if (abilityCache != null) abilityCache!! -= player.uniqueId
        this.removeFromPersistentData(player)

        sentryScoped(player, SCOPE, "$name.deactivate", context = plugin.minecraftDispatcher) {
            onDeactivate(player)
        }
        return true
    }

    fun isActivated(uuid: UUID): Boolean = abilityCache != null && uuid in abilityCache!!

    fun isOnCooldown(uuid: UUID): Boolean = (cooldownCache[uuid] ?: Instant.DISTANT_PAST) > now()

    init {
        if (abilityType == AbilityType.TOGGLE) {
            @Suppress("LeakingThis")
            abilityCache = CooldownSet(this)
        }
    }

    private fun addToPersistentData(player: Player) = async {
        val containers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)?.toMutableList() ?: mutableListOf<PersistentDataContainer>()
        val container = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER) ?: DirtyCraftPersistentDataContainer(CraftPersistentDataTypeRegistry())

        container.set(NAMESPACES[0], PersistentDataType.STRING, this@Ability.name)
        this@Ability.cooldownCache[player.uniqueId]?.toEpochMilliseconds()?.let { container.set(NAMESPACES[1], PersistentDataType.LONG, it) }
        if (this@Ability.isActivated(player.uniqueId)) container.set(NAMESPACES[2], PersistentDataType.BYTE, 1)

        containers.add(container)
        player.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toTypedArray())
    }

    private fun removeFromPersistentData(player: Player) = async {
        val containers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)
        if (containers.isNullOrEmpty()) return@async

        val newContainers = containers.filter { it.get(NAMESPACES[0], PersistentDataType.STRING) != name }
        player.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, newContainers.toTypedArray())
    }

    companion object : WithPlugin<Terix> {
        override val plugin by getKoin().inject<Terix>()
        private val NAMESPACE = NamespacedKey.fromString("origin.abilities", plugin)!!
        private val NAMESPACES = arrayOf(
            NamespacedKey.fromString("origin.abilities.name", plugin)!!,
            NamespacedKey.fromString("origin.abilities.cooldown", plugin)!!,
            NamespacedKey.fromString("origin.abilities.active", plugin)!!
        )
        const val SCOPE = "origin.abilities"

        init {
            event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
                val abilityContainers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)
                if (abilityContainers.isNullOrEmpty()) return@event

                for (container in abilityContainers) {
                    val ability = with(container.get(NAMESPACES[0], PersistentDataType.STRING)) {
                        if (this == null) return@with null
                        OriginService.getAbilities().values.firstOrNull { it.name == this } ?: run {
                            plugin.log.warn(scope = SCOPE) { "Player ${player.name} has an ability with the name $this, but no such ability exists." }
                            return@with null
                        }
                    } ?: continue
                    val cooldown = container.get(NAMESPACES[1], PersistentDataType.LONG)?.let(Instant::fromEpochMilliseconds)
                    val active = container.get(NAMESPACES[2], PersistentDataType.BYTE) == 1.toByte()

                    if (cooldown != null) ability.cooldownCache[player.uniqueId] = cooldown
                    if (active) ability.activate(player, true)
                }
            }

            event<PlayerOriginChangeEvent>(EventPriority.MONITOR, true) { player.persistentDataContainer.remove(NAMESPACE) }
        }
    }

    class CooldownSet(
        private val ability: Ability
    ) : HashSet<UUID>() {
        /** Returns true if the player is off cooldown and was added to the set. */
        override fun add(element: UUID): Boolean {
            if (ability.isOnCooldown(element)) return false

            ability.cooldownCache[element] = now() + ability.cooldown
            return super.add(element)
        }

        /** Skips the cooldown check and adds the player to the set. */
        fun forceAdd(element: UUID): Boolean {
            ability.cooldownCache.putIfAbsent(element, now() + ability.cooldown) // Account for force activations from data persistence.
            return super.add(element)
        }

        override fun remove(element: UUID): Boolean {
            if (ability.cooldownCache[element]?.let { it < now() } == true) {
                ability.cooldownCache.remove(element) // If the players' cooldown has expired, remove it from the map.
            }
            return super.remove(element)
        }

        init {
            scheduler {
                if (onlinePlayers.isEmpty() || this.isEmpty()) return@scheduler

                val uuidList = onlinePlayers.map(Player::getUniqueId)
                for (player in this) {
                    if (player in uuidList) continue

                    this.remove(player)
                    ability.cooldownCache.remove(player)
                }
            }.runAsyncTaskTimer(plugin, Duration.ZERO, 15.seconds)
        }
    }

    enum class AbilityType { TOGGLE, TRIGGER, TARGET }
}
