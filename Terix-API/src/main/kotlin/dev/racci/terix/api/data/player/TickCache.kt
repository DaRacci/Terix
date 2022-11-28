package dev.racci.terix.api.data.player

import arrow.fx.coroutines.Atomic

public data class TickCache internal constructor(
    public val sunlight: TwoStateCache = TwoStateCache(),
    public val darkness: TwoStateCache = TwoStateCache(),
    public val water: TwoStateCache = TwoStateCache(),
    public val rain: TwoStateCache = TwoStateCache()
) {
    public open class TwoStateCache internal constructor() {
        protected open val last: Atomic<Boolean> = Atomic.unsafe(false)
        protected open val current: Atomic<Boolean> = Atomic.unsafe(false)

        public suspend fun update(f: (value: Boolean) -> Boolean) {
            val newLast = current.getAndUpdate(f)
            last.set(newLast)
        }

        public suspend fun last(): Boolean = last.get()

        public suspend fun current(): Boolean = current.get()
    }
}
