package com.naturalpad.hammerandplanks.ui

import com.naturalpad.hammerandplanks.input.InputMode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Badge du mode d'interaction, en bas à gauche : picto, nom du mode, et comment en changer.
 * Tailles proportionnelles à l'écran, origine en bas à gauche (convention libGDX).
 */
data class InputModeBadgeLayout(
    val x: Float,
    val y: Float,
    val iconPx: Int,
    val labelPx: Int,
    val hintPx: Int,
    val padding: Float,
    val gap: Float,
    val cornerRadius: Int,
    /** Hauteur minimale de la zone tactile (recommandation d'accessibilité : environ 48 dp). */
    val minTouchHeight: Float,
) {
    /** Hauteur du badge pour un contenu de [contentHeight] pixels. */
    fun height(contentHeight: Float): Float = max(contentHeight + 2 * padding, minTouchHeight)

    /** Largeur du badge pour un texte de [textWidth] pixels. */
    fun width(textWidth: Float): Float = padding + iconPx + gap + textWidth + padding

    companion object {
        const val MIN_FONT_PX = 12

        fun compute(screenWidth: Int, screenHeight: Int, margin: Float): InputModeBadgeLayout {
            require(screenWidth > 0 && screenHeight > 0) { "Dimensions invalides : ${screenWidth}x$screenHeight" }
            require(margin >= 0f) { "Marge négative : $margin" }
            val shortSide = min(screenWidth, screenHeight).toFloat()
            val icon = max(20, (shortSide * 0.07f).roundToInt())
            val label = max(MIN_FONT_PX, (shortSide * 0.032f).roundToInt())
            val hint = max(MIN_FONT_PX, (shortSide * 0.024f).roundToInt())
            val padding = icon * 0.3f
            return InputModeBadgeLayout(
                x = margin,
                y = margin,
                iconPx = icon,
                labelPx = label,
                hintPx = hint,
                padding = padding,
                gap = padding,
                cornerRadius = max(2, (padding * 0.8f).roundToInt()),
                minTouchHeight = max(44f, shortSide * 0.11f),
            )
        }

        /** Picto du mode (PNG blanc 128 px, rendu par assets-src/ui/render_icons.py, teinté au dessin). */
        fun iconPath(mode: InputMode): String = "ui/input_" + mode.name.lowercase() + ".png"

        /** Clé de traduction du nom du mode. */
        fun labelKey(mode: InputMode): String = "input.mode." + mode.name.lowercase()
    }
}

/** Rectangle de détection d'appui, en pixels, origine en bas à gauche. */
data class HitBox(val x: Float, val y: Float, val width: Float, val height: Float) {
    fun contains(px: Float, py: Float): Boolean = px >= x && px <= x + width && py >= y && py <= y + height
}
