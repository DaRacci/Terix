package dev.racci.terix.api.origins.origin

import arrow.core.None
import arrow.core.toOption
import arrow.optics.lens
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.data.placement.TemporaryPlacement
import dev.racci.terix.api.extensions.maybeAppend
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.OriginValues.AbilityGenerator
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import kotlin.experimental.ExperimentalTypeInference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.time.Duration

// TODO -> Add more documentation
// TODO -> More DSL Friendly
public data class AbilityBuilder<A : Ability> @PublishedApi internal constructor(
    @PublishedApi internal var generator: AbilityGenerator<A>
) {
    // FIXME: WHY DOESNT THIS INFER THE CORRECT VALUE TYPE
    @OptIn(ExperimentalTypeInference::class)
    public fun <P : KProperty1<A, T>, T> parameter(
        parameter: P,
        @BuilderInference value: T
    ): AbilityBuilder<A> = this.copy(
        generator = AbilityGenerator<A>::additionalConstructorParams.lens.modify(generator) { current -> arrayOf(*current, Pair(parameter, value)) }
    )

    public fun configure(
        configure: A.() -> Unit
    ): AbilityBuilder<A> = this.copy(
        generator = AbilityGenerator<A>::abilityBuilder.lens.modify(generator) { current -> current.maybeAppend(configure) }
    )

    @PublishedApi internal companion object {
        inline fun <reified A : Ability> of(): AbilityBuilder<A> = AbilityBuilder(AbilityGenerator(None, A::class, {}, emptyArray()))
        fun <A : Ability> of(abilityKClass: KClass<A>): AbilityBuilder<A> = AbilityBuilder(AbilityGenerator(None, abilityKClass, {}, emptyArray()))
    }
}

public fun <A : KeybindAbility> AbilityBuilder<A>.keybinding(
    keyBinding: KeyBinding
): AbilityBuilder<A> = this.copy(
    generator = AbilityGenerator<A>::keybinding.lens.modify(generator) { keyBinding.toOption() }
)

public inline fun <reified A : KeybindAbility> AbilityBuilder<A>.cooldown(duration: Duration): AbilityBuilder<A> {
    return parameter(A::class.get(KeybindAbility::cooldownDuration), duration)
}

public inline fun <reified A> AbilityBuilder<A>.placementProvider(provider: Material): AbilityBuilder<A> where A : Ability, A : TemporaryPlacement.BlockDataProvider {
    return parameter(A::class.get(TemporaryPlacement.BlockDataProvider::placementData), provider.createBlockData())
}

public inline fun <reified A> AbilityBuilder<A>.placementProvider(provider: BlockData): AbilityBuilder<A> where A : Ability, A : TemporaryPlacement.BlockDataProvider {
    return parameter(A::class.get(TemporaryPlacement.BlockDataProvider::placementData), provider)
}

public inline fun <reified A> AbilityBuilder<A>.placementDuration(duration: Duration): AbilityBuilder<A> where A : Ability, A : TemporaryPlacement.DurationLimited {
    return parameter(A::class.get(TemporaryPlacement.DurationLimited::placementDuration), duration)
}

@PublishedApi
internal inline fun <reified S, I, T> KClass<S>.get(superProperty: KProperty1<I, T>): KProperty1<S, T> where I : Any, S : I {
    return this.declaredMemberProperties.find { property -> property.name == superProperty.name }.castOrThrow()
}
