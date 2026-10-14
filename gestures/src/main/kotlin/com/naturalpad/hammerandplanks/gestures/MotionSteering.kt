package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.pose.PoseFrame
import kotlin.math.abs
import kotlin.math.sign

/**
 * Mode caméra : l'inclinaison latérale du buste (ou de la tête) donne l'axe horizontal.
 * Pencher vers sa droite envoie le bateau à droite de l'écran (l'imagette est en miroir, comme un reflet).
 *
 * Étapes : mesure de l'angle ([BodyTilt]), calibration de la posture neutre sur les premières images
 * (joueur ou caméra un peu penchés), filtre One Euro, zone morte puis proportion jusqu'à [maxDeg].
 */
class MotionSteering(
    var source: TiltSource = TiltSource.SHOULDERS,
    /** Inclinaison ignorée autour de la posture neutre, en degrés. */
    val deadZoneDeg: Float = 3f,
    /** Inclinaison qui donne l'axe à fond, en degrés. */
    val maxDeg: Float = 20f,
    /** Nombre d'images utilisées pour mesurer la posture neutre. */
    val calibrationFrames: Int = 10,
    /** Au-delà, une pose est trop ancienne (caméra figée, personne sortie) : le bateau n'est plus piloté. */
    val staleAfterS: Double = 0.5,
    private val filter: OneEuroFilter = OneEuroFilter(minCutoff = 1.5f, beta = 0.02f),
) {
    init {
        require(deadZoneDeg >= 0f && maxDeg > deadZoneDeg) { "Zone morte et angle maximal incohérents" }
        require(calibrationFrames >= 0) { "Nombre d'images de calibration négatif" }
    }

    private var calibrationSum = 0f
    private var calibrationCount = 0
    private var lastFrameTime = Double.NaN
    private var lastInput = GameInput.IDLE

    /** Posture neutre mesurée, en degrés, ou null pendant la calibration. */
    var neutralDeg: Float? = null
        private set

    /** Dernière inclinaison filtrée par rapport à la posture neutre, en degrés (pour l'affichage et les tests). */
    var tiltDeg: Float = 0f
        private set

    /** GameInput pour la pose la plus récente [frame] (null si aucune), à l'instant [nowS]. */
    fun update(frame: PoseFrame?, nowS: Double): GameInput {
        if (frame == null || nowS - frame.timestampS > staleAfterS) {
            lastInput = GameInput.IDLE
            return lastInput
        }
        if (frame.timestampS == lastFrameTime) return lastInput // même image : rien de neuf
        lastFrameTime = frame.timestampS
        val raw = BodyTilt.lateralTiltDeg(frame, source)?.takeIf(BodyTilt::plausible) ?: return lastInput

        val neutral = neutralDeg ?: run {
            calibrationSum += raw
            calibrationCount++
            if (calibrationCount < calibrationFrames) return GameInput.IDLE.also { lastInput = it }
            (calibrationSum / calibrationCount).also { neutralDeg = it }
        }
        tiltDeg = filter.filter(raw - neutral, frame.timestampS)
        lastInput = GameInput(moveX = axis(tiltDeg))
        return lastInput
    }

    /** Axe [-1, 1] pour une inclinaison [deg] par rapport à la posture neutre. */
    fun axis(deg: Float): Float {
        val magnitude = ((abs(deg) - deadZoneDeg) / (maxDeg - deadZoneDeg)).coerceIn(0f, 1f)
        return sign(deg) * magnitude
    }

    /** Recommence la calibration (au passage en mode caméra, ou à la demande du joueur). */
    fun recalibrate() {
        neutralDeg = null
        calibrationSum = 0f
        calibrationCount = 0
        lastFrameTime = Double.NaN
        lastInput = GameInput.IDLE
        tiltDeg = 0f
        filter.reset()
    }
}
