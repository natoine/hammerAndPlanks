package com.naturalpad.hammerandplanks.gestures

import kotlin.math.PI
import kotlin.math.abs

/**
 * Filtre One Euro (Casiez, Roussel, Vogel, CHI 2012) : lisse fortement un signal lent (pas de tremblement
 * au repos) et peu un signal rapide (peu de retard dans le mouvement). Adapté aux poses estimées.
 */
class OneEuroFilter(
    /** Fréquence de coupure au repos, en Hz : plus bas = plus lisse. */
    private val minCutoff: Float = 1f,
    /** Gain de vitesse : plus haut = moins de retard quand le signal bouge vite. */
    private val beta: Float = 0.05f,
    /** Coupure du filtre de la dérivée, en Hz. */
    private val derivativeCutoff: Float = 1f,
) {
    init {
        require(minCutoff > 0f && beta >= 0f && derivativeCutoff > 0f) { "Réglages du filtre invalides" }
    }

    private var lastTime = Double.NaN
    private var lastValue = 0f
    private var lastDerivative = 0f

    /** Valeur filtrée de [value] mesurée à l'instant [timeS] (secondes, croissant). */
    fun filter(value: Float, timeS: Double): Float {
        if (lastTime.isNaN()) {
            lastTime = timeS; lastValue = value; lastDerivative = 0f
            return value
        }
        val dt = (timeS - lastTime).toFloat()
        if (dt <= 0f) return lastValue // même image ou horloge qui recule : on garde la dernière valeur
        lastTime = timeS
        val derivative = (value - lastValue) / dt
        lastDerivative += alpha(derivativeCutoff, dt) * (derivative - lastDerivative)
        val cutoff = minCutoff + beta * abs(lastDerivative)
        lastValue += alpha(cutoff, dt) * (value - lastValue)
        return lastValue
    }

    fun reset() {
        lastTime = Double.NaN
    }

    private fun alpha(cutoff: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoff)
        return 1f / (1f + tau / dt)
    }
}
