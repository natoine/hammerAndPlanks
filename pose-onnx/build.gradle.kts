import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Exécution d'InstantHMR avec ONNX Runtime (API Java commune au desktop et à Android).
// L'artefact natif est fourni par chaque lanceur : onnxruntime (desktop) ou onnxruntime-android.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    api(project(":pose"))
    compileOnly(libs.onnxruntime)

    testImplementation(libs.onnxruntime)
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test {
    useJUnitPlatform()
    // Le test d'intégration utilise le vrai modèle s'il a été téléchargé (./gradlew downloadModel).
    systemProperty("modelPath", rootProject.file("models/instanthmr.onnx").absolutePath)
}
