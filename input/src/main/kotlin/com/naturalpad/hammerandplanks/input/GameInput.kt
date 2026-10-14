package com.naturalpad.hammerandplanks.input

/**
 * Ce que le jeu reçoit, quel que soit le mode d'interaction (clavier, manette, caméra) :
 * le jeu ne connaît jamais le périphérique. Pour l'instant, l'axe horizontal seul ; tir, tir spécial,
 * interaction et pause viendront (voir README, section 4).
 */
data class GameInput(
    /** Axe horizontal dans [-1, 1] : -1 à gauche, 1 à droite. */
    val moveX: Float = 0f,
) {
    init {
        require(moveX.isFinite()) { "Axe invalide : $moveX" }
    }

    companion object {
        val IDLE = GameInput()
    }
}
