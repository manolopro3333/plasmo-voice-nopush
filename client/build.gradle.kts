plugins {
    id("net.fabricmc.fabric-loom") version "1.19.4"
    alias(libs.plugins.shadow)
    kotlin("jvm")
}

group = "$group.client"
base.archivesName.set("canelososvoice-fabric-26.3")

val minecraftVersion = "26.3"
val fabricApiVersion = project.property("deps.fabric_api") as String
val fabricPermissionsApiVersion = project.property("deps.fabric_permissions_api") as String
val clothConfigVersion = project.property("deps.cloth_config") as String
val modMenuVersion = project.property("deps.modmenu") as String
val slibMcSlug = project.property("deps.slib") as String
val slibVersion = libs.versions.slib.get()
val slibArtifact = "$slibMcSlug-fabric"
val mcVersionsRange = project.property("mod.minecraft_versions") as String

val mixins = listOf("plasmovoice.mixins.json", "slib.mixins.json")

loom {
    runs {
        named("client") {
            programArguments.addAll("--username", "GNOME__")
            systemProperties.put("plasmovoice.debug", "true")
        }
    }
}

repositories {
    mavenLocal()

    maven("https://repo.plasmoverse.com/snapshots")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.nucleoid.xyz/")

    maven("https://api.modrinth.com/maven") {
        content { includeGroup("maven.modrinth") }
    }

    maven("https://repo.maven.apache.org/maven2/") {
        name = "MavenCentralLWJGL"
        content { includeGroup("org.lwjgl") }
    }
}

val shadowCommon = configurations.create("shadowCommon")

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:0.20.1")

    compileOnly(libs.netty)

    implementation(libs.rnnoise.jni)
    implementation(libs.opus.jni)
    implementation(libs.opus.concentus)

    fun fabricApiModules(vararg module: String) {
        module.forEach {
            implementation(fabricApi.module("fabric-$it", fabricApiVersion))
        }
    }

    fabricApiModules("rendering-v1", "networking-api-v1", "lifecycle-events-v1", "key-mapping-api-v1")

    runtimeOnly("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    val fabricPermissionsApi = "me.lucko:fabric-permissions-api:$fabricPermissionsApiVersion"
    "include"(fabricPermissionsApi)
    implementation(fabricPermissionsApi) {
        isTransitive = false
    }

    implementation("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
        exclude("net.fabricmc.fabric-api")
    }

    implementation("maven.modrinth:modmenu:$modMenuVersion")

    val includedProjects = listOf(
        ":api:common",
        ":api:client",
        ":api:server-proxy-common",
        ":api:server",
        ":server:common",
        ":server-proxy-common",
        ":common",
        ":protocol",
        ":macos:protocol"
    )

    includedProjects.forEach {
        implementation(project(it))
        shadowCommon(project(it))
    }

    shadowCommon(libs.rnnoise.jni)

    api("su.plo.slib:$slibArtifact:$slibVersion") {
        isTransitive = false
    }
    shadowCommon("su.plo.slib:$slibArtifact:$slibVersion") {
        isTransitive = false
    }
}

tasks {
    java {
        withSourcesJar()
    }

    processResources {
        inputs.property("version", version)
        inputs.property("fabricApiVersion", fabricApiVersion)
        inputs.property("mcVersions", mcVersionsRange)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to version,
                "fabricDependencyName" to "fabric-api",
                "fabricApiVersion" to "*",
                "mcVersions" to mcVersionsRange,
                "mixins" to mixins.joinToString(", ") { "\"$it\"" }.removeSurrounding("\""),
            )
        }

        exclude("assets/plasmovoice/shaders/core/*.json")
        exclude("assets/plasmovoice/shaders/*.json")

        filesMatching(listOf("**/*.vsh", "**/*.fsh")) {
            filter { line ->
                val trimmed = line.trimStart()
                line.takeUnless { trimmed.startsWith("//#") || trimmed.startsWith("//$$") }
            }
        }
    }

    jar {
        archiveClassifier.set("dev")
        exclude("assets/plasmovoice/shaders/core/*.json")
        exclude("assets/plasmovoice/shaders/*.json")
    }

    shadowJar {
        dependsOn(jar)
        configurations = listOf(shadowCommon)
        archiveClassifier.set("")

        exclude("fabric.mod.json")
        rootSpec.from(sourceSets.main.get().output) {
            include("fabric.mod.json")
        }
        rootSpec.from(project.tasks.named("processIncludeJars")) {
            into("META-INF/jars")
        }

        mergeServiceFiles()

        relocate("gg.essential.universal", "su.plo.voice.universal")
        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
        relocate("kotlinx.serialization", "su.plo.voice.libs.kotlinx.serialization")
        relocate("com.ensarsarajcic.kotlinx", "su.plo.voice.libs.msgpack")
        relocate("org.concentus", "su.plo.voice.libs.concentus")
        relocate("at.favre.lib.hkdf", "su.plo.voice.libs.hkdf")
        relocate("dev.apehum.mcdsl", "su.plo.voice.libs.mcdsl")
        relocate("su.plo.crowdin", "su.plo.voice.libs.crowdin")
        relocate("org.bstats", "su.plo.voice.libs.bstats")

        exclude("DebugProbesKt.bin")
        exclude("_COROUTINE/**")
        exclude("LICENSE*")
        exclude("README.md")
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_module")
        exclude("**/*.kotlin_builtins")
        exclude("pack.mcmeta")
        exclude("META-INF/neoforge.mods.toml")

        dependencies {
            exclude(dependency("net.java.dev.jna:jna"))
            exclude(dependency("org.jetbrains:annotations"))
            exclude(dependency("org.projectlombok:lombok"))
            exclude(dependency("com.google.guava:guava"))
            exclude(dependency("com.google.guava:failureaccess"))
            exclude(dependency("com.google.guava:listenablefuture"))
            exclude(dependency("com.google.code.gson:gson"))
            exclude(dependency("com.google.code.findbugs:jsr305"))
            exclude(dependency("com.google.errorprone:error_prone_annotations"))
            exclude(dependency("com.google.j2objc:j2objc-annotations"))
            exclude(dependency("it.unimi.dsi:fastutil"))
            exclude(dependency("org.checkerframework:checker-qual"))
            exclude(dependency("com.mojang:brigadier"))
            exclude(dependency("org.slf4j:slf4j-api"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    register<Copy>("copyJarToRootProject") {
        dependsOn(shadowJar)
        from(shadowJar.get().archiveFile)
        into(rootProject.layout.buildDirectory.dir("libs"))
    }
}