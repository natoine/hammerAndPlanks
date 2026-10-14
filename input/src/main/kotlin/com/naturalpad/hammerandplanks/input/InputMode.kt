package com.naturalpad.hammerandplanks.input

/**
 * Modes d'interaction proposés au joueur. L'ordre de déclaration est l'ordre de bascule (touche Tab).
 * Le jeu ne dépend d'aucun mode en particulier : chacun ne produira qu'un GameInput.
 */
enum class InputMode {
    /** Clavier : flèches gauche et droite. Mode par défaut. */
    KEYBOARD,

    /** Manette : joystick. */
    GAMEPAD,

    /** Caméra : capture de mouvement du corps. */
    MOTION,
}
