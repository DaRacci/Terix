package dev.racci.terix.api.extensions // ktlint-disable filename

import dev.racci.minix.api.utils.kotlin.ifFalse
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

@OptIn(ExperimentalContracts::class)
public inline fun <E : Event> E.onFailure(action: (event: E) -> Unit): Boolean {
    contract {
        callsInPlace(action, InvocationKind.AT_MOST_ONCE)
    }

    return this.callEvent().ifFalse { action(this) }
}

@OptIn(ExperimentalContracts::class)
public inline fun <E : Event> E.fold(
    onSuccess: (event: E) -> Unit,
    onFailure: (event: E) -> Unit
): Boolean {
    contract {
        callsInPlace(onSuccess, InvocationKind.AT_MOST_ONCE)
        callsInPlace(onFailure, InvocationKind.AT_MOST_ONCE)
    }

    val result = this.callEvent()

    when (result) {
        true -> onSuccess(this)
        false -> onFailure(this)
    }

    return result
}
