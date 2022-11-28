package dev.racci.terix.api.data.player

import dev.racci.terix.api.origins.origin.Origin
import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

public class TerixPlayerEntityHolder(
    id: EntityID<UUID>
) : TerixPlayerEntity, UUIDEntity(id) {
    public override var lastChosenTime: Instant by TerixUser.lastChosenTime
    public override var freeChanges: Int by TerixUser.freeChanges
    public override val grants: MutableSet<String> by TerixUser.grants
    public override var origin: Origin by TerixUser.origin

    override fun toString(): String = buildString {
        append("TerixPlayerEntityHolder(")
        append("id=$id, ")
        append("lastChosenTime=$lastChosenTime, ")
        append("freeChanges=$freeChanges, ")
        append("grants=$grants, ")
        append("origin=$origin")
        append(")")
    }

    public companion object : UUIDEntityClass<TerixPlayerEntityHolder>(TerixUser, TerixPlayerEntityHolder::class.java, ::TerixPlayerEntityHolder)
}
