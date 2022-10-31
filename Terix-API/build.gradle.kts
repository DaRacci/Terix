apply {
    plugin("dev.racci.minix.nms")
}

java.withSourcesJar()

dependencies {
    testImplementation(project(":Terix-Core"))
}
