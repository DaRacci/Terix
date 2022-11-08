package dev.racci.terix.api.annotations

import dev.racci.terix.api.origins.enums.EventSelector
import dev.racci.terix.api.origins.origin.Origin
import org.bukkit.event.EventPriority

/**
 * Allows custom functions to be annotated inside [Origin] classes.
 *
 * @property selector What target to select from the event.
 * @property priority The priority of the event.
 * @property ignoreCancelled Whether to ignore cancelled events.
 */
// TODO -> CompileTime annotation processor
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
public annotation class OriginEventSelector(
    val selector: EventSelector,
    val priority: EventPriority = EventPriority.HIGHEST,
    val ignoreCancelled: Boolean = true
)
