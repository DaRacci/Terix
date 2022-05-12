package dev.racci.terix.api.dsl

import dev.racci.minix.api.plugin.MinixPlugin
import dev.racci.terix.api.origins.AbstractOrigin

class AngelOrigin : AbstractOrigin() {

    override val plugin: MinixPlugin get() = null!!
    override val name: String get() = "Angel"
}
