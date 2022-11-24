package dev.racci.terix.api.origins.abilities.keybind

import dev.racci.minix.api.events.keybind.ComboEvent
import dev.racci.minix.api.extensions.WithPlugin
import dev.racci.minix.api.extensions.event
import dev.racci.minix.api.flow.playerEventFlow
import dev.racci.terix.api.Terix
import dev.racci.terix.api.TerixPlayer.User.origin
import dev.racci.terix.api.data.Cooldown
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.abilityCustomOf
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.applyTag
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.events.PlayerOriginChangeEvent
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.enums.KeyBinding
import kotlinx.coroutines.flow.onEach
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_19_R1.persistence.CraftPersistentDataTypeRegistry
import org.bukkit.craftbukkit.v1_19_R1.persistence.DirtyCraftPersistentDataContainer
import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import kotlin.time.Duration

// TODO -> Concurrent protection / mutex lock
public sealed class KeybindAbility : Ability() {
    public var cooldown: Cooldown = Cooldown.NONE; protected set
    private val namespacedKey: NamespacedKey by lazy { NamespacedKey(plugin, "origin_ability_${origin.name}/${this.name}") }

    /** The duration before the ability can be activated again. */
    public abstract val cooldownDuration: Duration

    /** Call only from inside [onActivate] to show a failed ability. */
    protected fun failActivation() {
        this.cooldown = Cooldown.NONE
    }

    protected fun taggedPotion(potionEffectBuilder: PotionEffectBuilder): PotionEffect = potionEffectBuilder.applyTag(abilityCustomOf(linkedOrigin, this)).get()

    protected fun PotionEffectBuilder.tagged(): PotionEffect = this.applyTag(abilityCustomOf(linkedOrigin, this@KeybindAbility)).get()

    protected fun isTagged(potionEffect: PotionEffect): Boolean = potionEffect.key == namespacedKey

    protected abstract suspend fun handleKeybind(event: Event)

    protected fun shouldIgnoreKeybind(event: Event): Boolean {
        return when (event) {
            is ComboEvent -> abilityPlayer.activeItem.type == Material.AIR || event.isBlockEvent && event.item?.type?.isBlock == true
            else -> false
        }
    }

    internal fun activateWithKeybinding(keybind: KeyBinding) = playerEventFlow(
        keybind.event,
        player = abilityPlayer,
        plugin = plugin,
        priority = EventPriority.MONITOR,
        ignoreCancelled = true,
        listener = this.listener
    ).onEach(this::handleKeybind).abilitySubscription()

    // FIXME
    // TODO -> Convert to a NBT Data savable system.
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

    // FIXME
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
