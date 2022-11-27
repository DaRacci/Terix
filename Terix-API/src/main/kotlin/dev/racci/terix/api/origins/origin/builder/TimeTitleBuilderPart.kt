package dev.racci.terix.api.origins.origin.builder

import arrow.core.None
import arrow.core.Option
import arrow.core.toOption
import dev.racci.terix.api.dsl.DSLMutator
import dev.racci.terix.api.dsl.TitleBuilder
import dev.racci.terix.api.dsl.dslMutator
import dev.racci.terix.api.origins.origin.OriginValues
import dev.racci.terix.api.origins.states.State

public class TimeTitleBuilderPart internal constructor() : BuilderPart<TimeTitleBuilderPart.TimeTitleElement>() {

    /**
     * Displays this title to the player when then given trigger is activated.
     *
     * @param builder The title builder to use.
     * @receiver The trigger to activate the title.
     */
    public operator fun State.plusAssign(builder: TitleBuilder.() -> Unit) {
        TimeTitleElement(
            this,
            dslMutator(builder)
        ).also(::addElement)
    }

    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        super.getElements()
            .associate { it.state to it.builder }
            .forEach { (state, builder) -> originValues.stateData = originValues.stateData.modify(state, OriginValues.StateData::title) { title -> builder.mutateOrNew(title.orNull()).toOption() } }

        return None
    }

    public data class TimeTitleElement internal constructor(
        val state: State,
        val builder: DSLMutator<TitleBuilder>
    )
}
