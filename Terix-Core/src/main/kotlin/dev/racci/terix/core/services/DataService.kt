package dev.racci.terix.core.services

import dev.racci.minix.api.extension.Extension
import dev.racci.minix.api.serializables.DurationSerializer
import dev.racci.terix.api.Terix
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class DataService(override val plugin: Terix) : Extension<Terix>() {

    override val name = "Data Service"

    override suspend fun handleEnable() {
        TODO("Not yet implemented")
    }

    override suspend fun handleUnload() {
        TODO("Not yet implemented")
    }

    @Serializable
    data class Config(
        val origins: OriginConfig = OriginConfig(),
        val version: Int = 1
    ) {
        @Serializable
        data class OriginConfig(
            val defaultOrigin: String = "Human",
            val showTitleOnChange: Boolean = true,
            val intervalBeforeChange: @Serializable(with = DurationSerializer::class) Duration = 360.minutes,
        )
    }
}
