package dev.racci.terix.api.origins.enums

import dev.racci.minix.api.extensions.reflection.safeCast
import dev.racci.terix.api.origins.enums.EventSelector.PlayerSelector
import dev.racci.terix.api.origins.origin.Origin
import org.apiguardian.api.API
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.Event
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.valueParameters

@Suppress("UnusedReceiverParameter")
@API(status = API.Status.EXPERIMENTAL, since = "1.0.0")
public enum class EventSelector(public val selector: TargetSelector<Any, *>) {
    /** Selects the entity from an [EntityEvent]. */
    ENTITY(PlayerSelector<EntityEvent> { it.entity as? Player }),

    /** Selects the player from a [PlayerEvent]. */
    PLAYER(PlayerSelector<PlayerEvent> { it.player }),

    /** Selects the killer from an [EntityDeathEvent]. */
    KILLER(PlayerSelector<EntityDeathEvent> { it.entity.killer }),

    /** Selects the target from an [EntityTargetLivingEntityEvent]. */
    TARGET(PlayerSelector<EntityTargetLivingEntityEvent> { it.target.safeCast() }),

    /** Selects the shooter from any [EntityEvent] where the entity is a [Projectile]. */
    SHOOTER(PlayerSelector<EntityEvent> { it.entity.safeCast<Projectile>()?.shooter.safeCast() }),

    /** Selects the damager from an [EntityDamageByEntityEvent]. */
    OFFENDER(PlayerSelector<EntityDamageByEntityEvent> { it.damager.safeCast() });

    public sealed interface TargetSelector<out E : Any, R : Any> {
        public operator fun invoke(event: @UnsafeVariance E): R?

        public companion object {
            public inline fun <reified E : Any> TargetSelector<E, *>.isCompatible(func: KFunction<*>): Boolean {
                if (func.extensionReceiverParameter != null && func.extensionReceiverParameter!!.type.classifier.safeCast<KClass<E>>() != null) return true
                if (func.valueParameters.size != 1) return false
                if (func.valueParameters[0].type.classifier.safeCast<E>() == null) return false

                return true
            }
        }
    }

    public fun interface PlayerSelector<E : Event> : TargetSelector<E, Player> {
        override operator fun invoke(event: E): Player?
    }

    public fun interface OriginSelector<E : Event> : TargetSelector<E, Origin> {
        override operator fun invoke(event: E): Origin?
    }
}
