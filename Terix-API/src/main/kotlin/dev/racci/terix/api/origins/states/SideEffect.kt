package dev.racci.terix.api.origins.states

public sealed class SideEffect {
    public open val name: String = this::class.simpleName ?: throw IllegalStateException("Anonymous classes aren't supported.")

    public object WET : SideEffect()
    public object DRY : SideEffect()

    public sealed class Temperature : SideEffect() {
        public object HOT : Temperature()
        public object COLD : Temperature()
        public object BURNING : SideEffect()
        public object FREEZING : Temperature()
    }
}
