package com.naturalpad.hammerandplanks.fx

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoatWakeTest {
    @Test fun `proue en haut et poupe en bas quand le bateau va tout droit`() {
        val (bx, by) = BoatWake.hullPoint(0f, 100f, BoatWake.BOW_RADIUS)
        val (sx, sy) = BoatWake.hullPoint(0f, 100f, -BoatWake.STERN_RADIUS)
        assertEquals(0f, bx, 1e-4f); assertEquals(0f, sx, 1e-4f)
        assertTrue(by > sy, "Proue au-dessus de la poupe à l'écran")
        assertTrue(sy < -30f, "Poupe nettement sous le centre (flottaison basse en vue 3/4) : $sy")
        assertEquals(BoatWake.WATERLINE_OFFSET_Y * 100f, (by * BoatWake.STERN_RADIUS + sy * BoatWake.BOW_RADIUS) / (BoatWake.BOW_RADIUS + BoatWake.STERN_RADIUS), 1e-3f)
    }

    @Test fun `cap a gauche la proue part a gauche et la poupe a droite`() {
        val (bx, _) = BoatWake.hullPoint(45f, 100f, BoatWake.BOW_RADIUS)
        val (sx, _) = BoatWake.hullPoint(45f, 100f, -BoatWake.STERN_RADIUS)
        assertTrue(bx < 0f && sx > 0f)
    }

    @Test fun `les flancs sont de part et d autre de la proue`() {
        val (lx, _) = BoatWake.sidePoint(0f, 100f, 0.2f, side = -1f)
        val (rx, _) = BoatWake.sidePoint(0f, 100f, 0.2f, side = 1f)
        assertTrue(lx < 0f && rx > 0f)
        assertEquals(-lx, rx, 1e-4f)
        // De profil (proue à gauche), les flancs sont l'un au-dessus de l'autre à l'écran.
        val (_, ly) = BoatWake.sidePoint(90f, 100f, 0f, side = -1f)
        val (_, ry) = BoatWake.sidePoint(90f, 100f, 0f, side = 1f)
        assertTrue(ly != ry)
    }

    @Test fun `la vue 3-4 raccourcit les distances verticales`() {
        val (_, up) = BoatWake.direction(0f)
        val (side, _) = BoatWake.direction(90f)
        assertEquals(BoatWake.FORESHORTENING, up, 1e-5f)
        assertEquals(-1f, side, 1e-5f)
    }

    @Test fun `le sillage nait a la poupe et derive avec la mer`() {
        val w = BoatWake(Random(1))
        w.update(0.5f, 0f, 0f, 100f, 0f, 0f, seaSpeedPx = 40f)
        assertTrue(w.wakeCount > 0 && w.splashCount > 0)
    }

    @Test fun `plus d ecume quand le bateau va vite`() {
        val slow = BoatWake(Random(1)); val fast = BoatWake(Random(1))
        repeat(30) {
            slow.update(1f / 60f, 0f, 0f, 100f, 0f, 0f, 40f)
            fast.update(1f / 60f, 0f, 0f, 100f, 30f, 1f, 40f)
        }
        assertTrue(fast.wakeCount > slow.wakeCount)
    }

    @Test fun `ecume videe`() {
        val w = BoatWake(Random(1))
        w.update(1f, 0f, 0f, 100f, 0f, 0f, 40f)
        w.clear()
        assertEquals(0, w.wakeCount); assertEquals(0, w.splashCount)
    }
}
