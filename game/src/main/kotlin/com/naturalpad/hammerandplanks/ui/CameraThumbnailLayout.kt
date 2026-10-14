package com.naturalpad.hammerandplanks.ui

import kotlin.math.min

/**
 * Imagette du retour caméra, en bas à droite, au-dessus de la ligne de version.
 * Format portrait (3:4) sur un écran portrait, paysage (4:3) sinon. Pixels, origine en bas à gauche.
 */
data class CameraThumbnailLayout(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val screenWidth: Int,
    val screenHeight: Int,
) {
    /** Même zone en fractions de l'écran, origine en haut à gauche, pour les vues natives (Android, web). */
    fun normalized(): FloatArray = floatArrayOf(
        x / screenWidth,
        1f - (y + height) / screenHeight,
        width / screenWidth,
        height / screenHeight,
    )

    companion object {
        fun compute(screenWidth: Int, screenHeight: Int, margin: Float, versionLineHeight: Float): CameraThumbnailLayout {
            require(screenWidth > 0 && screenHeight > 0) { "Dimensions invalides : ${screenWidth}x$screenHeight" }
            require(margin >= 0f && versionLineHeight >= 0f) { "Marge ou hauteur de ligne négative" }
            val shortSide = min(screenWidth, screenHeight).toFloat()
            val portrait = screenHeight >= screenWidth
            val width = shortSide * if (portrait) 0.3f else 0.36f
            val height = width * if (portrait) 4f / 3f else 3f / 4f
            return CameraThumbnailLayout(
                x = screenWidth - margin - width,
                y = margin + versionLineHeight,
                width = width,
                height = height,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
            )
        }

        /**
         * Coordonnées de texture (u, v, u2, v2) pour remplir une zone [targetW] x [targetH] avec une image
         * [sourceW] x [sourceH] sans la déformer, en rognant ce qui dépasse (équivalent de object-fit: cover).
         * [mirrored] échange u et u2 pour l'effet miroir.
         */
        fun coverUv(sourceW: Int, sourceH: Int, targetW: Float, targetH: Float, mirrored: Boolean): FloatArray {
            require(sourceW > 0 && sourceH > 0 && targetW > 0f && targetH > 0f) { "Dimensions invalides" }
            val sourceRatio = sourceW.toFloat() / sourceH
            val targetRatio = targetW / targetH
            var u = 0f; var u2 = 1f; var v = 0f; var v2 = 1f
            if (sourceRatio > targetRatio) {
                val keep = targetRatio / sourceRatio
                u = (1f - keep) / 2f; u2 = u + keep
            } else {
                val keep = sourceRatio / targetRatio
                v = (1f - keep) / 2f; v2 = v + keep
            }
            return if (mirrored) floatArrayOf(u2, v, u, v2) else floatArrayOf(u, v, u2, v2)
        }
    }
}
