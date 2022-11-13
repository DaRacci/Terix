import java.net.URI

plugins {
    `maven-publish`
}

dependencies {
    testImplementation(project(":Terix-Core"))
    compileOnly(libs.minecraft.particles)
}

tasks {
    reobfJar.get().enabled = false

    afterEvaluate {
        jar.get().archiveClassifier.set("")
    }
}

publishing {
    repositories.maven {
        name = "RacciRepo"
        url = URI("https://repo.racci.dev/${if (version.toString().endsWith("-SNAPSHOT")) "snapshots" else "releases"}")
        credentials(PasswordCredentials::class)
    }

    publications.register("maven", MavenPublication::class) {
        artifactId = rootProject.name
        from(components["kotlin"])

        pom {
            inceptionYear.set("2022")
            url.set("https://terix.racci.dev")
            developers {
                developer {
                    name.set("Racci")
                    email.set("racci@racci.dev")
                    url.set("https://github.com/DaRacci")
                    timezone.set("Australia/Sydney")
                    roles.set(listOf("Lead", "Developer"))
                }
            }
        }
    }
}
