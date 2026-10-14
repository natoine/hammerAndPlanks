package com.naturalpad.hammerandplanks.input

import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/** Position brute d'un stick analogique, chaque axe dans [-1, 1] (y vers le haut ou le bas selon la plateforme, sans effet ici). */
data class StickPosition(val x: Float = 0f, val y: Float = 0f) {
    companion object {
        val CENTER = StickPosition()
    }
}

/**
 * Mode manette : le stick gauche donne l'axe horizontal, comme dans l'ancien jeu
 * (axe Unity « X_0 » : zone morte de 0,15 par axe, puis vecteur 2D borné à une longueur de 1
 * dans PlayerHorizontalMovement.getLateralAxisInput).
 */
object GamepadSteering {
    /** Zone morte de l'ancien jeu (ProjectSettings/InputManager.asset, axes X_0 et Y_0). */
    const val DEAD_ZONE = 0.15f

    /**
     * Zone morte façon Unity : sous [deadZone] la valeur vaut 0, au-delà elle est remise à l'échelle
     * pour repartir de 0 et atteindre 1 en butée (pas de saut à la sortie de la zone morte).
     */
    fun applyDeadZone(value: Float, deadZone: Float = DEAD_ZONE): Float {
        require(deadZone in 0f..<1f) { "Zone morte invalide : $deadZone" }
        if (!value.isFinite()) return 0f
        val clamped = value.coerceIn(-1f, 1f)
        if (abs(clamped) < deadZone) return 0f
        return sign(clamped) * (abs(clamped) - deadZone) / (1f - deadZone)
    }

    /** Entrée de jeu produite par le stick. */
    fun read(stick: StickPosition, deadZone: Float = DEAD_ZONE): GameInput {
        val x = applyDeadZone(stick.x, deadZone)
        val y = applyDeadZone(stick.y, deadZone)
        // Comme l'ancien jeu : en diagonale, le vecteur est ramené à une longueur de 1 au plus.
        val length = sqrt(x * x + y * y)
        val moveX = if (length > 1f) x / length else x
        return GameInput(moveX = moveX)
    }
}
