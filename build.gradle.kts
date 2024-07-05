import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar


plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "de.derioo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.derioo.de/releases")
    maven("https://reposilite.koboo.eu/releases")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("eu.koboo:en2do:2.3.9")

    implementation("com.fasterxml.jackson.core:jackson-core:2.17.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.2")

    implementation("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("org.jetbrains:annotations:24.1.0")

    implementation("org.mongodb:mongodb-driver-sync:5.1.1")
    implementation("com.cronutils:cron-utils:9.2.1")


    implementation("net.dv8tion:JDA:5.0.0-beta.24")
    implementation("de.derioo.javautils:common:2.6.15")
    implementation("de.derioo.javautils:discord:2.7.1")
}
java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    manifest {
        attributes(
            "Main-Class" to "de.derioo.reminder.Main"
        )
    }
}