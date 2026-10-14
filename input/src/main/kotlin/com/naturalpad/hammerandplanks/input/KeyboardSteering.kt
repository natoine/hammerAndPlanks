package com.naturalpad.hammerandplanks.input

/** Mode clavier : les flèches gauche et droite donnent un axe horizontal dans [-1, 1]. */
object KeyboardSteering {
    /** -1 à gauche, 1 à droite, 0 sans flèche ou avec les deux flèches enfoncées (elles s'annulent). */
    fun axis(leftPressed: Boolean, rightPressed: Boolean): Float = when {
        leftPressed == rightPressed -> 0f
        leftPressed -> -1f
        else -> 1f
    }

    /** Entrée de jeu produite par le clavier. */
    fun read(leftPressed: Boolean, rightPressed: Boolean): GameInput = GameInput(moveX = axis(leftPressed, rightPressed))
}
