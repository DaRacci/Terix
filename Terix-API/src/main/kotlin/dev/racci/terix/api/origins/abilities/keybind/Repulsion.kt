package dev.racci.terix.api.origins.abilities.keybind

import com.google.common.base.Predicates
import dev.racci.terix.api.data.player.TerixPlayer
import dev.racci.terix.api.extensions.emptyLambdaOne
import dev.racci.terix.api.extensions.truePredicateOne
import dev.racci.terix.api.origins.origin.Origin
import net.minecraft.world.entity.Entity
import org.bukkit.entity.LivingEntity
import kotlin.time.Duration

public class Repulsion(
    override val abilityPlayer: TerixPlayer,
    override val linkedOrigin: Origin,
    override val cooldownDuration: Duration = Duration.ZERO
) : TriggeringKeybindAbility() {

    public var radius: Double = 15.0
    public var strengthMultiplier: Double = 1.0
    public var repulsePredicate: (targetEntity: LivingEntity) -> Boolean = Unit.truePredicateOne()
    public var onRepulsion: (targetEntity: LivingEntity) -> Unit = Unit.emptyLambdaOne()

    // Pull target at defined speed until the entity is within the defined radius of the player
    override suspend fun handleTrigger() {
        val nmsPlayer = abilityPlayer.handle
        nmsPlayer.level.getEntities(
            nmsPlayer,
            nmsPlayer.boundingBox.inflate(radius),
            Predicates.alwaysTrue<Entity>()
        ).asSequence()
            .map { it.bukkitEntity }
            .filter { entity -> /* TODO: Is Friendly check */ true }
            .filterIsInstance<LivingEntity>()
            .filter { entity -> repulsePredicate(entity) }.toList()
            .forEach { entity ->
                val baseVector = entity.location.toVector().subtract(abilityPlayer.location.toVector())
                val length = baseVector.length()
                val strength = length * strengthMultiplier

                entity.velocity = entity.velocity.add(baseVector.normalize().multiply(-strengthMultiplier))
            }
    }
}
