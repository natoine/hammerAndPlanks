package com.naturalpad.hammerandplanks.launch

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Mise en page de l'écran de lancement, en pixels, origine en bas à gauche (convention libGDX).
 * Pensée mobile d'abord (portrait), elle s'adapte au paysage (desktop, navigateur) sans largeur fixe :
 * toutes les tailles sont des fractions de l'écran.
 */
data class LaunchLayout(
    val width: Int,
    val height: Int,
    val boatSize: Float,
    val boatCenterX: Float,
    val boatCenterY: Float,
    val titleFontPx: Int,
    val titleBaselineY: Float,
    val taglineFontPx: Int,
    val taglineBaselineY: Float,
    val versionFontPx: Int,
    val margin: Float,
    val textWidth: Float,
) {
    companion object {
        /** Taille minimale lisible d'une police, en pixels. */
        const val MIN_FONT_PX = 12

        fun compute(width: Int, height: Int): LaunchLayout {
            require(width > 0 && height > 0) { "Dimensions invalides : ${width}x$height" }
            val w = width.toFloat()
            val h = height.toFloat()
            val shortSide = min(w, h)
            val portrait = h >= w

            val margin = shortSide * 0.05f
            val textWidth = w - 2 * margin

            // Le sprite fait 512 px dont le bateau occupe environ 60 % : on vise un bateau visible
            // sans écraser le titre. En paysage la hauteur est la contrainte.
            val boatSize = if (portrait) min(w * 0.9f, h * 0.5f) else h * 0.55f
            val boatCenterX = w / 2f
            val boatCenterY = h * if (portrait) 0.40f else 0.38f

            val titlePx = font(if (portrait) w * 0.10f else h * 0.10f, h * 0.10f)
            val taglinePx = font(titlePx * 0.36f, h * 0.05f)
            val versionPx = font(shortSide * 0.025f, h * 0.04f)

            val titleBaselineY = h - margin - titlePx * 0.25f
            val taglineBaselineY = taglineBaselineBelow(titleBaselineY, titlePx.toFloat(), taglinePx)

            return LaunchLayout(
                width = width,
                height = height,
                boatSize = boatSize,
                boatCenterX = boatCenterX,
                boatCenterY = boatCenterY,
                titleFontPx = titlePx,
                titleBaselineY = titleBaselineY,
                taglineFontPx = taglinePx,
                taglineBaselineY = taglineBaselineY,
                versionFontPx = versionPx,
                margin = margin,
                textWidth = textWidth,
            )
        }

        /**
         * Ligne de base de l'accroche sous un bloc de titre de hauteur mesurée [titleBlockHeight]
         * (le titre peut passer sur deux lignes selon la langue et la largeur d'écran).
         */
        fun taglineBaselineBelow(titleBaselineY: Float, titleBlockHeight: Float, taglineFontPx: Int): Float {
            require(titleBlockHeight >= 0f) { "Hauteur de titre négative : $titleBlockHeight" }
            return titleBaselineY - titleBlockHeight - taglineFontPx * 0.9f
        }

        private fun font(target: Float, cap: Float): Int =
            max(MIN_FONT_PX, min(target, cap).roundToInt())
    }
}
