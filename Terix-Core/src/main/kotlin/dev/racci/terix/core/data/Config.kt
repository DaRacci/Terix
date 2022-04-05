package dev.racci.terix.core.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.terix.api.Terix
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
@MappedConfig(Terix::class, "Config.conf")
class Config {

    @Comment("Should more verbose logging be sent to the server console?")
    val debug: Boolean = false

    @Comment("The players default origin.")
    val defaultOrigin: String = "Human"

    @Comment("Should the player see a title when changing origins?")
    val showTitleOnChange: Boolean = true

    @Comment("How long does the player have to wait between changing their origin again?")
    val intervalBeforeChange: Duration = 360.minutes

    @Comment("How many times should the player be able to change their origin for free?")
    val freeChanges: Int = 3
}
