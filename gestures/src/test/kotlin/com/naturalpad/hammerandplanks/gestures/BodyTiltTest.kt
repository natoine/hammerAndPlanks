package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.pose.Mhr70
import com.naturalpad.hammerandplanks.pose.PoseFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyTiltTest {
    @Test fun `buste droit`() = assertEquals(0f, BodyTilt.lateralTiltDeg(pose())!!, 1e-4f)

    @Test fun `pencher vers sa droite donne un angle positif`() =
        assertEquals(15f, BodyTilt.lateralTiltDeg(pose(shoulderTiltDeg = 15f))!!, 1e-3f)

    @Test fun `pencher vers sa gauche donne un angle negatif`() =
        assertEquals(-10f, BodyTilt.lateralTiltDeg(pose(shoulderTiltDeg = -10f))!!, 1e-3f)

    @Test fun `la tete se mesure sur la ligne des oreilles, independamment des epaules`() {
        val frame = pose(shoulderTiltDeg = 0f, headTiltDeg = 12f)
        assertEquals(12f, BodyTilt.lateralTiltDeg(frame, TiltSource.HEAD)!!, 1e-3f)
        assertEquals(0f, BodyTilt.lateralTiltDeg(frame, TiltSource.SHOULDERS)!!, 1e-3f)
    }

    @Test fun `rotation du buste vers la camera sans inclinaison laissee a zero`() =
        assertEquals(0f, BodyTilt.lateralTiltDeg(pose(turnZ = 0.1f))!!, 1e-3f)

    @Test fun `joueur de profil mesure non fiable`() = assertNull(BodyTilt.lateralTiltDeg(pose(turnZ = 0.5f)))

    @Test fun `joueur de dos ou image en miroir rejete`() {
        val j = FloatArray(Mhr70.COUNT * 3)
        j[3 * Mhr70.LEFT_SHOULDER] = -0.2f; j[3 * Mhr70.RIGHT_SHOULDER] = 0.2f
        assertNull(BodyTilt.lateralTiltDeg(PoseFrame(j, FloatArray(140), 640, 480, 0.0)))
    }

    @Test fun `valeurs non finies rejetees`() {
        val frame = pose()
        frame.joints3d[3 * Mhr70.LEFT_SHOULDER] = Float.NaN
        assertNull(BodyTilt.lateralTiltDeg(frame))
    }

    @Test fun `plausibilite`() {
        assertTrue(BodyTilt.plausible(30f))
        assertFalse(BodyTilt.plausible(-75f))
    }
}
