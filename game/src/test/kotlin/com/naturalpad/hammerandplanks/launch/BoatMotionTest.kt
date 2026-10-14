package com.naturalpad.hammerandplanks.launch

import com.naturalpad.hammerandplanks.control.ControlSettings
import com.naturalpad.hammerandplanks.control.MovementControlMode
import com.naturalpad.hammerandplanks.input.GameInput
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoatMotionTest {
    private fun motion(mode: MovementControlMode, speed: Float = 1f) = BoatMotion(ControlSettings(mode, speed))

    private fun run(m: BoatMotion, axis: Float, seconds: Float) {
        repeat((seconds * 60).toInt()) { m.update(GameInput(axis), 1f / 60f) }
    }

    @Test fun `vitesse maximale reprise de l ancien jeu`() {
        // 6 unités/s sur un demi-rail de 16 unités.
        assertEquals(0.375f, BoatMotion().maxSpeed, 1e-6f)
        assertEquals(MovementControlMode.POSITION, BoatMotion().settings.mode)
    }

    @Test fun `immobile au centre au depart`() {
        val m = BoatMotion()
        assertEquals(0f, m.position); assertEquals(0f, m.velocity); assertEquals(0f, m.tiltDeg)
    }

    // --- Mode position (par défaut dans l'ancien jeu) ---

    @Test fun `position - la fleche gauche emmene le bateau vers le bord gauche`() {
        val m = motion(MovementControlMode.POSITION)
        run(m, -1f, 0.5f)
        assertTrue(m.position < 0f)
        assertTrue(m.tiltDeg > 0f, "Proue vers la gauche (rotation antihoraire)")
        run(m, -1f, 10f)
        assertEquals(-1f, m.position, 0.01f)
    }

    @Test fun `position - relacher ramene le bateau au centre`() {
        val m = motion(MovementControlMode.POSITION)
        run(m, 1f, 3f)
        run(m, 0f, 10f)
        assertEquals(0f, m.position, 0.01f)
    }

    @Test fun `position - jamais au dela de la cible`() {
        val m = motion(MovementControlMode.POSITION)
        repeat(600) { m.update(GameInput(1f), 1f / 60f); assertTrue(m.position <= 1f) }
    }

    // --- Mode vitesse ---

    @Test fun `vitesse - le bateau avance tant qu on appuie puis s arrete sur place`() {
        val m = motion(MovementControlMode.VELOCITY)
        run(m, 1f, 1f)
        val moved = m.position
        assertTrue(moved > 0f)
        run(m, 0f, 5f)
        assertTrue(abs(m.velocity) < 1e-3f, "Arrêté")
        assertTrue(m.position >= moved && m.position < 1f, "Reste là où il s'est arrêté, sans revenir au centre")
    }

    // --- Mode force ---

    @Test fun `force - relacher laisse le bateau sur sa lancee`() {
        val m = motion(MovementControlMode.FORCE)
        run(m, 1f, 0.8f)
        val speed = m.velocity
        assertTrue(speed > 0f)
        run(m, 0f, 0.5f)
        assertTrue(m.velocity > speed * 0.5f, "Garde de la vitesse sans appui")
    }

    @Test fun `force - pousser dans l autre sens freine puis repart`() {
        val m = motion(MovementControlMode.FORCE)
        run(m, 1f, 1f)
        run(m, -1f, 4f)
        assertTrue(m.velocity < 0f)
    }

    // --- Communs ---

    @Test fun `tous les modes butent sur le rail`() {
        for (mode in MovementControlMode.entries) {
            val m = motion(mode)
            run(m, -1f, 20f)
            assertTrue(m.position >= -1f && m.position < -0.9f, "$mode : position ${m.position}")
        }
    }

    @Test fun `le facteur de vitesse ralentit le bateau`() {
        val fast = motion(MovementControlMode.VELOCITY, 1f)
        val slow = motion(MovementControlMode.VELOCITY, 0.5f)
        run(fast, 1f, 1f); run(slow, 1f, 1f)
        assertTrue(slow.position < fast.position)
    }

    @Test fun `facteur de vitesse nul le bateau ne bouge pas`() {
        val m = motion(MovementControlMode.POSITION, 0f)
        run(m, 1f, 2f)
        assertEquals(0f, m.position, 1e-6f)
        assertEquals(0f, m.tiltDeg)
    }

    @Test fun `inclinaison bornee`() {
        for (mode in MovementControlMode.entries) {
            val m = motion(mode)
            repeat(300) { m.update(GameInput(1f), 1f / 60f); assertTrue(abs(m.tiltDeg) <= m.maxTiltDeg + 1e-4f) }
        }
    }

    @Test fun `une longue pause ne fait pas sauter le bateau`() {
        val m = motion(MovementControlMode.VELOCITY)
        m.update(GameInput(1f), 10f)
        assertTrue(m.position <= m.maxSpeed * BoatMotion.MAX_STEP_S + 1e-6f)
    }

    @Test fun `pas de temps nul sans effet`() {
        val m = BoatMotion()
        m.update(GameInput(1f), 0f)
        assertEquals(0f, m.position)
    }

    @Test fun `axe hors bornes ramene dans -1 1`() {
        val a = motion(MovementControlMode.VELOCITY); val b = motion(MovementControlMode.VELOCITY)
        run(a, 5f, 1f); run(b, 1f, 1f)
        assertEquals(b.position, a.position, 1e-5f)
    }

    @Test fun `changer de mode en cours de route`() {
        val m = motion(MovementControlMode.VELOCITY)
        run(m, 1f, 1f)
        m.settings = ControlSettings(MovementControlMode.POSITION)
        run(m, 0f, 10f)
        assertEquals(0f, m.position, 0.01f)
    }

    @Test fun `retour au centre`() {
        val m = BoatMotion()
        run(m, 1f, 1f)
        m.reset()
        assertEquals(0f, m.position); assertEquals(0f, m.velocity)
    }

    @Test fun `reglages invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { BoatMotion(maxSpeed = 0f) }
        assertFailsWith<IllegalArgumentException> { BoatMotion(smoothTimeS = 0f) }
    }
}
