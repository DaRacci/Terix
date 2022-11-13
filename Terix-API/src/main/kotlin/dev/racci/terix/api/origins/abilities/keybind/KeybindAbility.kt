package dev.racci.terix.api.origins.abilities.keybind

import arrow.analysis.pre
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.extensions.ticks
import dev.racci.minix.api.flow.playerEventFlow
import dev.racci.minix.api.utils.now
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer.User.origin
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.enums.KeyBinding
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Instant
import org.apiguardian.api.API
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataTypeRegistry
import org.bukkit.craftbukkit.v1_19_R1.persistence.DirtyCraftPersistentDataContainer
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import kotlin.time.Duration

// TODO -> Per player instances
// TODO -> Concurrent protection / mutex lock
// TODO -> Separate into toggleable and trigger-able abilities
public abstract class KeybindAbility : Ability() {
    protected var activatedAt: Instant? = null
    private val namespacedKey: NamespacedKey by lazy { NamespacedKey(plugin, "origin_ability_${origin.name}/${this.name}") }

    /** The duration before the ability can be activated again. */
    public open val cooldown: Duration = 20.ticks

    public abstract val isActivated: Boolean

    /** Call only from inside [onActivate] to show a failed ability. */
    protected fun failActivation() {
        pre(!isActivated) { "Ability was not activated." }
        this.activatedAt = null
    }

    protected fun taggedPotion(potionEffectBuilder: PotionEffectBuilder): PotionEffect = potionEffectBuilder.apply { key = namespacedKey }.get()

    protected fun isTagged(potionEffect: PotionEffect): Boolean = potionEffect.key == namespacedKey

    @API(status = API.Status.INTERNAL)
    protected fun cooldownRemaining(): Duration = cooldown - (now() - activatedAt!!)

    @API(status = API.Status.INTERNAL)
    protected fun cooldownExpired(): Boolean = activatedAt == null || ((activatedAt!! + cooldown) >= now())

    internal fun activateWithKeybinding(keybind: KeyBinding) = playerEventFlow(
        keybind.event,
        player = abilityPlayer,
        plugin = plugin,
        priority = EventPriority.MONITOR,
        ignoreCancelled = true,
        listener = this.listener
    ).onEach { _ ->
        when (this) {
            is TogglingKeybindAbility -> this.toggle()
            is TriggeringKeybindAbility -> this.trigger()
        }
    }.abilitySubscription()

    internal fun addToPersistentData() = async {
        return@async // FIXME
        val containers = abilityPlayer.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)?.toMutableList() ?: mutableListOf<PersistentDataContainer>()
        val container = abilityPlayer.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER) ?: DirtyCraftPersistentDataContainer(CraftPersistentDataTypeRegistry())

        container.set(NAMESPACES[0], PersistentDataType.STRING, this@KeybindAbility.name)
//        this@KeybindAbility.abilityCache[abilityPlayer.uniqueId]?.toEpochMilliseconds()?.let { container.set(NAMESPACES[1], PersistentDataType.LONG, it) }
//        if (this@KeybindAbility.isActivated(abilityPlayer.uniqueId)) container.set(NAMESPACES[2], PersistentDataType.BYTE, 1)

        containers.add(container)
        abilityPlayer.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, containers.toTypedArray())
    }

    internal fun removeFromPersistentData() = async {
        return@async // FIXME
        val containers = abilityPlayer.persistentDataContainer.get(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY)
        if (containers.isNullOrEmpty()) return@async

        val newContainers = containers.orEmpty().filter { it.get(NAMESPACES[0], PersistentDataType.STRING) != name }
        abilityPlayer.persistentDataContainer.set(NAMESPACE, PersistentDataType.TAG_CONTAINER_ARRAY, newContainers.toTypedArray())
    }

    public companion object : WithPlugin<Terix> {
        override val plugin: Terix by getKoin().inject()
        private val NAMESPACE = NamespacedKey.fromString("origin.abilities", plugin)!!
        private val NAMESPACES = arrayOf(
            NamespacedKey.fromString("origin.abilities.name", plugin)!!,
            NamespacedKey.fromString("origin.abilities.cooldown", plugin)!!,
            NamespacedKey.fromString("origin.abilities.active", plugin)!!
        )

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
}
