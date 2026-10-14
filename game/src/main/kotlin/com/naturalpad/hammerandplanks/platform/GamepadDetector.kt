package com.naturalpad.hammerandplanks.platform

/** Dit si au moins une manette est branchée. Implémenté par chaque lanceur (API native de la plateforme). */
fun interface GamepadDetector {
    fun isConnected(): Boolean

    companion object {
        /** Plateforme sans détection : aucune manette. */
        val NONE = GamepadDetector { false }
    }
}
