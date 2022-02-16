plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.copyjar")
    id("dev.racci.minix.testing")
    id("dev.racci.minix.purpurmc")
    kotlin("plugin.serialization")
    id("dev.racci.minix.publication")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.18"
    version = rootProject.version.toString()
    main = "dev.racci.terix.core.TerixImpl"
    load = net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder.STARTUP
    depend = listOf(
        "Minix",
        "ProtocolLib"
    )
    softDepend = listOf(
        "PlaceholderAPI",
        "Lands",
    )
    website = "https://minix.racci.dev/"
}

dependencies {
    implementation(project(":Terix-Core"))
}

subprojects {

    apply(plugin = "dev.racci.minix.kotlin")
    apply(plugin = "dev.racci.minix.purpurmc")
    apply(plugin = "kotlinx-serialization")

    dependencies {
        compileOnly("dev.racci:Minix:1.0.5")
        compileOnly("org.purpurmc.purpur:purpur-api:1.18.1-R0.1-SNAPSHOT")

        // Kotlin
        compileOnly(rootProject.libs.kotlin.stdlib)
        compileOnly(rootProject.libs.kotlin.reflect)

        // KotlinX
        compileOnly(rootProject.libs.kotlinx.dateTime)
        compileOnly(rootProject.libs.kotlinx.coroutines)
        compileOnly(rootProject.libs.kotlinx.serialization.kaml)
        compileOnly(rootProject.libs.kotlinx.immutableCollections)

        // Minecraft Libs
        compileOnly(rootProject.libs.adventure.kotlin)
        compileOnly(rootProject.libs.adventure.minimessage)

        // Extras
        compileOnly(rootProject.libs.koin.core)
    }
}

allprojects {
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.racci.dev/releases")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    dependencies {
        compileOnly(rootProject.libs.kotlin.stdlib)
    }

    ktlint {
        enableExperimentalRules.set(false)
    }

    tasks.shadowJar {
        val location = "dev.racci.terix.libs"
        dependencyFilter.exclude {
            it.moduleGroup == "org.jetbrains.kotlin" ||
                it.moduleGroup == "org.jetbrains.intellij" ||
                it.moduleGroup == "org.jetbrains" ||
                it.moduleName == "adventure-api" ||
                it.moduleName == "adventure-text-serializer-*" ||
                it.moduleName == "adventure-key" ||
                it.moduleName == "examination-*"
        }
        relocate("dev.jorel.commandapi", "$location.commandapi")
        relocate("net.kyori.adventure.text.minimessage", "dev.racci.minix.libs.kyori.minimessage")
        relocate("net.kyori.adventure.text.extra.kotlin", "dev.racci.minix.libs.kyroi.kotlin")
        relocate("org.valiktor", "$location.valiktor")
        relocate("dev.racci.minix.nms", "$location.minix-nms")
    }
}
