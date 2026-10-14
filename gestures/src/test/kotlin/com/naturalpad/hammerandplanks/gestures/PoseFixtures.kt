package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.pose.Mhr70
import com.naturalpad.hammerandplanks.pose.PoseFrame
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pose synthétique d'un joueur face à la caméra (axes caméra, y vers le bas) : sa gauche est à droite de l'image.
 * [shoulderTiltDeg] / [headTiltDeg] > 0 : il penche vers SA droite (épaule ou oreille droite plus basse).
 */
fun pose(shoulderTiltDeg: Float = 0f, headTiltDeg: Float = 0f, timeS: Double = 0.0, turnZ: Float = 0f): PoseFrame {
    val j = FloatArray(Mhr70.COUNT * 3)
    fun put(joint: Int, x: Float, y: Float, z: Float) { j[3 * joint] = x; j[3 * joint + 1] = y; j[3 * joint + 2] = z }
    fun line(left: Int, right: Int, halfWidth: Float, centerY: Float, tiltDeg: Float) {
        val a = Math.toRadians(tiltDeg.toDouble())
        val dx = (halfWidth * cos(a)).toFloat()
        val dy = (halfWidth * sin(a)).toFloat()
        // Penché vers sa droite : son côté droit (à gauche de l'image) descend, y augmente.
        put(right, -dx, centerY + dy, -turnZ)
        put(left, dx, centerY - dy, turnZ)
    }
    line(Mhr70.LEFT_SHOULDER, Mhr70.RIGHT_SHOULDER, 0.2f, 0f, shoulderTiltDeg)
    line(Mhr70.LEFT_EAR, Mhr70.RIGHT_EAR, 0.08f, -0.25f, headTiltDeg)
    return PoseFrame(j, FloatArray(Mhr70.COUNT * 2), 640, 480, timeS)
}
