plugins {
    id("dev.racci.minix.nms")
}

val minixVersion: String by rootProject

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
}
