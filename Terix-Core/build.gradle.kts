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

    implementation(project(":Terix-API"))
    implementation("dev.jorel.CommandAPI:commandapi-shade:6.5.3")
    implementation("dev.racci:Minix-NMS:$minixVersion")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("dev.racci:Minix:1.2.0")
    testImplementation(rootProject.libs.koin.test)
    testImplementation("io.strikt:strikt-core:0.34.1")
}

tasks.test.get().useJUnitPlatform()
