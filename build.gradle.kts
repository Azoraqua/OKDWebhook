plugins {
    kotlin("jvm") version "2.1.21"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.0.0-beta15"
}

group = "dev.azoraqua"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation(kotlin("stdlib"))

    val coroutinesVersion = "1.10.2"
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")

    val lampVersion = "4.0.0-rc.12"
    implementation("io.github.revxrsal:lamp.common:$lampVersion")
    implementation("io.github.revxrsal:lamp.bukkit:$lampVersion")
    implementation("io.github.revxrsal:lamp.velocity:$lampVersion")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.google.code.gson:gson:2.10.1")
}

val paperApiDependency = configurations.compileOnly.get().dependencies
    .find { it.group == "io.papermc.paper" && it.name == "paper-api" }
val velocityApiDependency = configurations.compileOnly.get().dependencies
    .find { it.group == "com.velocitypowered" && it.name == "velocity-api" }

val paperApiVersionFull = paperApiDependency?.version ?: "1.21.5-R0.1-SNAPSHOT"
val velocityApiVersionFull = velocityApiDependency?.version ?: "3.4.0-SNAPSHOT"

tasks.processResources {
    val paperApiVersion = paperApiVersionFull.substringBefore("-").substringBeforeLast(".")
    val velocityApiVersion = velocityApiVersionFull.substringBefore("-")

    expand(
        "projectName" to project.name,
        "projectVersion" to project.version,
        "paperApiVersion" to paperApiVersion,
        "velocityApiVersion" to velocityApiVersion
    )
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()

    val basePackage = "${project.group}.${project.name}".lowercase()
    relocate("com.squareup.okhttp3", "$basePackage.libs.okhttp3")
    relocate("com.google.gson", "$basePackage.libs.gson")
    relocate("revxrsal.commands", "$basePackage.libs.commands")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.runServer {
    minecraftVersion(paperApiVersionFull.substringBefore("-"))
    jvmArgs("-Dcom.mojang.eula.agree=true")
}

kotlin {
    jvmToolchain(21)
    compilerOptions.javaParameters = true
}
