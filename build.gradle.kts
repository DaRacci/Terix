plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("java")
}

group = "${gradle.rootProject.group}"
version = "0.0.4"

dependencies {

    compileOnly(project(":RacciCore"))

}
