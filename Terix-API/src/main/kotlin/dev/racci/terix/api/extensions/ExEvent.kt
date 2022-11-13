package dev.racci.terix.api.extensions // ktlint-disable filename

import dev.racci.minix.api.utils.kotlin.ifTrue
import org.bukkit.event.Event
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
public inline fun <E : Event> E.onSuccess(action: (event: E) -> Unit): Boolean {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    return this.callEvent().ifTrue { action(this) }
}
