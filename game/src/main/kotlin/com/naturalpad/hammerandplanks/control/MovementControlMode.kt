package com.naturalpad.hammerandplanks.control

/**
 * Façon dont l'axe d'entrée pilote le bateau, reprise de l'ancien jeu (MovementControlMode, PlayerHorizontalMovement).
 */
enum class MovementControlMode {
    /** L'axe désigne directement une position sur le rail : -1 bord gauche, 0 centre, 1 bord droit. Par défaut. */
    POSITION,

    /** L'axe donne une vitesse : relâcher arrête le bateau (après lissage). */
    VELOCITY,

    /** L'axe donne une accélération : relâcher laisse le bateau sur sa lancée. */
    FORCE,
}
