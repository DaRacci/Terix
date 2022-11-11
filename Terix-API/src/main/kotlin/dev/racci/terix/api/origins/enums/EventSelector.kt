package dev.racci.terix.api.origins.enums

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.racci.minix.api.extensions.reflection.castOrThrow
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
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

@API(status = API.Status.EXPERIMENTAL, since = "1.0.0")
public enum class EventSelector(public val selector: TargetSelector<*, *>) {
    /** Selects the entity from an [EntityEvent]. */
    ENTITY(PlayerSelector(EntityEvent::class) { (this.entity as? Player) }),

    /** Selects the player from a [PlayerEvent]. */
    PLAYER(PlayerSelector(PlayerEvent::class) { this.player }),

    /** Selects the killer from an [EntityDeathEvent]. */
    KILLER(PlayerSelector(EntityDeathEvent::class) { this.entity.killer }),

    /** Selects the target from an [EntityTargetLivingEntityEvent]. */
    TARGET(PlayerSelector(EntityTargetLivingEntityEvent::class) { (this.target as? Player) }),

    /** Selects the shooter from any [EntityEvent] where the entity is a [Projectile]. */
    SHOOTER(PlayerSelector(EntityEvent::class) { ((this.entity as? Projectile)?.shooter as? Player) }),

    /** Selects the damager from an [EntityDamageByEntityEvent]. */
    OFFENDER(PlayerSelector(EntityDamageByEntityEvent::class) { (this.damager as? Player) });

    public operator fun invoke(event: Event): Either<Player?, Origin?> = selector.castOrThrow<TargetSelector<Event, *>>()(event)

    public sealed interface TargetSelector<E : Event, R : Either<Player?, Origin?>> {
        public val eventType: KClass<E>
        public operator fun invoke(event: E): R

        public fun isApplicable(event: KClass<*>): Boolean = eventType.isSuperclassOf(event)

        public companion object {
            public fun <E : Event> TargetSelector<E, *>.isCompatible(func: KFunction<*>): Boolean {
                val receiver = func.valueParameters.getOrElse(0) { func.extensionReceiverParameter } ?: return false
                val receiverType = receiver.type.jvmErasure

                return func.parameters.size == 2 && this.isApplicable(receiverType)
            }
        }
    }

    public class PlayerSelector<E : Event>(
        override val eventType: KClass<E>,
        public val selector: E.() -> Player?
    ) : TargetSelector<E, Either<Player?, Nothing>> {
        override operator fun invoke(event: E): Either<Player?, Nothing> = selector(event).left()
    }

    public class OriginSelector<E : Event>(
        override val eventType: KClass<E>,
        public val selector: E.() -> Origin?
    ) : TargetSelector<E, Either<Nothing, Origin?>> {
        override operator fun invoke(event: E): Either<Nothing, Origin?> = selector(event).right()
    }
}
