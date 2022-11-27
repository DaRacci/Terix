package dev.racci.terix.api.origins.origin

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import dev.racci.minix.api.annotations.MinixDsl
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.origins.OriginItem
import dev.racci.terix.api.origins.origin.builder.AbilityBuilderPart
import dev.racci.terix.api.origins.origin.builder.AttributeBuilderPart
import dev.racci.terix.api.origins.origin.builder.BuilderPart
import dev.racci.terix.api.origins.origin.builder.DamageBuilderPart
import dev.racci.terix.api.origins.origin.builder.FoodBuilderPart
import dev.racci.terix.api.origins.origin.builder.PotionBuilderPart
import dev.racci.terix.api.origins.origin.builder.TimeTitleBuilderPart
import org.apiguardian.api.API
import org.bukkit.entity.Player
import kotlin.reflect.KClass

/** Handles the origins primary variables. */
// TODO -> I don't like the current DSL style.
@API(status = API.Status.STABLE, since = "1.0.0")
public sealed class OriginBuilder : OriginValues() {

    @API(status = API.Status.INTERNAL)
    public val builderCache: LoadingCache<KClass<out BuilderPart<*>>, BuilderPart<*>> = Caffeine.newBuilder().build { kClass -> kClass.constructors.first().call() }

    private inline fun <reified T : BuilderPart<*>> builder(): T = builderCache[T::class].castOrThrow()

    public fun fireImmunity(fireImmunity: Boolean = true): OriginBuilder {
        this.fireImmunity = fireImmunity
        return this
    }

    public fun waterBreathing(waterBreathing: Boolean = true): OriginBuilder {
        this.waterBreathing = waterBreathing
        return this
    }

    @MinixDsl
    protected suspend fun potions(builder: suspend PotionBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun attributes(builder: suspend AttributeBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun title(builder: suspend TimeTitleBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun damage(builder: suspend DamageBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun food(builder: suspend FoodBuilderPart.() -> Unit) {
        builder(builder())
    }

    @MinixDsl
    protected suspend fun item(builder: suspend OriginItem.() -> Unit) {
        builder(item)
    }

    @MinixDsl
    protected suspend fun abilities(builder: suspend AbilityBuilderPart.() -> Unit) {
        builder(builder())
    }
}

public typealias PlayerLambda = suspend (player: Player) -> Unit
