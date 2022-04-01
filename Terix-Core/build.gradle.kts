plugins {
    id("dev.racci.minix.nms")
}

val minixVersion: String by rootProject
val minixAPIVersion: String by rootProject
val serverVersion: String by rootProject

dependencies {
    compileOnly(rootProject.libs.minecraft.api.placeholderAPI)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.minecraft.api.landsAPI)
    compileOnly(rootProject.libs.minecraft.api.ecoEnchants)
    compileOnly(rootProject.libs.minecraft.api.eco)

    compileOnly(project(":Terix-API"))
    compileOnly(rootProject.libs.minecraft.inventoryFramework)
    compileOnly(rootProject.libs.minecraft.commandAPI)
    compileOnly("dev.racci:Minix-NMS:$minixVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.6.10") {
        because("For some reason the koin dep breaks this one in the bundle.")
    }
    testImplementation(rootProject.libs.bundles.testing) {
        exclude("org.jetbrains.kotlin", "kotlin-test-junit")
    }
    testImplementation(rootProject.libs.bundles.kotlin)
    testImplementation(rootProject.libs.bundles.kotlinx)
    testImplementation(rootProject.libs.minecraft.minix)
    testImplementation("dev.racci.tentacles:tentacles-api:$serverVersion")
}

tasks.test.get().useJUnitPlatform()
