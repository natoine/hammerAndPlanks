package com.naturalpad.hammerandplanks.pose

/**
 * Indices des articulations MHR70 utilisées par le jeu (ordre d'InstantHMR, instanthmr/skeleton.py).
 * « left » et « right » sont ceux du joueur, pas de l'image.
 */
object Mhr70 {
    const val COUNT = 70
    const val NOSE = 0
    const val LEFT_EYE = 1
    const val RIGHT_EYE = 2
    const val LEFT_EAR = 3
    const val RIGHT_EAR = 4
    const val LEFT_SHOULDER = 5
    const val RIGHT_SHOULDER = 6
    const val LEFT_ELBOW = 7
    const val RIGHT_ELBOW = 8
    const val LEFT_HIP = 9
    const val RIGHT_HIP = 10
    const val RIGHT_WRIST = 41
    const val LEFT_WRIST = 62
    const val LEFT_ACROMION = 67
    const val RIGHT_ACROMION = 68
    const val NECK = 69

    /** Articulations du haut du corps, visibles quand le joueur est cadré en buste : servent au suivi de la zone. */
    val UPPER_BODY = intArrayOf(
        NOSE, LEFT_EYE, RIGHT_EYE, LEFT_EAR, RIGHT_EAR, LEFT_SHOULDER, RIGHT_SHOULDER,
        LEFT_ELBOW, RIGHT_ELBOW, LEFT_ACROMION, RIGHT_ACROMION, NECK,
    )
}
