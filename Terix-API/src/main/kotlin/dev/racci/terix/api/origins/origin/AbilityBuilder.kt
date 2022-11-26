package dev.racci.terix.api.origins.origin

import arrow.core.Option
import arrow.core.toOption
import arrow.optics.lens
import dev.racci.minix.api.extensions.reflection.castOrThrow
import dev.racci.terix.api.data.TemporaryPlacement
import dev.racci.terix.api.extensions.maybeAppend
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.enums.KeyBinding
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
    @PublishedApi internal val keyBinding: Option<KeyBinding>,
    private val abilityKClass: KClass<A>
) {
    @PublishedApi
    internal var generator: OriginValues.AbilityGenerator<A> = OriginValues.AbilityGenerator(keyBinding, abilityKClass, {}, emptyArray())

    // FIXME: WHY DOESNT THIS INFER THE CORRECT VALUE TYPE
    @OptIn(ExperimentalTypeInference::class)
    public fun <P : KProperty1<A, T>, T> parameter(
        parameter: P,
        @BuilderInference value: T
    ): AbilityBuilder<A> {
        generator = OriginValues.AbilityGenerator<A>::additionalConstructorParams.lens.modify(generator) { current -> arrayOf(*current, Pair(parameter, value)) }
        return this
    }

    public fun configure(
        configure: A.() -> Unit
    ): AbilityBuilder<A> {
        generator = OriginValues.AbilityGenerator<A>::abilityBuilder.lens.modify(generator) { current -> current.maybeAppend(configure) }
        return this
    }

    public fun keybinding(
        keyBinding: KeyBinding
    ): AbilityBuilder<A> {
        AbilityBuilder<A>::keyBinding.lens.modify(this) { keyBinding.toOption() }
        return this
    }
}

public inline fun <reified A : KeybindAbility> AbilityBuilder<A>.cooldown(duration: Duration): AbilityBuilder<A> {
    return parameter(A::class.get(KeybindAbility::cooldown), duration)
}

public inline fun <reified A> AbilityBuilder<A>.placementRadius(radius: Double): AbilityBuilder<A> where A : Ability, A : TemporaryPlacement.RadiusLimited {
    return parameter(A::class.get(TemporaryPlacement.RadiusLimited::placementRadius), radius)
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
    return this.declaredMemberProperties.first { it.name == superProperty.name && it.returnType.classifier == superProperty.returnType.classifier }.castOrThrow()
}
