package dev.racci.terix.api.origins.origin

import org.apiguardian.api.API
import org.bukkit.entity.Player
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties

// TODO -> Player levels
// TODO -> Per player instances of origins
// TODO -> Potion modifier system (eg preventing potion effects and modifying them)
// TODO -> Attribute modifier system (eg preventing attribute modifiers and modifying them)
// TODO -> Internal tracking UID
// TODO -> Templating data class
@API(status = API.Status.MAINTAINED, since = "1.0.0")
public abstract class Origin : OriginBuilder() {

    /** Called when Terix first registers this origin. */
    public open suspend fun handleRegister(): Unit = Unit

    /**
     * Called when the player loads|joins|respawns as this origin.
     * Called after [handleBecomeOrigin] if the player is becoming this origin.
     */
    public open suspend fun handleLoad(player: Player): Unit = Unit

    /** Called when the server is stopping or plugin is being unloaded and caches should be cleaned. */
    public open suspend fun handleUnload(): Unit = Unit

    /**
     * Called when the player first becomes this origin.
     * Called before [handleLoad].
     */
    public open suspend fun handleBecomeOrigin(player: Player): Unit = Unit

    /** Called when the player changes from this origin. */
    public open suspend fun handleChangeOrigin(player: Player): Unit = Unit

    /** When the player changes to gm 1 for example. */
    public open suspend fun handleDeactivate(player: Player): Unit = Unit

    /** Called each game tick. */
    public open suspend fun onTick(player: Player): Unit = Unit

    final override fun toString(): String = buildString {
        fun appender(property: KProperty1<OriginValues, *>) {
            if (last() != '(') append(", ")
            append(property.name)
            append("='")
            append(property.get(this@Origin))
            append('\'')
        }

        append("Origin(")

        OriginValues::class.declaredMemberProperties
            .filter { it.annotations.isEmpty() }
            .forEach { prop -> appender(prop) }

        append(')')
    }
}
