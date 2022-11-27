package dev.racci.terix.api.origins.origin.builder

import arrow.core.None
import arrow.core.Option
import dev.racci.terix.api.data.OriginNamespacedTag
import dev.racci.terix.api.data.OriginNamespacedTag.Companion.applyTag
import dev.racci.terix.api.dsl.DSLMutator
import dev.racci.terix.api.dsl.PotionEffectBuilder
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State

public class PotionBuilderPart internal constructor() : BuilderPart<PotionBuilderPart.PotionElement>() {

    /**
     * Adds a [PotionEffect] which will be granted to the player while this state is active.
     *
     * @receiver The [State] which will grant the potion.
     * @param mutator A [DSLMutator] which will be used to configure the [PotionEffect].
     */
    public operator fun State.plusAssign(mutator: DSLMutator<PotionEffectBuilder>) {
        PotionElement(
            this,
            mutator.asNew()
        ).also(::addElement)
    }

    /**
     * Adds a [PotionEffect] which will be granted to the player for these states.
     *
     * @receiver The Collection of [State]'s which will grant the potion.
     * @param mutator A [DSLMutator] which will be used to configure the [PotionEffect].
     */
    public operator fun Collection<State>.plusAssign(mutator: DSLMutator<PotionEffectBuilder>) {
        this.forEach { state ->
            PotionElement(
                state,
                mutator.asNew()
            ).also(::addElement)
        }
    }

    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        super.getElements().associate { it.state to it.builder }
            .onEach { (state, builder) -> builder.applyTag(OriginNamespacedTag.baseStateOf(originValues, state)) }
            .forEach { (state, builder) -> originValues.stateData = originValues.stateData.modify(state, OriginValues.StateData::potions) { potions -> potions.add(builder) } }

        return None
    }

    public data class PotionElement internal constructor(
        val state: State,
        val builder: PotionEffectBuilder
    )
}
