import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URL

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.copyjar")
    id("dev.racci.minix.purpurmc")
    id("dev.racci.minix.nms")
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
    depend = listOf("Minix")
    softDepend = listOf(
        "PlaceholderAPI",
        "Lands",
        "EcoEnchants",
        "ProtocolLib"
    )
    libraries = listOf(
        libs.minecraft.commandAPI.get().toString()
    )
    website = "https://terix.racci.dev/"
}

val minixVersion: String by project
dependencies {
    implementation(project(":Terix-Core"))
    implementation(project(":Terix-API"))
    implementation(libs.minecraft.inventoryFramework)
    implementation("dev.racci:Minix-NMS:$minixVersion")
}

subprojects {

    val serverVersion: String by rootProject

    apply(plugin = "dev.racci.minix.kotlin")
    apply(plugin = "dev.racci.minix.purpurmc")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.racci.dev/releases")
        maven("https://repo.racci.dev/snapshots/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    dependencies {
        compileOnly("dev.racci:Minix:2.10.1")
        compileOnly("dev.racci:Minix-Core:2.10.1")

        testImplementation(platform(kotlin("bom")))
        testImplementation(rootProject.libs.minecraft.minix)
        testImplementation(rootProject.libs.bundles.kotlin)
        testImplementation(rootProject.libs.bundles.kotlinx)
//        testImplementation(rootProject.libs.bundles.testing) {
//            exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit5")
//        }
        testImplementation("dev.racci.tentacles:tentacles-api:$serverVersion")
    }

    tasks {

        test.get().useJUnitPlatform()

        dokkaHtml.get().dokkaSourceSets.configureEach {
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            displayName.set(project.name.split("-").last())
            platform.set(org.jetbrains.dokka.Platform.jvm)
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/DaRacci/Terix/blob/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
            jdkVersion.set(17)
            externalDocumentationLink {
                url.set(URL("https://terix.racci.dev/"))
            }
        }
    }
}

fun included(
    build: String,
    task: String
) = gradle.includedBuild(build).task(task)

tasks {

    withType<ShadowJar> {
        val location = "dev.racci.terix.libs"
        relocate("com.github.retrooper", "$location.packetevents")
        relocate("com.github.stefvanschie.inventoryframework", "$location.inventoryframework")
        relocate("dev.racci.minix.nms", "$location.minix-nms")
    }

    ktlintFormat {
        dependsOn(gradle.includedBuilds.map { it.task(":ktlintFormat") })
    }

    build {
        dependsOn(gradle.includedBuilds.map { it.task(":build") })
    }

    clean {
        dependsOn(gradle.includedBuilds.map { it.task(":clean") })
    }

    withType<org.jetbrains.dokka.gradle.DokkaMultiModuleTask> {
        outputDirectory.set(File("$rootDir/docs"))
    }
}
