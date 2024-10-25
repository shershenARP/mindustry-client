
import org.jetbrains.kotlin.gradle.dsl.*
import java.io.File
import java.util.*

plugins {
    kotlin("jvm") version "2.0.10"
}

repositories {
    maven(url = "https://jitpack.io")
    mavenCentral()
}

dependencies {
    val arcHash = Properties(20).apply { load(file("../gradle.properties").inputStream()) }["archash"]
    val localArc = File(rootDir.parentFile.parent, "Arc").exists() && !project.hasProperty("noLocalArc")
    implementation("com.github.mindustry-antigrief${if (localArc) "" else ".arc"}:arc-core:$arcHash")
}

// I swear gradle is the worst
kotlin.compilerOptions.jvmTarget.set(JvmTarget.JVM_16)

tasks.withType(org.gradle.api.tasks.compile.JavaCompile::class.java){
    targetCompatibility = "16"
}