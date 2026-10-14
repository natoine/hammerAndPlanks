import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// Génère BuildInfo.kt avec la version lue dans package.json (voir build.gradle.kts racine).
val generatedDir = layout.buildDirectory.dir("generated/buildinfo/kotlin")
val generateBuildInfo = tasks.register("generateBuildInfo") {
    val version = project.version.toString()
    val outDir = generatedDir
    inputs.property("version", version)
    outputs.dir(outDir)
    doLast {
        val file = outDir.get().file("com/naturalpad/hammerandplanks/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.naturalpad.hammerandplanks
            |
            |/** Généré par Gradle depuis package.json. Ne pas modifier. */
            |object BuildInfo {
            |    const val VERSION: String = "$version"
            |}
            |""".trimMargin()
        )
    }
}
sourceSets["main"].kotlin.srcDir(generatedDir)
tasks.named("compileKotlin") { dependsOn(generateBuildInfo) }

dependencies {
    api(libs.gdx)
    api(libs.gdx.freetype)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Les tests lisent les fichiers de traduction partagés dans assets/.
    systemProperty("assetsDir", rootProject.file("assets").absolutePath)
}
