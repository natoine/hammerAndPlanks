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

// Modèle de pose InstantHMR (non versionné, licence SAM) : téléchargé puis vérifié par son empreinte SHA-256.
tasks.register("downloadModel") {
    group = "setup"
    description = "Télécharge models/instanthmr.onnx depuis HuggingFace et vérifie son empreinte."
    val target = layout.projectDirectory.file("models/instanthmr.onnx").asFile
    val checksums = layout.projectDirectory.file("models/checksums.txt").asFile
    outputs.file(target)
    doLast {
        val expected = checksums.readLines().first { it.endsWith("instanthmr.onnx") }.substringBefore(" ")
        fun sha256(file: java.io.File): String {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(1 shl 16)
                while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (target.isFile && sha256(target) == expected) {
            logger.lifecycle("Modèle déjà présent et vérifié : ${target.path}")
            return@doLast
        }
        val url = "https://huggingface.co/momolesang/InstantHMR/resolve/main/instanthmr.onnx"
        logger.lifecycle("Téléchargement de $url (environ 77 Mo)...")
        val partial = java.io.File(target.path + ".part")
        java.net.URI(url).toURL().openStream().use { input -> partial.outputStream().use { input.copyTo(it) } }
        val actual = sha256(partial)
        if (actual != expected) {
            partial.delete()
            throw GradleException("Empreinte inattendue pour instanthmr.onnx : $actual (attendu $expected)")
        }
        partial.renameTo(target)
        logger.lifecycle("Modèle téléchargé et vérifié : ${target.path}")
    }
}
