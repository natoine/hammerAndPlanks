package com.naturalpad.hammerandplanks.desktop

/** Options de la ligne de commande du lanceur desktop. */
data class DesktopOptions(
    val language: String? = null,
    val width: Int = DEFAULT_WIDTH,
    val height: Int = DEFAULT_HEIGHT,
    val screenshotPath: String? = null,
    val inputMode: String? = null,
    val controlMode: String? = null,
    val speedFactor: String? = null,
    val modelPath: String? = null,
    val tiltSource: String? = null,
    val debugPose: Boolean = false,
    val screenshotDelayS: Float = 3f,
) {
    companion object {
        const val DEFAULT_WIDTH = 540
        const val DEFAULT_HEIGHT = 960
        val INPUT_MODES = listOf("keyboard", "gamepad", "motion")
        val CONTROL_MODES = listOf("position", "velocity", "force")
        /** shoulders : ligne des épaules ; head : ligne des oreilles. */
        val TILT_SOURCES = listOf("shoulders", "head")

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
                    arg.startsWith("--control=") -> {
                        require(value.lowercase() in CONTROL_MODES) { "Mode de contrôle inconnu : '$value' (attendu : ${CONTROL_MODES.joinToString()})" }
                        options.copy(controlMode = value.uppercase())
                    }
                    arg.startsWith("--speed=") -> {
                        val speed = value.toFloatOrNull()
                        require(speed != null && speed in 0f..1f) { "Facteur de vitesse invalide : '$value' (attendu entre 0 et 1)" }
                        options.copy(speedFactor = value)
                    }
                    arg == "--debug-pose" -> options.copy(debugPose = true)
                    arg.startsWith("--screenshot-delay=") -> {
                        val delay = value.toFloatOrNull()
                        require(delay != null && delay > 0f) { "Délai de capture invalide : '$value'" }
                        options.copy(screenshotDelayS = delay)
                    }
                    arg.startsWith("--model=") -> options.copy(modelPath = value.ifBlank { null })
                    arg.startsWith("--tilt=") -> {
                        require(value.lowercase() in TILT_SOURCES) { "Inclinaison inconnue : '$value' (attendu : ${TILT_SOURCES.joinToString()})" }
                        options.copy(tiltSource = value.uppercase())
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
