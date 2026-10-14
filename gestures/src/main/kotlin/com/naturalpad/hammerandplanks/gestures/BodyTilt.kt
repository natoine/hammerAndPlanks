package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.pose.Mhr70
import com.naturalpad.hammerandplanks.pose.PoseFrame
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** Ligne du corps dont on mesure l'inclinaison latérale. */
enum class TiltSource {
    /** Ligne des épaules : suit l'inclinaison du buste. Par défaut. */
    SHOULDERS,

    /** Ligne des oreilles : suit l'inclinaison de la tête. */
    HEAD,
}

/** Inclinaison latérale du joueur, mesurée sur les articulations 3D d'InstantHMR (axes caméra, y vers le bas). */
object BodyTilt {
    /**
     * Ligne trop courte face à sa profondeur (joueur de profil) : angle non fiable.
     * Rapport minimal entre la longueur de la ligne dans le plan de l'image et sa longueur 3D.
     */
    const val MIN_FRONTAL_RATIO = 0.5f

    /**
     * Angle d'inclinaison en degrés, positif quand le joueur penche vers SA droite (son épaule ou son oreille
     * droite descend), négatif vers sa gauche ; null si la mesure n'est pas fiable.
     */
    fun lateralTiltDeg(frame: PoseFrame, source: TiltSource = TiltSource.SHOULDERS): Float? {
        val (left, right) = when (source) {
            TiltSource.SHOULDERS -> Mhr70.LEFT_SHOULDER to Mhr70.RIGHT_SHOULDER
            TiltSource.HEAD -> Mhr70.LEFT_EAR to Mhr70.RIGHT_EAR
        }
        // Face à la caméra, le côté gauche du joueur est à droite de l'image (x croissant).
        val dx = frame.x3(left) - frame.x3(right)
        val dy = frame.y3(left) - frame.y3(right)
        val dz = frame.z3(left) - frame.z3(right)
        if (!dx.isFinite() || !dy.isFinite() || !dz.isFinite()) return null
        val planar = hypot(dx, dy)
        val length = hypot(planar, dz)
        if (length == 0f || planar / length < MIN_FRONTAL_RATIO || dx <= 0f) return null
        // y vers le bas : si l'épaule droite descend, dy < 0 et l'angle image est négatif ; on change le signe.
        val rollDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        return -rollDeg
    }

    /** Vrai si l'angle est dans une plage plausible pour un buste ou une tête (moins de 60°). */
    fun plausible(tiltDeg: Float): Boolean = abs(tiltDeg) < 60f
}
