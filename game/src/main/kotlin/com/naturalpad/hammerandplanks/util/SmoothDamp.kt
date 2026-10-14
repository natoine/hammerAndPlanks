package com.naturalpad.hammerandplanks.util

import kotlin.math.max

/**
 * Amortissement critique vers une cible, portage exact de Mathf.SmoothDamp de Unity
 * (utilisé par l'ancien jeu pour le déplacement du bateau). [velocity] est l'état à conserver d'une image à l'autre.
 */
class SmoothDamp(
    /** Temps approximatif pour atteindre la cible, en secondes (ancien jeu : 0,6). */
    val smoothTimeS: Float,
) {
    init {
        require(smoothTimeS > 0f) { "Temps de lissage invalide : $smoothTimeS" }
    }

    var velocity: Float = 0f

    /** Nouvelle valeur après [deltaS] secondes, en allant de [current] vers [target] sans dépasser [maxSpeed] par seconde. */
    fun step(current: Float, target: Float, maxSpeed: Float, deltaS: Float): Float {
        if (deltaS <= 0f) return current
        val smoothTime = max(0.0001f, smoothTimeS)
        val omega = 2f / smoothTime
        val x = omega * deltaS
        val exp = 1f / (1f + x + 0.48f * x * x + 0.235f * x * x * x)
        val maxChange = maxSpeed * smoothTime
        val change = (current - target).coerceIn(-maxChange, maxChange)
        val clampedTarget = current - change
        val temp = (velocity + omega * change) * deltaS
        velocity = (velocity - omega * temp) * exp
        var output = clampedTarget + (change + temp) * exp
        // Empêche de dépasser la cible.
        if ((target - current > 0f) == (output > target)) {
            output = target
            velocity = (output - target) / deltaS
        }
        return output
    }

    fun reset() {
        velocity = 0f
    }
}
