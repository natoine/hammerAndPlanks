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
        assertEquals(MovementControlMode.MOMENTUM, BoatMotion().settings.mode)
    }

    @Test fun `immobile au centre au depart`() {
        val m = BoatMotion()
        assertEquals(0f, m.position); assertEquals(0f, m.velocity); assertEquals(0f, m.tiltDeg)
    }

    // --- Mode élan (par défaut) ---

    /** Vitesses successives sur [seconds] de poussée continue. */
    private fun speeds(m: BoatMotion, axis: Float, seconds: Float): List<Float> {
        val out = mutableListOf<Float>()
        repeat((seconds * 60).toInt()) { m.update(GameInput(axis), 1f / 60f); out += m.velocity }
        return out
    }

    @Test fun `elan - l acceleration croit tant qu on pousse`() {
        val m = motion(MovementControlMode.MOMENTUM)
        val v = speeds(m, 1f, 0.6f)
        val early = v[10] - v[0]          // gain de vitesse sur les premiers 1/6 s
        val later = v[35] - v[25]         // même durée, plus tard
        assertTrue(later > early * 1.3f, "Accélération : début $early, plus tard $later")
    }

    @Test fun `elan - deux fois la vitesse de l ancien jeu, du centre au bord en 2 a 3 s`() {
        val m = motion(MovementControlMode.MOMENTUM)
        assertEquals(m.maxSpeed * 2f, m.momentum.maxSpeed, 1e-6f)
        run(m, 1f, 1f)
        assertTrue(m.position in 0.3f..0.6f, "Après 1 s : ${m.position}")
        run(m, 1f, 1.5f)
        assertTrue(m.position > 0.95f, "Après 2,5 s : ${m.position}")
    }

    @Test fun `elan - vitesse de pointe jamais depassee`() {
        val m = motion(MovementControlMode.MOMENTUM)
        repeat(300) { m.update(GameInput(1f), 1f / 60f); assertTrue(abs(m.velocity) <= m.momentum.maxSpeed + 1e-4f) }
    }

    @Test fun `elan - relacher le bateau glisse un peu puis revient au centre`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, -1f, 1f)
        val released = m.position
        run(m, 0f, 0.1f)
        assertTrue(m.position < released, "Glisse sur son élan")
        run(m, 0f, 4f)
        assertEquals(0f, m.position)
        assertEquals(0f, m.velocity)
    }

    /** Temps pour aller du centre à 0,95 en poussant, puis pour revenir du bord à 0,05 en relâchant. */
    private fun outAndBack(m: BoatMotion): Pair<Float, Float> {
        var out = 0f
        while (m.position < 0.95f) { m.update(GameInput(1f), 1f / 60f); out += 1f / 60f }
        repeat(120) { m.update(GameInput(1f), 1f / 60f) }
        var back = 0f
        while (m.position > 0.05f) {
            m.update(GameInput.IDLE, 1f / 60f); back += 1f / 60f
            assertTrue(back < 10f, "Pas de retour")
        }
        return out to back
    }

    @Test fun `elan - le retour au centre est un peu plus lent que l aller`() {
        val (out, back) = outAndBack(motion(MovementControlMode.MOMENTUM))
        assertTrue(back > out * 1.1f && back < out * 1.6f, "Aller $out s, retour $back s")
    }

    @Test fun `elan - vitesse de retour plafonnee sous la vitesse de pointe`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 1f, 4f)
        repeat(240) {
            m.update(GameInput.IDLE, 1f / 60f)
            if (m.velocity < 0f) assertTrue(-m.velocity <= m.momentum.maxSpeed * m.momentum.returnSpeedRatio + 1e-5f)
        }
    }

    @Test fun `elan - le plafond de retour suit le facteur de vitesse`() {
        val m = motion(MovementControlMode.MOMENTUM, speed = 0.5f)
        run(m, 1f, 8f)
        repeat(240) {
            m.update(GameInput.IDLE, 1f / 60f)
            assertTrue(-m.velocity <= m.momentum.maxSpeed * m.momentum.returnSpeedRatio * 0.5f + 1e-5f)
        }
    }

    @Test fun `elan - la glisse vers l exterieur n est pas bridee`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 1f, 1.2f)
        val speed = m.velocity
        assertTrue(speed > m.momentum.maxSpeed * m.momentum.returnSpeedRatio)
        m.update(GameInput.IDLE, 1f / 60f)
        assertTrue(m.velocity > m.momentum.maxSpeed * m.momentum.returnSpeedRatio * 0.9f, "Glisse ${m.velocity}")
    }

    @Test fun `elan - le retour au centre ne depasse pas le centre`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 1f, 1.5f)
        repeat(300) { m.update(GameInput.IDLE, 1f / 60f); assertTrue(m.position >= 0f, "Dépasse le centre : ${m.position}") }
    }

    @Test fun `elan - en poussant on peut rester sur un cote`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 1f, 4f)
        assertTrue(m.position > 0.9f)
    }

    @Test fun `elan - sans rappel le bateau reste ou il s arrete`() {
        val m = BoatMotion(ControlSettings(MovementControlMode.MOMENTUM), momentum = MomentumTuning(centering = 0f))
        run(m, 1f, 1f)
        run(m, 0f, 3f)
        assertTrue(m.position > 0.3f)
    }

    @Test fun `elan - pousser a contresens freine plus fort que relacher`() {
        val glide = motion(MovementControlMode.MOMENTUM); run(glide, 1f, 0.8f)
        val brake = motion(MovementControlMode.MOMENTUM); run(brake, 1f, 0.8f)
        run(glide, 0f, 0.1f); run(brake, -1f, 0.1f)
        assertTrue(brake.velocity < glide.velocity * 0.85f, "Contresens ${brake.velocity}, relâché ${glide.velocity}")
    }

    @Test fun `elan - changer de direction remet la poussee a zero`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 1f, 1f)
        assertTrue(m.holdTimeS > 0.9f)
        m.update(GameInput(-1f), 1f / 60f)
        assertEquals(0f, m.holdTimeS)
        m.update(GameInput(0f), 1f / 60f)
        assertEquals(0f, m.holdTimeS)
    }

    @Test fun `elan - axe analogique a mi course pousse moins`() {
        val full = motion(MovementControlMode.MOMENTUM); run(full, 1f, 0.3f)
        val half = motion(MovementControlMode.MOMENTUM); run(half, 0.5f, 0.3f)
        assertEquals(full.velocity / 2f, half.velocity, 0.05f)
    }

    @Test fun `elan - bruit d axe ignore`() {
        val m = motion(MovementControlMode.MOMENTUM)
        run(m, 0.03f, 1f)
        assertEquals(0f, m.position)
    }

    @Test fun `elan - rebond sur le bord`() {
        val m = motion(MovementControlMode.MOMENTUM, speed = 1f)
        run(m, 1f, 3f)                   // contre le bord droit, à pleine vitesse
        assertEquals(1f, m.position)
        val hit = BoatMotion(ControlSettings(MovementControlMode.MOMENTUM), momentum = MomentumTuning(centering = 0f, drag = 0f))
        run(hit, 1f, 3f)
        assertTrue(hit.velocity < 0f || hit.position < 1f, "Le bord renvoie le bateau")
    }

    @Test fun `elan - inclinaison maximale a la vitesse de pointe`() {
        val m = motion(MovementControlMode.MOMENTUM)
        repeat(60) { m.update(GameInput(-1f), 1f / 60f) }
        repeat(30) { m.update(GameInput(-1f), 1f / 60f) }
        assertTrue(m.tiltDeg > m.maxTiltDeg * 0.9f || m.position == -1f)
    }

    @Test fun `elan - facteur de vitesse applique`() {
        val m = motion(MovementControlMode.MOMENTUM, speed = 0.5f)
        run(m, 1f, 3f)
        assertTrue(abs(m.velocity) <= m.momentum.maxSpeed * 0.5f + 1e-4f)
    }

    @Test fun `elan - reglages invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { MomentumTuning(boost = 0.5f) }
        assertFailsWith<IllegalArgumentException> { MomentumTuning(bounce = 2f) }
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
        for (mode in MovementControlMode.entries - MovementControlMode.MOMENTUM) {
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

    @Test fun `en elan le bateau reste dans le rail`() {
        val m = motion(MovementControlMode.MOMENTUM)
        repeat(1200) { m.update(GameInput(if (it % 240 < 120) -1f else 1f), 1f / 60f); assertTrue(m.position in -1f..1f) }
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
