package dev.racci.terix.api.origins.states

sealed class SideEffect {
    open val name = this::class.simpleName ?: throw IllegalStateException("Anonymous classes aren't supported.")

    object WET : SideEffect()
    object DRY : SideEffect()

    sealed class Temperature : SideEffect() {
        object HOT : Temperature()
        object COLD : Temperature()
        object BURNING : SideEffect()
        object FREEZING : Temperature()
    }
}
