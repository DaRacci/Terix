package dev.racci.terix.api.extensions // ktlint-disable filename

public typealias Lambda = () -> Unit
public typealias Lambda1<A> = (A) -> Unit
public typealias Lambda2<A, B> = (A, B) -> Unit
public typealias Lambda3<A, B, C> = (A, B, C) -> Unit

public typealias SuspendLambda = suspend () -> Unit
public typealias SuspendLambda1<A> = suspend (A) -> Unit
public typealias SuspendLambda2<A, B> = suspend (A, B) -> Unit
public typealias SuspendLambda3<A, B, C> = suspend (A, B, C) -> Unit

@JvmName("maybeAppend")
public fun Lambda?.maybeAppend(other: Lambda): Lambda {
    if (this == null) return other
    return {
        this()
        other()
    }
}

@JvmName("maybePrepend")
public fun Lambda?.maybePrepend(other: Lambda): Lambda {
    if (this == null) return other
    return {
        other()
        this()
    }
}

@JvmName("maybeAppendSuspend")
public fun SuspendLambda?.maybeAppend(other: SuspendLambda): SuspendLambda {
    if (this == null) return other
    return {
        this()
        other()
    }
}

@JvmName("maybePrependSuspend")
public fun SuspendLambda?.maybePrepend(other: SuspendLambda): SuspendLambda {
    if (this == null) return other
    return {
        other()
        this()
    }
}

@JvmName("maybeAppendOne")
public fun <A> Lambda1<A>?.maybeAppend(other: Lambda1<A>): Lambda1<A> {
    if (this == null) return other
    return { a ->
        this(a)
        other(a)
    }
}

@JvmName("maybePrependOne")
public fun <A> Lambda1<A>?.maybePrepend(other: Lambda1<A>): Lambda1<A> {
    if (this == null) return other
    return { a ->
        other(a)
        this(a)
    }
}

@JvmName("maybeAppendSuspendOne")
public fun <A> SuspendLambda1<A>?.maybeAppend(other: SuspendLambda1<A>): SuspendLambda1<A> {
    if (this == null) return other
    return { a ->
        this(a)
        other(a)
    }
}

@JvmName("maybePrependSuspendOne")
public fun <A> SuspendLambda1<A>?.maybePrepend(other: SuspendLambda1<A>): SuspendLambda1<A> {
    if (this == null) return other
    return { a ->
        other(a)
        this(a)
    }
}

@JvmName("maybeAppendTwo")
public fun <A, B> Lambda2<A, B>?.maybeAppend(other: Lambda2<A, B>): Lambda2<A, B> {
    if (this == null) return other
    return { a, b ->
        this(a, b)
        other(a, b)
    }
}

@JvmName("maybePrependTwo")
public fun <A, B> Lambda2<A, B>?.maybePrepend(other: Lambda2<A, B>): Lambda2<A, B> {
    if (this == null) return other
    return { a, b ->
        other(a, b)
        this(a, b)
    }
}

@JvmName("maybeAppendSuspendTwo")
public fun <A, B> SuspendLambda2<A, B>?.maybeAppend(other: SuspendLambda2<A, B>): SuspendLambda2<A, B> {
    if (this == null) return other
    return { a, b ->
        this(a, b)
        other(a, b)
    }
}

@JvmName("maybePrependSuspendTwo")
public fun <A, B> SuspendLambda2<A, B>?.maybePrepend(other: SuspendLambda2<A, B>): SuspendLambda2<A, B> {
    if (this == null) return other
    return { a, b ->
        other(a, b)
        this(a, b)
    }
}

@JvmName("maybeAppendThree")
public fun <A, B, C> Lambda3<A, B, C>?.maybeAppend(other: Lambda3<A, B, C>): Lambda3<A, B, C> {
    if (this == null) return other
    return { a, b, c ->
        this(a, b, c)
        other(a, b, c)
    }
}

@JvmName("maybePrependThree")
public fun <A, B, C> Lambda3<A, B, C>?.maybePrepend(other: Lambda3<A, B, C>): Lambda3<A, B, C> {
    if (this == null) return other
    return { a, b, c ->
        other(a, b, c)
        this(a, b, c)
    }
}

@JvmName("maybeAppendSuspendThree")
public fun <A, B, C> SuspendLambda3<A, B, C>?.maybeAppend(other: SuspendLambda3<A, B, C>): SuspendLambda3<A, B, C> {
    if (this == null) return other
    return { a, b, c ->
        this(a, b, c)
        other(a, b, c)
    }
}

@JvmName("maybePrependSuspendThree")
public fun <A, B, C> SuspendLambda3<A, B, C>?.maybePrepend(other: SuspendLambda3<A, B, C>): SuspendLambda3<A, B, C> {
    if (this == null) return other
    return { a, b, c ->
        other(a, b, c)
        this(a, b, c)
    }
}
