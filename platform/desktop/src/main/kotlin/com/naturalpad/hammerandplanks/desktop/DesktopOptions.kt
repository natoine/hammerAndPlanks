package com.naturalpad.hammerandplanks.desktop

/** Options de la ligne de commande du lanceur desktop. */
data class DesktopOptions(
    val language: String? = null,
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
    val screenshotPath: String? = null,
    val inputMode: String? = null,
) {
    companion object {
        const val DEFAULT_WIDTH = 540
        const val DEFAULT_HEIGHT = 960
        val INPUT_MODES = listOf("keyboard", "gamepad", "motion")

        fun parse(args: Array<String>): DesktopOptions {
            var options = DesktopOptions()
            for (arg in args) {
                val value = arg.substringAfter("=", "")
                options = when {
                    arg.startsWith("--lang=") -> options.copy(language = value.ifBlank { null })
                    arg.startsWith("--input-mode=") -> {
                        require(value.lowercase() in INPUT_MODES) { "Mode inconnu : '$value' (attendu : ${INPUT_MODES.joinToString()})" }
                        options.copy(inputMode = value.uppercase())
                    }
                    arg.startsWith("--screenshot=") -> options.copy(screenshotPath = value.ifBlank { null })
                    arg.startsWith("--size=") -> {
                        val parts = value.lowercase().split("x")
                        val w = parts.getOrNull(0)?.toIntOrNull()
                        val h = parts.getOrNull(1)?.toIntOrNull()
                        require(parts.size == 2 && w != null && h != null && w > 0 && h > 0) {
                            "Taille invalide : '$value' (attendu LARGEURxHAUTEUR, ex. 540x960)"
                        }
                        options.copy(width = w, height = h)
                    }
                    else -> throw IllegalArgumentException("Option inconnue : $arg")
                }
            }
            return options
        }
    }
}
