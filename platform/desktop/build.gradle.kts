import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }

application {
    mainClass.set("com.naturalpad.hammerandplanks.desktop.DesktopLauncherKt")
}

dependencies {
    implementation(project(":game"))
    implementation(libs.gdx.backend.lwjgl3)
    implementation(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    implementation(variantOf(libs.gdx.freetype.platform) { classifier("natives-desktop") })
    implementation(libs.gdx.controllers.desktop) // manettes (SDL via Jamepad)
    implementation(libs.webcam.capture)          // webcam (V4L2, AVFoundation, DirectShow)
    implementation(project(":gestures"))         // pose -> gestes -> GameInput (mode caméra)
    implementation(project(":pose-onnx"))
    implementation(libs.onnxruntime)             // InstantHMR sur le processeur
}

tasks.named<JavaExec>("run") {
    // Les fichiers « internal » de libGDX sont lus depuis le dossier assets partagé.
    workingDir = rootProject.file("assets")
    // Modèle de pose, non versionné (licence SAM) : ./gradlew downloadModel
    args("--model=" + rootProject.file("models/instanthmr.onnx").absolutePath)
    // Permet de forcer la langue : ./gradlew :platform:desktop:run -Plang=en
    // et de prendre une capture : -Pscreenshot=/chemin/capture.png -Psize=1920x1080
    project.findProperty("lang")?.let { args("--lang=$it") }
    project.findProperty("size")?.let { args("--size=$it") }
    project.findProperty("screenshot")?.let { args("--screenshot=$it") }
    // et d'imposer le mode d'interaction : -Pmode=motion (keyboard, gamepad, motion)
    project.findProperty("mode")?.let { args("--input-mode=$it") }
    // et le contrôle du bateau : -Pcontrol=velocity -Pspeed=0.5
    project.findProperty("control")?.let { args("--control=$it") }
    project.findProperty("speed")?.let { args("--speed=$it") }
    // et l'inclinaison suivie en mode caméra : -Ptilt=head (shoulders par défaut)
    project.findProperty("tilt")?.let { args("--tilt=$it") }
    // et journaliser les mesures de pose chaque seconde : -PdebugPose
    if (project.hasProperty("debugPose")) args("--debug-pose")
    project.findProperty("screenshotDelay")?.let { args("--screenshot-delay=$it") }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.test { useJUnitPlatform() }
