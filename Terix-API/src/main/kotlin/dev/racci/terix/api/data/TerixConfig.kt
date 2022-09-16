package dev.racci.terix.api.data

import dev.racci.minix.api.annotations.MappedConfig
import dev.racci.minix.api.data.MinixConfig
import dev.racci.terix.api.Terix
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@ConfigSerializable
@MappedConfig(Terix::class, "Config.conf")
class TerixConfig : MinixConfig<Terix>(true) {

    @Comment("The players default origin.")
    val defaultOrigin: String = "Human"

    @Comment("Should the player see a title when changing origins?")
    val showTitleOnChange: Boolean = true

    @Comment("How long does the player have to wait between changing their origin again?")
    val intervalBeforeChange: Duration = 360.minutes

    @Comment("How many times should the player be able to change their origin for free?")
    val freeChanges: Int = 3
}
