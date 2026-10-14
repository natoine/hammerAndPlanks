package com.naturalpad.hammerandplanks.platform

import com.naturalpad.hammerandplanks.input.StickPosition

/**
 * Manettes : présence et stick gauche de la première manette. Implémenté par chaque lanceur
 * (API native de la plateforme).
 */
fun interface GamepadDetector {
    fun isConnected(): Boolean

    /** Position brute du stick gauche de la manette active (zone morte non appliquée), centre si aucune. */
    fun leftStick(): StickPosition = StickPosition.CENTER

    companion object {
        /** Plateforme sans détection : aucune manette. */
        val NONE = GamepadDetector { false }
    }
}
