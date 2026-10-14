package com.naturalpad.hammerandplanks.launch

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Animations de l'écran de lancement, en fonctions pures du temps écoulé (secondes). */
object LaunchAnimation {
    /** Vitesse de défilement de la mer, en fraction de la hauteur d'une tuile par seconde. */
    const val SEA_SPEED = 0.04f
    const val BOB_PERIOD_S = 3.2f
    const val BOB_AMPLITUDE = 0.015f
    const val FADE_IN_S = 1.2f

    /**
     * Décalage vertical de la mer, dans [0, tileHeight[, pour un défilement continu vers le bas
     * (le bateau semble remonter l'écran, comme dans le futur vertical shooter).
     */
    fun seaOffset(timeS: Float, tileHeight: Float): Float {
        require(tileHeight > 0f) { "tileHeight doit être positif" }
        val distance = timeS * SEA_SPEED * tileHeight
        return distance - floor(distance / tileHeight) * tileHeight
    }

    /** Houle : décalage vertical du bateau, proportionnel à sa taille. */
    fun bobOffset(timeS: Float, boatSize: Float): Float =
        sin(2.0 * PI * timeS / BOB_PERIOD_S).toFloat() * BOB_AMPLITUDE * boatSize

    /** Opacité des textes : de 0 à 1 pendant [FADE_IN_S], bornée. */
    fun fadeIn(timeS: Float, delayS: Float = 0f): Float =
        ((timeS - delayS) / FADE_IN_S).coerceIn(0f, 1f)
}
