package com.naturalpad.hammerandplanks.launch

import kotlin.math.floor

/**
 * Correspondance cap du bateau / image du sprite en vue 3/4 (assets/boats/player/boat_player_34_NN.png).
 * Cap 0° : proue vers le haut de l'écran. Les angles croissent dans le sens inverse des aiguilles d'une montre,
 * comme les images rendues par Blender (02 : proue à gauche, 04 : en bas, 06 : à droite).
 */
object BoatFrames {
    const val FRAME_COUNT = 8

    fun indexForHeading(headingDeg: Float, frameCount: Int = FRAME_COUNT): Int {
        require(frameCount > 0) { "frameCount doit être positif" }
        require(headingDeg.isFinite()) { "Cap invalide : $headingDeg" }
        val step = 360f / frameCount
        val normalized = headingDeg - floor(headingDeg / 360f) * 360f
        return (floor(normalized / step + 0.5f).toInt()) % frameCount
    }

    fun path(index: Int): String {
        require(index in 0 until FRAME_COUNT) { "Index d'image hors limites : $index" }
        return "boats/player/boat_player_34_${index.toString().padStart(2, '0')}.png"
    }
}
