@file:Suppress("UnusedReceiverParameter")

package dev.racci.terix.api.extensions

public fun Unit.emptyLambdaNone(): () -> Unit = { }

public fun <O> Unit.emptyLambdaOne(): (O) -> Unit = { }

public fun <A, B> Unit.emptyLambdaTwo(): (A, B) -> Unit = { _, _ -> }

public fun <A, B, C> Unit.emptyLambdaThree(): (A, B, C) -> Unit = { _, _, _ -> }
