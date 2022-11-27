package dev.racci.terix.core.integrations

import dev.racci.minix.api.extensions.server
import dev.racci.minix.api.integrations.Integration
import dev.racci.minix.api.utils.kotlin.ifTrue
import dev.racci.terix.core.TerixImpl
import java.net.URL
import java.util.zip.ZipInputStream

public sealed class FileExtractorIntegration : Integration {

    protected abstract fun filterResource(name: String): Boolean

    protected open fun codeSourceLocation(): URL = TerixImpl::class.java.protectionDomain.codeSource.location

    protected fun extractDefaultAssets(forceUpdate: Boolean = plugin.version.isPreRelease): Boolean {
        val src = codeSourceLocation()
        val folder = server.pluginManager.getPlugin(this.pluginName)!!.dataFolder
        val scope = "integrations.$pluginName"
        var filesChanged = false

        runCatching {
            logger.info(scope = scope) { "Extracting default assets..." }
            ZipInputStream(src.openStream()).use { stream ->
                while (true) {
                    val entry = stream.nextEntry ?: break
                    val name = entry.name

                    if (entry.isDirectory || !filterResource(name)) continue

                    val dest = folder.resolve(name)
                    if (!dest.exists() || forceUpdate) {
                        dest.parentFile.mkdirs()
                        dest.createNewFile().ifTrue { filesChanged = true }
                        logger.debug(scope = scope) { "Extracting $name" }
                        plugin.getResource(name)!!.use { input -> dest.outputStream().use(input::copyTo) }
                    }
                }
            }
            logger.info(scope = scope) { "Finished extracting default assets." }
        }.onFailure { err -> logger.error(err, scope) { "Failed to extract default assets." } }

        return filesChanged
    }
}
