plugins {
    kotlin("jvm") version "2.1.21"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.gradleup.shadow") version "9.0.0-beta15"
}

group = "nl.odysseykingdom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.5-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")

    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.12")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.12")
    implementation("io.github.revxrsal:lamp.velocity:4.0.0-rc.12")

    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.12.0"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()

    relocate("com.squareup.okhttp3", "nl.odysseykingdom.webhook.libs.okhttp3")
    relocate("com.google.gson", "nl.odysseykingdom.webhook.libs.gson")
    relocate("revxrsal.commands", "nl.odysseykingdom.webhook.libs.commands")

    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.runServer {
    minecraftVersion("1.21.5")
    jvmArgs("-Dcom.mojang.eula.agree=true")
}

kotlin {
    jvmToolchain(21)
    compilerOptions.javaParameters = true
}