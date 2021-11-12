plugins {
    java
    idea
    `java-library`
    `maven-publish`
    kotlin("jvm")                       version "1.6.0-RC2"
    id("org.jetbrains.dokka")               version "1.5.31"
    kotlin("plugin.serialization")      version "1.5.31"
    id("com.github.johnrengelman.shadow")   version "7.1.0"
}

group = rootProject.group
version = "0.0.6"

dependencies {

    compileOnly(rootProject.libs.racciCore)
    compileOnly(rootProject.libs.kotlinX.serialization)
    compileOnly(rootProject.libs.plugin.placeholderAPI)
    compileOnly(rootProject.libs.plugin.luckPermsAPI)
    compileOnly(rootProject.libs.plugin.protocolLib)

}