plugins {
    alias(libs.plugins.android.application)
}

// Bibliothèques natives libGDX à extraire dans jniLibs pour chaque ABI.
val natives: Configuration = configurations.create("natives")

android {
    namespace = "com.naturalpad.hammerandplanks.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.naturalpad.hammerandplanks"
        minSdk = 24
        targetSdk = 36
        versionName = project.version.toString()
        // 0.1.0 -> 100, 1.2.3 -> 10203
        versionCode = project.version.toString().split(".").map { it.takeWhile(Char::isDigit).toInt() }
            .let { (major, minor, patch) -> major * 10000 + minor * 100 + patch }
    }

    sourceSets["main"].assets.directories.add(rootProject.file("assets").path)
    sourceSets["main"].jniLibs.directories.add(layout.buildDirectory.dir("natives").get().asFile.path)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":game"))
    implementation(libs.gdx.backend.android)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.lifecycle.runtime) // LifecycleOwner Kotlin pour lier CameraX hors ComponentActivity
    testImplementation(kotlin("test-junit5"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
    for (abi in listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")) {
        natives(variantOf(libs.gdx.platform) { classifier("natives-$abi") })
        natives(variantOf(libs.gdx.freetype.platform) { classifier("natives-$abi") })
    }
}

val copyAndroidNatives = tasks.register<Copy>("copyAndroidNatives") {
    val outDir = layout.buildDirectory.dir("natives")
    into(outDir)
    natives.files.forEach { jar ->
        val abi = Regex("natives-([^.]+)\\.jar$").find(jar.name)?.groupValues?.get(1) ?: return@forEach
        from(zipTree(jar)) {
            include("*.so")
            into(abi)
        }
    }
}
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
    .configureEach { dependsOn(copyAndroidNatives) }

tasks.withType<Test>().configureEach { useJUnitPlatform() }
