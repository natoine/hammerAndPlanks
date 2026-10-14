package com.naturalpad.hammerandplanks.control

/**
 * Façon dont l'axe d'entrée pilote le bateau. Position, vitesse et force sont reprises de l'ancien jeu
 * (MovementControlMode, PlayerHorizontalMovement) ; l'élan est le mode arcade par défaut.
 */
enum class MovementControlMode {
    /**
     * Élan (mode arcade, par défaut) : tant qu'on pousse dans la même direction, la poussée augmente ;
     * relâcher laisse glisser le bateau, pousser à contresens freine fort, les bords font rebondir.
     */
    MOMENTUM,

    /** L'axe désigne directement une position sur le rail : -1 bord gauche, 0 centre, 1 bord droit (ancien jeu). */
    POSITION,

    /** L'axe donne une vitesse : relâcher arrête le bateau (après lissage). */
    VELOCITY,

    /** L'axe donne une accélération : relâcher laisse le bateau sur sa lancée. */
    FORCE,
}
