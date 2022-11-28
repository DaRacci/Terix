package dev.racci.terix.api.data.player

import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant

public sealed interface TerixPlayerEntity {
    public var lastChosenTime: Instant
    public var freeChanges: Int
    public val grants: MutableSet<String>
    public var origin: Origin
}
