import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder
import org.jetbrains.dokka.gradle.DokkaPlugin
import java.net.URL

// Workaround for (https://youtrack.jetbrains.com/issue/KTIJ-19369)
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minix.nms)
    alias(libs.plugins.minix.kotlin)
    alias(libs.plugins.minix.copyjar)
    alias(libs.plugins.minix.purpurmc)

    alias(libs.plugins.minecraft.pluginYML)
    alias(libs.plugins.dokka)

    alias(libs.plugins.kotlin.atomicfu)
    alias(libs.plugins.slimjar)
}

bukkit {
    name = project.name
    prefix = project.name
    author = "Racci"
    apiVersion = "1.19"
    version = rootProject.version.toString()
    main = "dev.racci.terix.core.TerixImpl"
    load = PluginLoadOrder.POSTWORLD
    depend = listOf("Minix")
    softDepend = listOf(
        "PlaceholderAPI",
        "Lands",
        "EcoEnchants",
        "ProtocolLib",
        "LibsDisguises"
    )
    permissions {
        register("terix.selection.bypass-cooldown") {
            description = "Allows bypassing the cooldown for changing origins."
            default = Default.OP
        }

        register("terix.command.origin.get") {
            description = "Allows using the /origin get command."
            default = Default.OP
        }

        register("terix.command.origin.set") {
            description = "Allows using the /origin set command."
            default = Default.OP
        }

        register("terix.menu") {
            description = "Allows using the /origin menu command."
            default = Default.TRUE
        }
    }
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

    slim("com.frengor:ultimateadvancementapi-shadeable:2.2.1")
    slim(libs.minecraft.particles)
}

subprojects {
    apply<Dev_racci_minix_nmsPlugin>()
    apply<Dev_racci_minix_kotlinPlugin>()
    apply<Dev_racci_minix_purpurmcPlugin>()
    apply<DokkaPlugin>()

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
        compileOnly(rootProject.libs.minecraft.minix)
        compileOnly(rootProject.libs.minecraft.minix.core)
        compileOnly(rootProject.libs.minecraft.api.libsDisguises) { exclude("org.spigotmc", "spigot") }

        compileOnly(platform("io.arrow-kt:arrow-stack:1.1.3"))
        compileOnly("io.arrow-kt:arrow-core")
        compileOnly("io.arrow-kt:arrow-fx-coroutines")

        testImplementation(platform(kotlin("bom")))
        testImplementation(rootProject.libs.minecraft.minix)
        testImplementation(rootProject.libs.minecraft.minix.core)
        testImplementation(rootProject.libs.bundles.kotlin)
        testImplementation(rootProject.libs.bundles.kotlinx)
        testImplementation(rootProject.libs.bundles.testing)
        testImplementation(rootProject.libs.minecraft.bstats.base)
        testImplementation(rootProject.libs.koin.test)
        testImplementation(rootProject.libs.koin.test.junit5)
        testImplementation("dev.racci:Minix-NMS:$minixVersion")
        testImplementation(rootProject.libs.minecraft.api.protoclLib)
        testImplementation(rootProject.libs.minecraft.api.placeholderAPI)
    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    configurations {
        all {
            exclude("org.jetbrains.kotlin", "kotlin-test-junit")
            exclude("org.spigotmc", "spigot")
            exclude("io.papermc.paper", "paper-api")
        }
    }

    tasks {

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

tasks {

    shadowJar {
        dependencyFilter.include { dep ->
            dep.moduleName == "Terix-API" ||
                dep.moduleName == "Terix-Core" ||
                dep.moduleGroup == "org.incendo.interfaces"
        }

        relocate("org.incendo.interfaces", "dev.racci.terix.libs.interfaces")
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

    kotlin {
        explicitApiWarning()
    }
}
