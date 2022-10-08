import java.net.URL

plugins {
    id("dev.racci.minix.kotlin")
    id("dev.racci.minix.copyjar")
    id("dev.racci.minix.purpurmc")
    id("dev.racci.minix.nms")
    kotlin("plugin.serialization")
//    kotlin("kapt") version "1.7.10"
    id("dev.racci.minix.publication")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
//    id("io.arrow-kt.analysis.kotlin") version "2.0"
    id("dev.racci.slimjar") version "1.3.3"
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
    website = "https://terix.racci.dev/"
}

tasks {
    val quickBuild by creating {
        this.group = "build"
        dependsOn(compileKotlin)
        dependsOn(shadowJar)
        dependsOn(reobfJar)
        findByName("copyJar")?.let { dependsOn(it) }
    }
}

val minixVersion: String by project
dependencies {
    implementation(project(":Terix-Core"))
    implementation(project(":Terix-API"))

    slim(libs.minecraft.inventoryFramework)
    slim("com.frengor:ultimateadvancementapi-shadeable:2.2.1")
}

subprojects {

    apply(plugin = "dev.racci.minix.kotlin")
    apply(plugin = "dev.racci.minix.purpurmc")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "org.jetbrains.dokka")
//    apply(plugin = "kotlin-kapt")
//    apply(plugin = "io.arrow-kt.analysis.kotlin")

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
        maven("https://repo.racci.dev/releases")
        maven("https://repo.racci.dev/snapshots/")
        maven("https://repo.md-5.net/content/groups/public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    dependencies {
//        compileOnly("dev.racci:Minix:4.2.0-SNAPSHOT")
//        compileOnly("dev.racci:Minix-Core:4.2.0-SNAPSHOT")
        compileOnly(rootProject.libs.minecraft.minix)
        compileOnly(rootProject.libs.minecraft.minix.core)
        compileOnly(rootProject.libs.minecraft.api.libsDisguises)

        compileOnly(platform("io.arrow-kt:arrow-stack:1.1.3"))
        compileOnly("io.arrow-kt:arrow-core")
        compileOnly("io.arrow-kt:arrow-fx-coroutines")
//        kapt("io.arrow-kt:arrow-meta")

        testImplementation(platform(kotlin("bom")))
        testImplementation(rootProject.libs.minecraft.minix)
        testImplementation(rootProject.libs.minecraft.minix.core)
        testImplementation(rootProject.libs.bundles.kotlin)
        testImplementation(rootProject.libs.bundles.kotlinx)
        testImplementation(rootProject.libs.bundles.testing)
        testImplementation(rootProject.libs.minecraft.bstats)
        testImplementation(rootProject.libs.koin.test)
        testImplementation(rootProject.libs.koin.test.junit5)
        testImplementation("dev.racci:Minix-NMS:$minixVersion")
        testImplementation(rootProject.libs.minecraft.api.protoclLib)
        testImplementation(rootProject.libs.minecraft.api.placeholderAPI)
    }

    configurations {
        testImplementation.get().exclude("org.jetbrains.kotlin", "kotlin-test-junit")
    }

    tasks {

//        test.get().useJUnitPlatform()

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

    shadowJar {
        dependencyFilter.include { dep ->
            dep.moduleName == "Terix-API" ||
                dep.moduleName == "Terix-Core"
        }
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

allprojects {
    repositories {
        maven("https://nexus.frengor.com/repository/public/")
    }

    configurations.configureEach {
        exclude("me.carleslc.Simple-YAML", "Simple-Configuration")
        exclude("me.carleslc.Simple-YAML", "Simple-Yaml")
        exclude("com.github.technove", "Flare")
    }
}
