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
        compileOnly(rootProject.libs.bundles.kyori)
        compileOnly(rootProject.libs.bundles.kotlin)
        compileOnly(rootProject.libs.bundles.kotlinx)
        compileOnly(rootProject.libs.bundles.exposed)
        compileOnly("dev.racci:Minix:2.0.3")
        compileOnly(rootProject.libs.kotlinx.serialization.kaml)
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

    tasks.shadowJar {
        val location = "dev.racci.terix.libs"
        relocate("com.github.stefvanschie.inventoryframework", "$location.inventoryframework")
        relocate("dev.jorel.commandapi", "$location.commandapi")
        relocate("net.kyori.adventure.extra", "dev.racci.minix.libs.adventure.extra")
        relocate("org.valiktor", "$location.valiktor")
        relocate("dev.racci.minix.nms", "$location.minix-nms")
    }
}
