package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.scheduler.CoroutineRunnable
import dev.racci.minix.api.utils.Closeable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent

class MotherCoroutineRunnable(
    private val supervisorScope: CoroutineScope,
    private val dispatcher: Closeable<ExecutorCoroutineDispatcher>
) : CoroutineRunnable(), KoinComponent {
    val children: LinkedHashSet<ChildCoroutineRunnable> = LinkedHashSet()

    override suspend fun run() {
        for (child in children) {
            if (!child.isAlive) {
                when {
                    child.upForAdoption -> child.breakWater()
                    else -> continue
                }
            }

            supervisorScope.launch(dispatcher.get()) { child.run() }
        }
    }
}
