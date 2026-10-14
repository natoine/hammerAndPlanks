package com.naturalpad.hammerandplanks.input

/**
 * Mode d'interaction courant et bascule cyclique (clavier, manette, caméra, puis de nouveau clavier).
 * Le choix peut être sauvegardé sous forme de texte (voir [storageKey]) et relu au lancement suivant.
 */
class InputModeSelector(
    initial: InputMode = DEFAULT,
    private val onChange: (InputMode) -> Unit = {},
) {
    var current: InputMode = initial
        private set

    /** Passe au mode suivant et le renvoie. */
    fun next(): InputMode = select(nextOf(current))

    /** Choisit [mode] ; ne notifie que s'il change. */
    fun select(mode: InputMode): InputMode {
        if (mode != current) {
            current = mode
            onChange(mode)
        }
        return current
    }

    /** Valeur à sauvegarder pour le mode courant. */
    val storageKey: String get() = current.name

    companion object {
        val DEFAULT: InputMode = InputMode.KEYBOARD

        fun nextOf(mode: InputMode): InputMode {
            val modes = InputMode.entries
            return modes[(mode.ordinal + 1) % modes.size]
        }

        /** Relit une valeur sauvegardée ; valeur absente ou inconnue (ancienne version, fichier abîmé) : mode par défaut. */
        fun parse(stored: String?): InputMode =
            InputMode.entries.firstOrNull { it.name == stored?.trim()?.uppercase() } ?: DEFAULT
    }
}
