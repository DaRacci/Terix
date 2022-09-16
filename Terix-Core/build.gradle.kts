plugins {
    id("dev.racci.minix.nms")
}

val minixVersion: String by rootProject

dependencies {
    compileOnly(project(":Terix-API"))
    compileOnly(rootProject.libs.minecraft.api.placeholderAPI)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.minecraft.api.landsAPI)
    compileOnly(rootProject.libs.minecraft.api.ecoEnchants)
    compileOnly(rootProject.libs.minecraft.api.eco)
    compileOnly(rootProject.libs.minecraft.api.lightAPI)
    compileOnly(rootProject.libs.minecraft.inventoryFramework)
    compileOnly(rootProject.libs.bundles.cloud)
    compileOnly(rootProject.libs.bundles.cloud.kotlin)
    compileOnly(rootProject.libs.minecraft.particles)

    testImplementation(project(":Terix-API"))
}
