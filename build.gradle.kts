plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow")
    id("java")
}

group = "${gradle.rootProject.group}"
version = "0.0.4"

dependencies {

    compileOnly(project(":RacciCore"))
    compileOnly("me.clip:placeholderapi:2.10.10")
    compileOnly("com.github.angeschossen:LandsAPI:5.15.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")

}
