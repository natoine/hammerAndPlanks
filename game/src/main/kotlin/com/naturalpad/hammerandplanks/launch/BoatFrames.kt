package com.naturalpad.hammerandplanks.launch

import kotlin.math.floor

/**
 * Correspondance cap du bateau / image du sprite en vue 3/4 (assets/boats/player/boat_player_34_NN.png),
 * 32 orientations rendues par Blender, une tous les 11,25°. Cap 0° : proue vers le haut de l'écran.
 * Les angles croissent dans le sens inverse des aiguilles d'une montre (08 : proue à gauche, 16 : en bas,
 * 24 : à droite). Entre deux images, le sprite est tourné du petit reste ([residualDeg], au plus 5,6°).
 */
object BoatFrames {
    const val FRAME_COUNT = 32
    const val STEP_DEG = 360f / FRAME_COUNT

    fun indexForHeading(headingDeg: Float, frameCount: Int = FRAME_COUNT): Int {
        require(frameCount > 0) { "frameCount doit être positif" }
        require(headingDeg.isFinite()) { "Cap invalide : $headingDeg" }
        val step = 360f / frameCount
        val normalized = headingDeg - floor(headingDeg / 360f) * 360f
        return (floor(normalized / step + 0.5f).toInt()) % frameCount
    }

    /** Rotation à appliquer au sprite de l'image la plus proche pour afficher exactement [headingDeg]. */
    fun residualDeg(headingDeg: Float): Float {
        val index = indexForHeading(headingDeg)
        var residual = headingDeg - index * STEP_DEG
        residual -= floor((residual + 180f) / 360f) * 360f // ramené dans [-180, 180[
        return residual
    }

    /** Images couvrant les caps de -[maxHeadingDeg] à +[maxHeadingDeg] : les seules à charger. */
    fun indicesWithin(maxHeadingDeg: Float): List<Int> {
        require(maxHeadingDeg in 0f..180f) { "Cap maximal invalide : $maxHeadingDeg" }
        val reach = kotlin.math.ceil(maxHeadingDeg / STEP_DEG).toInt()
        return (-reach..reach).map { ((it % FRAME_COUNT) + FRAME_COUNT) % FRAME_COUNT }.distinct()
    }

    fun path(index: Int): String {
        require(index in 0 until FRAME_COUNT) { "Index d'image hors limites : $index" }
        return "boats/player/boat_player_34_${index.toString().padStart(2, '0')}.png"
    }
}
