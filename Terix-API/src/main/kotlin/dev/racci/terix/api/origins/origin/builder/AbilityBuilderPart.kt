package dev.racci.terix.api.origins.origin.builder

import arrow.core.None
import arrow.core.Option
import arrow.optics.lens
import dev.racci.terix.api.origins.abilities.Ability
import dev.racci.terix.api.origins.abilities.keybind.KeybindAbility
import dev.racci.terix.api.origins.abilities.passive.PassiveAbility
import dev.racci.terix.api.origins.enums.KeyBinding
import dev.racci.terix.api.origins.origin.AbilityBuilder
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.origin.keybinding
import kotlin.reflect.KProperty1

public class AbilityBuilderPart internal constructor() : BuilderPart<OriginValues.AbilityGenerator<out Ability>>() {

    /**
     * Adds a keybinding bound ability that is granted with this origin.
     *
     * @receiver The keybinding to bind the ability to.
     * @param A The type of ability to add.
     * @param configure A builder function to configure the ability on creation.
     */
    public inline fun <reified A : KeybindAbility> KeyBinding.add(
        vararg constructorParams: Pair<KProperty1<A, *>, *>,
        noinline configure: A.() -> Unit = {}
    ): Unit = newBuilder<A>()
        .keybinding(this)
        .configure(configure)
        .apply { constructorParams.forEach { (property, value) -> parameter(property, value) } }
        .build()

    /**
     * Adds a passive ability that is granted with this origin.
     *
     * @param A The type of ability to add.
     * @param configure A builder function to configure the ability on creation.
     */
    public inline fun <reified A : PassiveAbility> withPassive(
        vararg constructorParams: Pair<KProperty1<A, *>, *>,
        noinline configure: A.() -> Unit = {}
    ): Unit = newBuilder<A>()
        .configure(configure)
        .apply { constructorParams.forEach { (property, value) -> parameter(property, value) } }
        .build()

    /**
     * Creates a new ability builder.
     *
     * @param A The reified type of ability to add.
     */
    public inline fun <reified A : Ability> newBuilder(): AbilityBuilder<A> = AbilityBuilder.of()

    /**
     * Completes the ability builder and adds the ability to the origin.
     *
     * @receiver The ability builder to complete.
     */
    public fun <A : Ability> AbilityBuilder<in A>.build() {
        this.generator.copy().also(::addElement)
    }

    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        originValues.abilityData = OriginValues.AbilityData::generators.lens.modify(originValues.abilityData) { generators ->
            generators.builder().also { set -> set.addAll(super.getElements()) }.build()
        }
        return None
    }
}
