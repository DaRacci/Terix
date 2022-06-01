package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.scheduler.CoroutineRunnable
import dev.racci.terix.api.Terix
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class MotherCoroutineRunnable : CoroutineRunnable(), KoinComponent {
    val children: LinkedHashSet<ChildCoroutineRunnable> = LinkedHashSet()

    private val handler = CoroutineExceptionHandler { _, throwable ->
        get<Terix>().log.error(throwable) { "There was an unhandled exception while mother wasn't watch her child run on the road." }
    }

    override suspend fun run() {
        supervisorScope {
            for (child in children) {

                if (!child.isAlive && child.upForAdoption) {
                    runBlocking(handler) { child.breakWater() }
                }

                if (child.isAlive) {
                    launch(handler) { child.run() }
                }
            }
        }
    }
}
