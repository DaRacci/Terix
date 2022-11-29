import com.google.devtools.ksp.gradle.KspGradleSubplugin
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.PluginLoadOrder
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin
import java.net.URL

// Workaround for (https://youtrack.jetbrains.com/issue/KTIJ-19369)
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.minix.nms)
    alias(libs.plugins.minix.kotlin)
    alias(libs.plugins.minix.copyjar)
    alias(libs.plugins.minix.purpurmc) apply false

    alias(libs.plugins.minecraft.pluginYML)
    alias(libs.plugins.dokka)

    alias(libs.plugins.kotlin.binaryValidator)
    alias(libs.plugins.slimjar)
    alias(libs.plugins.ksp)
}

apiValidation {
    ignoredPackages += "Terix-Core"
    ignoredPackages.add("dev.racci.terix.core")
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
        "LibsDisguises",
        "ItemsAdder"
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
    apply<KspGradleSubplugin>()

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

        compileOnly(rootProject.libs.arrow.core)
        compileOnly(rootProject.libs.arrow.fx.stm)
        compileOnly(rootProject.libs.arrow.fx.coroutines)
        compileOnly(rootProject.libs.arrow.optics.reflect)
        compileOnly(rootProject.libs.aedile)

        ksp(rootProject.libs.arrow.optics.ksp)

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
//        withSourcesJar()
    }

    kotlin {
        explicitApiWarning()
    }

    buildDir = File("$rootDir/build/${project.name}")

    sourceSets.getByName("main").kotlin.srcDir("$buildDir/generated/ksp/main/kotlin")

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

        val minixPrefix = "dev.racci.minix.libs"
        relocate("io.sentry", "$minixPrefix.io.sentry")
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

fun Project.emptySources() = project.sourceSets.none { set -> set.allSource.any { file -> file.extension == "kt" } }

allprojects {
    if (!project.emptySources()) {
        apply<KtlintPlugin>()

        configure<KtlintExtension> {
            this.baseline.set(file("$rootDir/config/ktlint/baseline-${project.name.toLowerCase()}.xml"))
        }

        tasks {
            // For some reason this task likes to delete the entire folder contents,
            // So we need all projects to have their own sub folder.
            val apiDir = file("$rootDir/config/api/${project.name.toLowerCase()}")
            apiDump { destinationDir = apiDir }
            apiCheck { projectApiDir = apiDir }
        }
    } else {
        tasks {
            apiDump.get().enabled = false
            apiCheck.get().enabled = false
        }
    }

    repositories {
        maven("https://nexus.frengor.com/repository/public/")
    }

    dependencies {
        compileOnly(rootProject.libs.arrow.optics)
    }
}
