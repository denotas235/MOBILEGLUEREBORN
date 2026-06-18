plugins {
    id("fabric-loom") version "1.9.2"
    id("maven-publish")
}

version = "1.0.0"
group = "com.nexus"

base {
    archivesName.set("mobileglues-sanitizer")
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

dependencies {
    // Minecraft 1.21.1 com Mojang Official Mappings
    minecraft("com.mojang:minecraft:1.21.1")
    mappings(loom.officialMojangMappings())

    // Fabric Loader — Mixin não precisa de Fabric API
    modImplementation("net.fabricmc:fabric-loader:0.16.9")
}

tasks.processResources {
    inputs.property("version", version)
    filteringCharset = "UTF-8"
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to version))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
}
