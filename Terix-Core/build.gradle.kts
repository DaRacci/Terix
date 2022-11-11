val minixVersion: String by rootProject

dependencies {
    compileOnly(project(":Terix-API"))
    compileOnly(rootProject.libs.minecraft.api.placeholderAPI)
    compileOnly(rootProject.libs.minecraft.api.protoclLib)
    compileOnly(rootProject.libs.minecraft.api.landsAPI)
    compileOnly("com.willfp:eco:6.44.0")
    compileOnly(rootProject.libs.minecraft.api.lightAPI)
    compileOnly(rootProject.libs.bundles.cloud)
    compileOnly(rootProject.libs.bundles.cloud.kotlin)
    compileOnly(rootProject.libs.minecraft.particles)

    compileOnly(libs.minecraft.api.ecoEnchants)
    compileOnly("com.willfp:libreforge:3.104.0")
    compileOnly("com.frengor:ultimateadvancementapi-shadeable:2.2.1")
    implementation("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT")
    implementation("org.incendo.interfaces:interfaces-kotlin:1.0.0-SNAPSHOT")

    testImplementation(project(":Terix-API"))
}

tasks.compileKotlin {
    kotlinOptions {
        this.freeCompilerArgs += "-opt-in=dev.racci.minix.api.annotations.MinixInternal"
        useK2 = true
    }
}
