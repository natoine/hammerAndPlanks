package com.naturalpad.hammerandplanks.launch

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Animations de l'écran de lancement, en fonctions pures du temps écoulé (secondes). */
object LaunchAnimation {
    /**
     * Défilement de la mer, en unités de l'ancien jeu par seconde (bateau de 2,5 de long) : 1, soit 0,4 longueur
     * de bateau par seconde. C'est ce défilement qui étire le sillage derrière le bateau. La moitié de la vitesse
     * minimale de l'ancien jeu (Settings.Game.MinFixedScrollSpeed = 2), jugée deux fois trop rapide au ressenti.
     */
    const val SEA_SCROLL_SPEED = 1f

    /** Vitesse de défilement de la mer en pixels par seconde, pour un sprite de bateau de côté [boatSize]. */
    fun seaScrollSpeedPx(boatSize: Float): Float =
        SEA_SCROLL_SPEED / com.naturalpad.hammerandplanks.fx.BoatWake.OLD_BOAT_LENGTH *
            com.naturalpad.hammerandplanks.fx.BoatWake.BOAT_LENGTH * boatSize
    const val BOB_PERIOD_S = 3.2f
    const val BOB_AMPLITUDE = 0.015f
    const val FADE_IN_S = 1.2f

    /**
     * Décalage vertical de la mer, dans [0, tileHeight[, après [scrolledPx] pixels de défilement vers le bas
     * (le bateau semble remonter l'écran, comme dans le futur vertical shooter).
     */
    fun seaOffset(scrolledPx: Float, tileHeight: Float): Float {
        require(tileHeight > 0f) { "tileHeight doit être positif" }
        return scrolledPx - floor(scrolledPx / tileHeight) * tileHeight
    }

    /** Houle : décalage vertical du bateau, proportionnel à sa taille. */
    fun bobOffset(timeS: Float, boatSize: Float): Float =
        sin(2.0 * PI * timeS / BOB_PERIOD_S).toFloat() * BOB_AMPLITUDE * boatSize

    /** Opacité des textes : de 0 à 1 pendant [FADE_IN_S], bornée. */
    fun fadeIn(timeS: Float, delayS: Float = 0f): Float =
        ((timeS - delayS) / FADE_IN_S).coerceIn(0f, 1f)
}
