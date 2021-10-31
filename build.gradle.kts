plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("java")
}

group = rootProject.group
version = "0.0.5"

dependencies {

    compileOnly(project(":RacciCore"))
    compileOnly(rootProject.libs.placeholderAPI)
    compileOnly(rootProject.libs.luckPermsAPI)
    compileOnly(rootProject.libs.protocolLib)

}
