package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.annotations.MinixInternal
import dev.racci.minix.api.extension.ExtensionSkeleton
import dev.racci.terix.api.Terix
import kotlinx.collections.immutable.PersistentSet
import kotlinx.coroutines.launch

@OptIn(MinixInternal::class)
public class MotherTicker(
    grandparent: ExtensionSkeleton<Terix>,
    private val children: PersistentSet<ChildTicker>
) : ExtensionSkeleton<Terix> by grandparent {

    public suspend fun run() {
        for (child in children) {
            if (!child.isAlive) {
                when {
                    child.upForAdoption -> child.breakWater()
                    else -> continue
                }
            }

            if (child.player.isDead) {
                continue
            }

            this.supervisor.launch(dispatcher.get()) { child.run() }
        }
    }

    public suspend fun endSuffering() {
        logger.debug { "Ending suffering of ${children.size} children." }
        for (child in children) {
            if (!child.isAlive) continue
            child.coatHanger()
        }
    }
}
