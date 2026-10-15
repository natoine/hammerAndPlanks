package com.naturalpad.hammerandplanks.pose

import kotlin.math.hypot
import kotlin.math.max

/** Segment arrondi (capsule) en pixels de l'image caméra : de (x1, y1) à (x2, y2), rayon [radius]. */
data class HaloStroke(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val radius: Float)

/**
 * Forme approchée de la silhouette du joueur suivi, pour l'entourer d'un halo dans le retour caméra : quand
 * plusieurs personnes sont devant la caméra, on voit qui pilote. InstantHMR ne donne pas de contour : la forme
 * est faite de capsules posées sur les articulations du haut du corps (tête, cou, tronc, épaules, bras),
 * dimensionnées d'après la largeur des épaules. Les plateformes dessinent ces capsules élargies et floutées,
 * puis effacent l'intérieur : il reste un liseré lumineux autour de la personne.
 */
class HaloShape(
    val strokes: List<HaloStroke>,
    /** Épaisseur du halo autour de la forme, en pixels de l'image. */
    val haloWidth: Float,
    val imageWidth: Int,
    val imageHeight: Int,
)

object PlayerHalo {
    /** Épaisseur du halo, en fraction de la largeur des épaules. */
    const val HALO_WIDTH = 0.10f
    /** Couleur du halo : doré, comme les halos des bonus de l'ancien jeu (shader Halo2), et son opacité. */
    const val COLOR_R = 1f
    const val COLOR_G = 0.85f
    const val COLOR_B = 0.35f
    const val OPACITY = 0.55f

    /**
     * Forme du joueur pour la pose [frame], ou null si elle n'est pas exploitable (points non finis, épaules
     * confondues). Les proportions viennent de la largeur des épaules, mesure stable du buste.
     */
    fun shape(frame: PoseFrame): HaloShape? {
        val j = frame.joints2d
        fun x(i: Int) = j[2 * i]
        fun y(i: Int) = j[2 * i + 1]
        for (i in USED) if (!x(i).isFinite() || !y(i).isFinite()) return null
        val shoulderWidth = hypot(x(Mhr70.LEFT_SHOULDER) - x(Mhr70.RIGHT_SHOULDER), y(Mhr70.LEFT_SHOULDER) - y(Mhr70.RIGHT_SHOULDER))
        if (shoulderWidth < MIN_SHOULDER_PX) return null

        val midShoulderX = (x(Mhr70.LEFT_SHOULDER) + x(Mhr70.RIGHT_SHOULDER)) / 2f
        val midShoulderY = (y(Mhr70.LEFT_SHOULDER) + y(Mhr70.RIGHT_SHOULDER)) / 2f
        val midHipX = (x(Mhr70.LEFT_HIP) + x(Mhr70.RIGHT_HIP)) / 2f
        val midHipY = (y(Mhr70.LEFT_HIP) + y(Mhr70.RIGHT_HIP)) / 2f
        val headX = (x(Mhr70.LEFT_EAR) + x(Mhr70.RIGHT_EAR) + x(Mhr70.NOSE)) / 3f
        val headY = (y(Mhr70.LEFT_EAR) + y(Mhr70.RIGHT_EAR) + y(Mhr70.NOSE)) / 3f
        val earSpan = hypot(x(Mhr70.LEFT_EAR) - x(Mhr70.RIGHT_EAR), y(Mhr70.LEFT_EAR) - y(Mhr70.RIGHT_EAR))
        val headRadius = max(earSpan * 0.75f, shoulderWidth * 0.3f)
        val limb = shoulderWidth * 0.14f

        fun stroke(a: Int, b: Int, r: Float) = HaloStroke(x(a), y(a), x(b), y(b), r)
        val strokes = listOf(
            HaloStroke(headX, headY, headX, headY, headRadius),                              // tête
            HaloStroke(headX, headY, midShoulderX, midShoulderY, shoulderWidth * 0.18f),     // cou
            HaloStroke(midShoulderX, midShoulderY, midHipX, midHipY, shoulderWidth * 0.45f), // tronc
            stroke(Mhr70.LEFT_SHOULDER, Mhr70.RIGHT_SHOULDER, shoulderWidth * 0.16f),         // épaules
            stroke(Mhr70.LEFT_SHOULDER, Mhr70.LEFT_ELBOW, limb),
            stroke(Mhr70.LEFT_ELBOW, Mhr70.LEFT_WRIST, limb * 0.85f),
            stroke(Mhr70.RIGHT_SHOULDER, Mhr70.RIGHT_ELBOW, limb),
            stroke(Mhr70.RIGHT_ELBOW, Mhr70.RIGHT_WRIST, limb * 0.85f),
        )
        return HaloShape(strokes, shoulderWidth * HALO_WIDTH, frame.imageWidth, frame.imageHeight)
    }

    /** Sérialisation compacte « x1,y1,x2,y2,r;... » pour les plateformes qui dessinent en JavaScript. */
    fun encode(shape: HaloShape): String = buildString {
        append(shape.imageWidth).append(',').append(shape.imageHeight).append(',').append(shape.haloWidth)
        for (s in shape.strokes) append(';').append(s.x1).append(',').append(s.y1).append(',').append(s.x2).append(',').append(s.y2).append(',').append(s.radius)
    }

    /** Épaules plus proches que cela : pose non exploitable (personne trop loin ou de profil). */
    const val MIN_SHOULDER_PX = 8f

    private val USED = intArrayOf(
        Mhr70.NOSE, Mhr70.LEFT_EAR, Mhr70.RIGHT_EAR, Mhr70.LEFT_SHOULDER, Mhr70.RIGHT_SHOULDER,
        Mhr70.LEFT_ELBOW, Mhr70.RIGHT_ELBOW, Mhr70.LEFT_WRIST, Mhr70.RIGHT_WRIST, Mhr70.LEFT_HIP, Mhr70.RIGHT_HIP,
    )
}
