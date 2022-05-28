package dev.racci.terix.core.services.runnables

import dev.racci.minix.api.scheduler.CoroutineRunnable

abstract class ChildCoroutineRunnable(val mother: CoroutineRunnable) {

    var isAlive: Boolean = false
    var upForAdoption: Boolean = true

    abstract suspend fun run()

    /** Runs the initialisation / pushing out of the baby. */
    open suspend fun onBirth() {}

    /** Designed for when this task might be reused in the future. */
    open suspend fun onPutUpForAdoption() {}

    /** Absolutely kills the child, getting rid of that unwanted fetus. */
    open suspend fun onAbortion() {}

    suspend fun breakWater() {
        isAlive = true
        onBirth()
    }

    suspend fun putUpForAdoption() {
        upForAdoption = true
        onPutUpForAdoption()
    }

    suspend fun coatHanger() {
        isAlive = false
        onAbortion()
    }
}
