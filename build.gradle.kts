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
    apiVersion = "1.19"
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
        "dev.jorel:commandapi-shade:8.5.1",
        "xyz.xenondevs:particle:1.7.1"
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

    apply(plugin = "dev.racci.minix.kotlin")
    apply(plugin = "dev.racci.minix.purpurmc")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.dokka")

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.racci.dev/releases")
        maven("https://repo.racci.dev/snapshots/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    dependencies {
        compileOnly("dev.racci:Minix-NMS:$minixVersion")
        compileOnly(rootProject.libs.minecraft.minix)
        compileOnly(rootProject.libs.minecraft.minix.core)

        testImplementation(platform(kotlin("bom")))
        testImplementation("dev.racci:Minix-NMS:$minixVersion")
        testImplementation(rootProject.libs.minecraft.minix)
        testImplementation(rootProject.libs.minecraft.minix.core)
        testImplementation(rootProject.libs.bundles.kotlin)
        testImplementation(rootProject.libs.bundles.kotlinx)
        testImplementation(rootProject.libs.bundles.testing)
        testImplementation(rootProject.libs.minecraft.bstats)
        testImplementation("io.insert-koin:koin-test:3.+")
        testImplementation("io.insert-koin:koin-test-junit5:3.+")
        testImplementation("io.mockk:mockk:1.12.4")
        testImplementation("dev.racci:Minix-NMS:$minixVersion")
        testImplementation(rootProject.libs.minecraft.api.protoclLib)
        testImplementation(rootProject.libs.minecraft.api.placeholderAPI)
    }

    configurations {
        testImplementation.get().exclude("org.jetbrains.kotlin", "kotlin-test-junit")
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

//    withType<ShadowJar> {
//        val location = "dev.racci.terix.libs"
//        relocate("com.github.retrooper", "$location.packetevents")
//        relocate("com.github.stefvanschie.inventoryframework", "$location.inventoryframework")
//        relocate("dev.racci.minix.nms", "$location.minix-nms")
//    }

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
