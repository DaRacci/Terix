package dev.racci.terix.api.origins.abilities

import dev.racci.minix.api.coroutine.asyncDispatcher
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.ticks
import dev.racci.terix.api.Terix
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.events.PlayerAbilityActivateEvent
import dev.racci.terix.api.events.PlayerAbilityDeactivateEvent
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.OriginHelper
import dev.racci.terix.api.origins.origin.Origin
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
import org.bukkit.potion.PotionEffect
import org.koin.core.component.inject
import java.util.UUID
import kotlin.time.Duration

// TODO -> Per player instances
public abstract class Ability(
    private val abilityType: AbilityType
) : WithPlugin<Terix> {
    protected abstract val origin: Origin
    private val abilityCache: CooldownSet by lazy { CooldownSet(this) }
    private val namespacedKey: NamespacedKey by lazy { NamespacedKey(plugin, "origin_ability_${origin.name}/${this.name}") }

    final override val plugin: Terix by inject()

    /** The duration before the ability can be activated again. */
    public open val cooldown: Duration = 20.ticks

    public open val name: String = this::class.simpleName ?: throw IllegalStateException("Ability name is null")

    /**
     * Called when the ability is activated.
     * This is always called off the main thread.
     *
     * @param player The player who activated the ability.
     */
    public open suspend fun onActivate(player: Player): Unit = Unit

    /**
     * Called when the ability is deactivated.
     * This is always called off the main thread.
     *
     * @param player The player who deactivated the ability.
     */
    public open suspend fun onDeactivate(player: Player): Unit = Unit

    /**
     * Attempts to toggle this [player]'s ability.
     *
     * If the ability type is [AbilityType.TRIGGER], this will try to activate it.
     * If the ability type is [AbilityType.TOGGLE] and the ability is active for the player, this will try to deactivate it.
     *
     * @param player to toggle the ability for.
     * @return If the ability is on cooldown, this will return null. Otherwise, it will return true.
     */
    public suspend fun toggle(player: Player): Boolean? = when {
        OriginHelper.shouldIgnorePlayer(player) -> false
        this.abilityCache.containsCooldown(player.uniqueId) -> null
        this.isActivated(player.uniqueId) -> this.deactivate(player)
        else -> this.activate(player, null)
    }

    /** Call only from inside [onActivate] to show a failed ability. */
    protected fun failActivation(player: Player) {
        this.abilityCache.remove(player.uniqueId, true)
    }

    protected fun isActivated(uuid: UUID): Boolean = abilityType == AbilityType.TOGGLE && uuid in abilityCache

    protected fun taggedPotion(potionEffectBuilder: PotionEffectBuilder): PotionEffect = potionEffectBuilder.apply { key = namespacedKey }.get()

    protected fun isTagged(potionEffect: PotionEffect): Boolean = potionEffect.key == namespacedKey

    internal suspend fun activate(
        player: Player,
        forceValue: Instant?
    ): Boolean {
        if (!this.abilityCache.add(player.uniqueId, forceValue)) return false
        if (!PlayerAbilityActivateEvent(player, this).callEvent()) return false

        this.addToPersistentData(player)
        sentryScoped(player, SCOPE, "$name.activate", context = plugin.asyncDispatcher) {
            this.onActivate(player)
        }
        return true
    }

    internal suspend fun deactivate(player: Player): Boolean {
        if (!PlayerAbilityDeactivateEvent(player, this).callEvent()) return false

        this.abilityCache -= player.uniqueId
        this.removeFromPersistentData(player)

        sentryScoped(player, SCOPE, "$name.deactivate", context = plugin.asyncDispatcher) {
            this.onDeactivate(player)
        }
        return true
    }

    private fun addToPersistentData(player: Player) = async {
        return@async // FIXME
        val containers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)?.toMutableList() ?: mutableListOf<PersistentDataContainer>()
        val container = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER) ?: DirtyCraftPersistentDataContainer(CraftPersistentDataTypeRegistry())

        container.set(NAMESPACES[0], PersistentDataType.STRING, this@Ability.name)
        this@Ability.abilityCache[player.uniqueId]?.toEpochMilliseconds()?.let { container.set(NAMESPACES[1], PersistentDataType.LONG, it) }
        if (this@Ability.isActivated(player.uniqueId)) container.set(NAMESPACES[2], PersistentDataType.BYTE, 1)

        containers.add(container)
        player.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toTypedArray())
    }

    private fun removeFromPersistentData(player: Player) = async {
        return@async // FIXME
        val containers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)
        if (containers.isNullOrEmpty()) return@async

        val newContainers = containers.filter { it.get(NAMESPACES[0], PersistentDataType.STRING) != name }
        player.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, newContainers.toTypedArray())
    }

    public companion object : WithPlugin<Terix> {
        override val plugin: Terix by getKoin().inject()
        private val NAMESPACE = NamespacedKey.fromString("origin.abilities", plugin)!!
        private val NAMESPACES = arrayOf(
            NamespacedKey.fromString("origin.abilities.name", plugin)!!,
            NamespacedKey.fromString("origin.abilities.cooldown", plugin)!!,
            NamespacedKey.fromString("origin.abilities.active", plugin)!!
        )
        public const val SCOPE: String = "origin.abilities"

        init {
            event<PlayerJoinEvent>(EventPriority.MONITOR, true) {
                val abilityContainers = player.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)
                if (abilityContainers.isNullOrEmpty()) return@event

                for (container in abilityContainers) {
                    // FIXME
//                    val ability = with(container.get(NAMESPACES[0], PersistentDataType.STRING)) {
//                        if (this == null) return@with null
//                        OriginService.getAbilities().values.firstOrNull { it.name == this } ?: run {
//                            plugin.log.warn(scope = SCOPE) { "Player ${player.name} has an ability with the name $this, but no such ability exists." }
//                            return@with null
//                        }
//                    } ?: continue
//                    val lastUsed = container.get(NAMESPACES[1], PersistentDataType.LONG)?.let(Instant::fromEpochMilliseconds)
//                    val active = container.get(NAMESPACES[2], PersistentDataType.BYTE) == 1.toByte()
//
//                    if (active) ability.activate(player, lastUsed)
                }
            }

            event<PlayerOriginChangeEvent>(EventPriority.MONITOR, true) { player.persistentDataContainer.remove(NAMESPACE) }
        }
    }

    public enum class AbilityType { TOGGLE, TRIGGER }
}
