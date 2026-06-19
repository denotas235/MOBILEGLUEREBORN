plugins {
    id("fabric-loom") version "1.17.11"
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
    // Minecraft 1.21.11 com Mojang Official Mappings
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())

    // Fabric Loader 0.19.3 (latest para 1.21.11)
    modImplementation("net.fabricmc:fabric-loader:0.19.3")

    // Fabric API 0.141.4+1.21.11
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.4+1.21.11")
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
