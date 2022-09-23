plugins {
    id("dev.racci.minix.nms")
}

val minixVersion: String by rootProject

repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(project(":Terix-API"))
    compileOnly(rootProject.libs.minecraft.api.placeholderAPI)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.minecraft.api.landsAPI)
    compileOnly(rootProject.libs.minecraft.api.eco)
    compileOnly(rootProject.libs.minecraft.api.lightAPI)
    compileOnly(rootProject.libs.minecraft.inventoryFramework)
    compileOnly(rootProject.libs.bundles.cloud)
    compileOnly(rootProject.libs.bundles.cloud.kotlin)
    compileOnly(rootProject.libs.minecraft.particles)

    compileOnly("com.willfp:libreforge:3.104.0")
    compileOnly("com.willfp:EcoEnchants:9.0.0-b3")

    testImplementation(project(":Terix-API"))
}
