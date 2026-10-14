package com.naturalpad.hammerandplanks.pose

import kotlin.math.max
import kotlin.math.min

/**
 * Suivi de la zone de la personne sans détecteur (voir README, section 5) : la boîte de l'image suivante
 * est déduite des points 2D du haut du corps, élargie et lissée. Au départ, ou si le suivi est perdu,
 * on repart de l'image entière (le joueur cadré en buste remplit l'essentiel de l'image).
 */
class RoiTracker(
    /** Marge ajoutée autour des points, en fraction du côté de leur boîte (le buste continue sous les épaules). */
    private val margin: Float = 0.35f,
    /** Part de la nouvelle boîte à chaque image (lissage exponentiel contre le tremblement). */
    private val smoothing: Float = 0.5f,
    /** Côté minimal d'une boîte plausible, en fraction du petit côté de l'image. */
    private val minSideFraction: Float = 0.15f,
) {
    init {
        require(margin >= 0f && smoothing in 0f..1f && minSideFraction > 0f) { "Réglages de suivi invalides" }
    }

    private var box: Box? = null

    /** Boîte à utiliser pour l'image courante. */
    fun current(imageWidth: Int, imageHeight: Int): Box = box ?: Box.fullFrame(imageWidth, imageHeight)

    /** Met à jour la boîte à partir des points 2D (pixels de l'image, 70×2) de la dernière inférence. */
    fun update(joints2dImage: FloatArray, imageWidth: Int, imageHeight: Int) {
        var x1 = Float.MAX_VALUE; var y1 = Float.MAX_VALUE; var x2 = -Float.MAX_VALUE; var y2 = -Float.MAX_VALUE
        for (j in Mhr70.UPPER_BODY) {
            val x = joints2dImage[2 * j]; val y = joints2dImage[2 * j + 1]
            if (!x.isFinite() || !y.isFinite()) { reset(); return }
            x1 = min(x1, x); y1 = min(y1, y); x2 = max(x2, x); y2 = max(y2, y)
        }
        val side = max(x2 - x1, y2 - y1)
        val minSide = minSideFraction * min(imageWidth, imageHeight)
        val cx = (x1 + x2) / 2f
        val cy = (y1 + y2) / 2f
        val insideImage = cx in 0f..imageWidth.toFloat() && cy in 0f..imageHeight.toFloat()
        if (side < minSide || !insideImage) { reset(); return }
        // Le buste s'étend surtout sous les épaules : on élargit plus vers le bas.
        val m = side * margin
        val next = Box(x1 - m, y1 - m, x2 + m, y2 + m * 2f)
        val previous = box
        box = if (previous == null) next else Box(
            lerp(previous.x1, next.x1), lerp(previous.y1, next.y1),
            lerp(previous.x2, next.x2), lerp(previous.y2, next.y2),
        )
    }

    fun reset() {
        box = null
    }

    private fun lerp(a: Float, b: Float) = a + (b - a) * smoothing
}
