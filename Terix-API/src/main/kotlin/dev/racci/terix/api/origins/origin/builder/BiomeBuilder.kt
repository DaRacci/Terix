package dev.racci.terix.api.origins.origin.builder

import arrow.core.Option
import dev.racci.terix.api.origins.origin.OriginValues

public class BiomeBuilder internal constructor() : BuilderPart<Nothing>() {
    override suspend fun insertInto(originValues: OriginValues): Option<Exception> {
        TODO()
    }
}
