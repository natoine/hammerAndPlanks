package com.naturalpad.hammerandplanks.fx

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoatWakeTest {
    private val size = 500f // côté du sprite : bateau de 300 px de long, 1 unité de l'ancien jeu = 120 px

    @Test fun `vitesse convertie dans les unites de l ancien jeu, defilement compris`() {
        val w = BoatWake(Random(1))
        assertEquals(0f, w.oldGameSpeed(0f, 0f, size))
        assertEquals(1f, w.oldGameSpeed(0f, 120f, size), 1e-4f)      // la mer défile d'une unité par seconde
        assertEquals(5f, w.oldGameSpeed(360f, 480f, size), 1e-4f)    // 3-4-5 : vitesse latérale et défilement
    }

    @Test fun `debits a la vitesse de jeu de reference, meme si la mer defile lentement`() {
        val w = BoatWake(Random(1))
        repeat(60) { w.update(1f / 60f, 0f, 0f, size, 0f, 0f, seaSpeedPx = 10f) } // 1 s, bateau immobile, mer lente
        // Vitesse 2 : sillage 200/s vivant 1,5 s (environ 200 après 1 s), gerbes 800/s vivant 0,5 s + rafales.
        assertTrue(w.wakeCount in 180..220, "Sillage ${w.wakeCount}")
        assertTrue(w.splashCount > 380, "Gerbes ${w.splashCount}")
    }

    @Test fun `aller vite multiplie encore l ecume comme dans TrailAnim`() {
        val still = BoatWake(Random(1)); val fast = BoatWake(Random(1))
        repeat(60) {
            still.update(1f / 60f, 0f, 0f, size, 0f, 0f, 10f)
            fast.update(1f / 60f, 0f, 0f, size, 0f, lateralSpeedPx = 360f, seaSpeedPx = 10f) // 3 unités/s de côté
        }
        assertTrue(fast.wakeCount > still.wakeCount * 1.4f, "Sillage ${still.wakeCount} -> ${fast.wakeCount}")
    }

    @Test fun `rafale de 50 gerbes des la premiere seconde`() {
        val w = BoatWake(Random(1))
        repeat(70) { w.update(1f / 60f, 0f, 0f, size, 0f, 0f, 0f) }
        assertTrue(w.splashCount >= BoatWake.BURST_COUNT)
    }

    @Test fun `traits horizontaux comme l ancien sillage, sans rotation`() {
        val w = BoatWake(Random(1))
        w.update(0.5f, 0f, 0f, size, headingDeg = 30f, lateralSpeedPx = 100f, seaSpeedPx = 40f)
        assertTrue(w.wakeRotations().isNotEmpty())
        w.wakeRotations().forEach { assertEquals(0f, it) }
    }

    @Test fun `emetteurs a la position de l ancien bateau`() {
        // Gerbes à 0,712 / 2,5 de la longueur vers l'avant, sillage à 0,176 / 2,5.
        assertEquals(0.712f / 2.5f * 0.6f, BoatWake.SPLASH_ALONG, 1e-5f)
        assertEquals(0.176f / 2.5f * 0.6f, BoatWake.TRAIL_ALONG, 1e-5f)
        assertTrue(BoatWake.SPLASH_ALONG < BoatWake.BOW_RADIUS)
    }

    @Test fun `proue au-dessus de la poupe a l ecran quand le bateau va tout droit`() {
        val (bx, by) = BoatWake.hullPoint(0f, 100f, BoatWake.BOW_RADIUS)
        val (sx, sy) = BoatWake.hullPoint(0f, 100f, -BoatWake.STERN_RADIUS)
        assertEquals(0f, bx, 1e-4f); assertEquals(0f, sx, 1e-4f)
        assertTrue(by > sy)
        assertTrue(sy < -30f, "Flottaison basse en vue 3/4 : $sy")
    }

    @Test fun `cap a gauche la proue part a gauche et la poupe a droite`() {
        val (bx, _) = BoatWake.hullPoint(45f, 100f, BoatWake.BOW_RADIUS)
        val (sx, _) = BoatWake.hullPoint(45f, 100f, -BoatWake.STERN_RADIUS)
        assertTrue(bx < 0f && sx > 0f)
    }

    @Test fun `les flancs sont de part et d autre de l axe`() {
        val (lx, _) = BoatWake.sidePoint(0f, 100f, 0.2f, side = -1f)
        val (rx, _) = BoatWake.sidePoint(0f, 100f, 0.2f, side = 1f)
        assertTrue(lx < 0f && rx > 0f)
        assertEquals(-lx, rx, 1e-4f)
    }

    @Test fun `panneaux d ecume ecrases comme vus par la camera de l ancien jeu`() {
        assertEquals(kotlin.math.cos(Math.toRadians(57.8)).toFloat(), BoatWake.BILLBOARD_SQUASH, 0.01f)
    }

    @Test fun `la vue 3-4 raccourcit les distances verticales`() {
        val (_, up) = BoatWake.direction(0f)
        val (side, _) = BoatWake.direction(90f)
        assertEquals(BoatWake.FORESHORTENING, up, 1e-5f)
        assertEquals(-1f, side, 1e-5f)
    }

    @Test fun `ecume videe`() {
        val w = BoatWake(Random(1))
        w.update(1f, 0f, 0f, size, 0f, 0f, 40f)
        w.clear()
        assertEquals(0, w.wakeCount); assertEquals(0, w.splashCount)
    }
}
