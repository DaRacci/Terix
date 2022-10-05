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

    resolutionStrategy {
        val kotlinVersion: String by settings
        val minixConventions: String by settings
        val conventionsVersion = "$kotlinVersion-$minixConventions"

        eachPlugin {
            if (requested.id.id.startsWith("dev.racci.minix")) {
                useVersion(conventionsVersion)
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://repo.racci.dev/releases")
    }

    versionCatalogs.create("libs") {
        val kotlinVersion: String by settings
        val minixConventions: String by settings
        val conventionsVersion = "$kotlinVersion-$minixConventions"
        from("dev.racci:catalog:$conventionsVersion")
    }
}

rootProject.name = "Terix"

include("Terix-API")
include("Terix-Core")
