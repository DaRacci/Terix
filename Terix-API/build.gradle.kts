apply {
    plugin("dev.racci.minix.testing")
    plugin("dev.racci.minix.nms")
}

dependencies {
    testImplementation(project(":Terix-Core"))
}
