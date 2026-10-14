package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.input.GameInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MotionSteeringTest {
    /** Envoie [count] images à 30 i/s depuis [start] et renvoie la dernière entrée. */
    private fun feed(s: MotionSteering, tilt: Float, count: Int, start: Double = 0.0, source: TiltSource = TiltSource.SHOULDERS): GameInput {
        var input = GameInput.IDLE
        for (i in 0 until count) {
            val t = start + i / 30.0
            val frame = if (source == TiltSource.HEAD) pose(headTiltDeg = tilt, timeS = t) else pose(shoulderTiltDeg = tilt, timeS = t)
            input = s.update(frame, t)
        }
        return input
    }

    @Test fun `pas de pilotage pendant la calibration`() {
        val s = MotionSteering(calibrationFrames = 10)
        assertEquals(GameInput.IDLE, feed(s, 15f, 9))
        assertNull(s.neutralDeg)
    }

    @Test fun `pencher vers sa droite envoie le bateau a droite`() {
        val s = MotionSteering()
        feed(s, 0f, 10)
        val input = feed(s, 15f, 60, start = 1.0)
        assertTrue(input.moveX > 0.5f, "Axe ${input.moveX}")
    }

    @Test fun `pencher vers sa gauche envoie le bateau a gauche`() {
        val s = MotionSteering()
        feed(s, 0f, 10)
        assertTrue(feed(s, -15f, 60, start = 1.0).moveX < -0.5f)
    }

    @Test fun `au dela de l angle maximal l axe est a fond`() {
        val s = MotionSteering(maxDeg = 20f)
        feed(s, 0f, 10)
        assertEquals(1f, feed(s, 35f, 90, start = 1.0).moveX, 1e-3f)
    }

    @Test fun `petite inclinaison ignoree par la zone morte`() {
        val s = MotionSteering(deadZoneDeg = 3f)
        feed(s, 0f, 10)
        assertEquals(0f, feed(s, 2f, 60, start = 1.0).moveX)
    }

    @Test fun `la posture neutre penchee est compensee`() {
        // Caméra posée de travers : le joueur droit apparaît penché de 8°.
        val s = MotionSteering()
        feed(s, 8f, 10)
        assertEquals(8f, s.neutralDeg!!, 1e-3f)
        assertEquals(0f, feed(s, 8f, 30, start = 1.0).moveX)
        assertTrue(feed(s, 23f, 60, start = 2.0).moveX > 0.5f)
    }

    @Test fun `source tete`() {
        val s = MotionSteering(source = TiltSource.HEAD)
        feed(s, 0f, 10, source = TiltSource.HEAD)
        assertTrue(feed(s, 15f, 60, start = 1.0, source = TiltSource.HEAD).moveX > 0.5f)
    }

    @Test fun `pose trop ancienne ou absente le bateau n est plus pilote`() {
        val s = MotionSteering()
        feed(s, 0f, 10)
        feed(s, 15f, 60, start = 1.0)
        assertEquals(GameInput.IDLE, s.update(pose(shoulderTiltDeg = 15f, timeS = 3.0), nowS = 4.0))
        assertEquals(GameInput.IDLE, s.update(null, 4.0))
    }

    @Test fun `la meme image n est pas comptee deux fois`() {
        val s = MotionSteering(calibrationFrames = 2)
        val frame = pose(timeS = 1.0)
        s.update(frame, 1.0); s.update(frame, 1.01); s.update(frame, 1.02)
        assertNull(s.neutralDeg)
    }

    @Test fun `mesure non fiable garde la derniere entree`() {
        val s = MotionSteering()
        feed(s, 0f, 10)
        val before = feed(s, 15f, 60, start = 1.0)
        assertEquals(before, s.update(pose(turnZ = 0.5f, timeS = 3.0), 3.0))
    }

    @Test fun `recalibrer repart de zero`() {
        val s = MotionSteering()
        feed(s, 5f, 10)
        s.recalibrate()
        assertNull(s.neutralDeg)
        assertEquals(GameInput.IDLE, s.update(pose(shoulderTiltDeg = 20f, timeS = 5.0), 5.0))
    }

    @Test fun `axe proportionnel entre zone morte et maximum`() {
        val s = MotionSteering(deadZoneDeg = 4f, maxDeg = 24f)
        assertEquals(0.5f, s.axis(14f), 1e-5f)
        assertEquals(-0.5f, s.axis(-14f), 1e-5f)
        assertEquals(0f, s.axis(4f))
    }

    @Test fun `reglages incoherents refuses`() {
        assertFailsWith<IllegalArgumentException> { MotionSteering(deadZoneDeg = 10f, maxDeg = 5f) }
    }
}
