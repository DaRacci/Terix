plugins {
    java
    idea
    `java-library`
    `maven-publish`
    kotlin("jvm")                       version "1.6.0-RC2"
    id("org.jetbrains.dokka")               version "1.5.31"
    kotlin("plugin.serialization")      version "1.5.31"
    id("com.github.johnrengelman.shadow")   version "7.1.0"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
    maven("https://repo.pl3x.net/")
    maven("https://dl.bintray.com/kotlin/kotlin-dev/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {

    compileOnly("me.racci:RacciCore:0.2.4")

    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.0-RC2")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:1.6.0-RC2")

    compileOnly("org.jetbrains.kotlinx:kotlinx-datetime:0.3.1")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.5.2-native-mt")

    compileOnly("net.luckperms:api:5.3")
    compileOnly("me.clip:placeholderapi:2.10.10")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")

}

tasks {

    processResources {
        from(sourceSets.main.get().resources.srcDirs) {
            filesMatching("plugin.yml") {
                expand("version" to project.version)}
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
    }

    compileKotlin {
        kotlinOptions.suppressWarnings = true
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }

    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    val devServer by registering(Jar::class) {
        dependsOn(shadowJar)
        destinationDirectory.set(File("${System.getProperty("user.home")}/Desktop/Minecraft/Sylph/Development/plugins/"))
        archiveClassifier.set("all")
        from(zipTree(shadowJar.get().outputs.files.singleFile))
    }

    val sourcesJar by registering(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    val javadocJar by registering(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc.get().outputDirectory)
    }

    dokkaGfm {
        outputDirectory.set(File("$buildDir/../docs"))
    }

    artifacts {
        archives(sourcesJar)
        archives(javadocJar)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = findProperty("group")!!
version = findProperty("version")!!