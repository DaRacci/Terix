package dev.racci.terix.api.annotations

/**
 * Provides context of what dispatcher is used to call this function.
 */
@Target(AnnotationTarget.FUNCTION)
public annotation class DispatcherContext(val context: Context) {

    public enum class Context {
        MAIN,
        ASYNC;
    }
}