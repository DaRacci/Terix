enableFeaturePreview("VERSION_CATALOGS")
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.racci.dev/releases")
        maven("https://papermc.io/repo/repository/maven-public/")
    }
    plugins {
        val kotlinVersion: String by settings
        kotlin("plugin.serialization") version kotlinVersion
    }
    val minixConventions: String by settings
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.startsWith("dev.racci.minix")) {
                useVersion(minixConventions)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.racci.dev/releases")
    }

    val minixConventions: String by settings
    versionCatalogs.create("libs") {
        from("dev.racci:catalog:$minixConventions")
    }
}

rootProject.name = "Sylphia"

include("Sylphia-API")
include("Sylphia-Core")
