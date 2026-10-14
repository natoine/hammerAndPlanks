package com.naturalpad.hammerandplanks.pose

import kotlin.math.max

/** Rectangle en pixels de l'image, origine en haut à gauche (convention des images et d'InstantHMR). */
data class Box(val x1: Float, val y1: Float, val x2: Float, val y2: Float) {
    val width: Float get() = x2 - x1
    val height: Float get() = y2 - y1
    val centerX: Float get() = (x1 + x2) / 2f
    val centerY: Float get() = (y1 + y2) / 2f
    val side: Float get() = max(width, height)
    val isValid: Boolean get() = x1.isFinite() && y1.isFinite() && x2.isFinite() && y2.isFinite() && width > 0f && height > 0f

    companion object {
        fun fullFrame(width: Int, height: Int) = Box(0f, 0f, width.toFloat(), height.toFloat())
    }
}
