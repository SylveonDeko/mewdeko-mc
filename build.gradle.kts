plugins {
    java
    id("net.neoforged.gradle.userdev") version "7.0.145"
}

group = "tech.mewdeko"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

runs {
    create("server") {
        server()
        systemProperty("forge.logging.console.level", "debug")
    }
}

dependencies {
    implementation("net.neoforged:neoforge:21.1.77")

    // Embed OkHttp and its dependencies (Gson is already provided by Minecraft)
    val okhttp = "com.squareup.okhttp3:okhttp:4.12.0"
    val okio = "com.squareup.okio:okio:3.9.0"
    val okioJvm = "com.squareup.okio:okio-jvm:3.9.0"
    val kotlinStdlib = "org.jetbrains.kotlin:kotlin-stdlib:2.0.21"

    implementation(okhttp)
    implementation(okio)
    implementation(okioJvm)
    implementation(kotlinStdlib)

    jarJar(okhttp) { jarJar.ranged(this, "[4.12.0,5.0)") }
    jarJar(okio) { jarJar.ranged(this, "[3.9.0,4.0)") }
    jarJar(okioJvm) { jarJar.ranged(this, "[3.9.0,4.0)") }
    jarJar(kotlinStdlib) { jarJar.ranged(this, "[2.0.0,3.0)") }
}

tasks.withType<ProcessResources> {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}
