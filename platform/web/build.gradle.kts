import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    implementation(project(":game"))
    implementation(project(":gestures")) // pose -> gestes -> GameInput (mode caméra)
    implementation(libs.gdx.teavm.backend)
    implementation(libs.gdx.teavm.freetype)
}

val distDir = layout.buildDirectory.dir("dist")

// Compile le jeu en JavaScript avec TeaVM (gdx-teavm) dans build/dist/webapp.
val buildWeb = tasks.register<JavaExec>("buildWeb") {
    group = "build"
    description = "Construit la version navigateur (TeaVM) dans build/dist/webapp."
    dependsOn("classes")
    mainClass.set("com.naturalpad.hammerandplanks.web.BuildWebKt")
    classpath = sourceSets["main"].runtimeClasspath
    args(
        rootProject.file("assets").absolutePath,
        distDir.get().asFile.absolutePath,
    )
    finalizedBy("publishChangelog", "customizeWebIndex")
}

// Publie le CHANGELOG sur la route /changelog de la version web.
val publishChangelog = tasks.register("publishChangelog") {
    val changelog = rootProject.file("CHANGELOG.md")
    val out = distDir.map { it.file("webapp/changelog/index.html") }
    inputs.file(changelog)
    outputs.file(out)
    doLast {
        val escaped = changelog.readText()
            .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val file = out.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |<!doctype html>
            |<html lang="fr">
            |<head>
            |<meta charset="utf-8">
            |<meta name="viewport" content="width=device-width, initial-scale=1">
            |<title>Hammer and Planks - Changelog</title>
            |<style>
            |  body { margin: 0; padding: 1rem; background: #14213d; color: #f4e3b5; font: 1rem/1.5 system-ui, sans-serif; }
            |  pre { white-space: pre-wrap; word-wrap: break-word; max-width: 60rem; margin: 0 auto; }
            |</style>
            |</head>
            |<body><pre id="changelog">$escaped</pre></body>
            |</html>
            |""".trimMargin()
        )
    }
}

// Remplace la page d'accueil générée par gdx-teavm (viewport mobile, fond, interop navigateur) et ajoute
// l'estimation de pose : pose.js, ONNX Runtime Web (WebAssembly) et, s'il est téléchargé, le modèle.
tasks.register<Copy>("customizeWebIndex") {
    into(distDir.map { it.dir("webapp") })
    from(layout.projectDirectory.dir("webapp"))
    from(rootProject.file("node_modules/onnxruntime-web/dist")) {
        include("ort.wasm.min.js", "ort-wasm-simd-threaded.wasm", "ort-wasm-simd-threaded.mjs")
        into("ort")
    }
    from(rootProject.file("models")) {
        include("instanthmr.onnx")
        into("models")
    }
    doFirst {
        check(rootProject.file("node_modules/onnxruntime-web/dist/ort.wasm.min.js").isFile) {
            "onnxruntime-web absent : lancer npm install"
        }
    }
}
