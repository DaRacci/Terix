@file:Suppress("UnusedReceiverParameter")

package dev.racci.terix.api.extensions

public fun Unit.emptyLambdaNone(): () -> Unit = { }

public fun Unit.truePredicateNone(): () -> Boolean = { true }

public fun Unit.falsePredicateNone(): () -> Boolean = { false }

public fun <O> Unit.emptyLambdaOne(): (O) -> Unit = { }

public fun <O> Unit.truePredicateOne(): (O) -> Boolean = { true }

public fun <O> Unit.falsePredicateOne(): (O) -> Boolean = { false }

public fun <A, B> Unit.emptyLambdaTwo(): (A, B) -> Unit = { _, _ -> }

public fun <A, B> Unit.truePredicateTwo(): (A, B) -> Boolean = { _, _ -> true }

public fun <A, B> Unit.falsePredicateTwo(): (A, B) -> Boolean = { _, _ -> false }

public fun <A, B, C> Unit.emptyLambdaThree(): (A, B, C) -> Unit = { _, _, _ -> }

public fun <A, B, C> Unit.truePredicateThree(): (A, B, C) -> Boolean = { _, _, _ -> true }

public fun <A, B, C> Unit.falsePredicateThree(): (A, B, C) -> Boolean = { _, _, _ -> false }
