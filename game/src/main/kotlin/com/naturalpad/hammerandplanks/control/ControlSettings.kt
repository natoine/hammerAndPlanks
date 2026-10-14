package com.naturalpad.hammerandplanks.control

/**
 * Réglages de contrôle du bateau, avec les valeurs par défaut de l'ancien jeu
 * (Settings.ControlMode.xControlMode = Position, Settings.Player.velocity = 1 dans [0, 1]).
 */
data class ControlSettings(
    val mode: MovementControlMode = MovementControlMode.POSITION,
    /** Facteur de vitesse du joueur dans [0, 1] (réglage d'accessibilité et de difficulté). */
    val speedFactor: Float = 1f,
) {
    init {
        require(speedFactor in 0f..1f) { "Facteur de vitesse hors de [0, 1] : $speedFactor" }
    }

    companion object {
        /** Relit des valeurs sauvegardées ou passées en option ; valeur absente ou invalide : valeur par défaut. */
        fun parse(mode: String?, speedFactor: String?): ControlSettings {
            val parsedMode = MovementControlMode.entries.firstOrNull { it.name == mode?.trim()?.uppercase() }
                ?: MovementControlMode.POSITION
            val parsedSpeed = speedFactor?.trim()?.toFloatOrNull()?.takeIf { it.isFinite() && it in 0f..1f } ?: 1f
            return ControlSettings(parsedMode, parsedSpeed)
        }
    }
}
