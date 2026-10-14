// La version du projet vient de package.json : une seule source de vérité pour Gradle, npm et le CHANGELOG.
val packageVersion: String = Regex("\"version\"\\s*:\\s*\"([^\"]+)\"")
    .find(rootProject.file("package.json").readText())
    ?.groupValues?.get(1)
    ?: error("Version introuvable dans package.json")

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
}

allprojects {
    group = "com.naturalpad.hammerandplanks"
    version = packageVersion
}
