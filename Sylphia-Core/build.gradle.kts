plugins {
    id("dev.racci.minix.nms")
}

val minixVersion: String by rootProject

dependencies {
    compileOnly(rootProject.libs.exposed.core)
    compileOnly(rootProject.libs.exposed.dao)
    compileOnly(rootProject.libs.exposed.jdbc)
    compileOnly(rootProject.libs.hikariCP)
    compileOnly(rootProject.libs.minecraft.api.placeholderAPI)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.caffeine)
    compileOnly(rootProject.libs.minecraft.api.landsAPI)

    implementation(project(":Sylphia-API"))
    implementation("dev.jorel.CommandAPI:commandapi-shade:6.5.3")
    implementation("dev.racci:Minix-NMS:$minixVersion")
}
