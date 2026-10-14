package com.naturalpad.hammerandplanks.util

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SmoothDampTest {
    private fun simulate(damp: SmoothDamp, from: Float, to: Float, maxSpeed: Float, seconds: Float, dt: Float = 1f / 60f): Float {
        var x = from
        repeat((seconds / dt).toInt()) { x = damp.step(x, to, maxSpeed, dt) }
        return x
    }

    @Test fun `rejoint la cible`() {
        assertEquals(1f, simulate(SmoothDamp(0.6f), 0f, 1f, 100f, 5f), 1e-3f)
    }

    @Test fun `en environ le temps de lissage on est proche de la cible`() {
        // Valeur de référence de Unity : à t = smoothTime, environ 59 % du chemin (amortissement critique).
        val x = simulate(SmoothDamp(0.6f), 0f, 1f, 100f, 0.6f)
        assertTrue(x in 0.5f..0.7f, "Avancement $x")
    }

    @Test fun `ne depasse jamais la cible`() {
        val damp = SmoothDamp(0.3f)
        var x = 0f
        repeat(600) { x = damp.step(x, 1f, 100f, 1f / 60f); assertTrue(x <= 1f) }
    }

    @Test fun `vitesse plafonnee`() {
        val damp = SmoothDamp(0.6f)
        var x = 0f
        repeat(120) {
            val before = x
            x = damp.step(x, 100f, 0.5f, 1f / 60f)
            assertTrue(abs(x - before) * 60f <= 0.5f + 1e-3f, "Vitesse ${(x - before) * 60f}")
        }
    }

    @Test fun `independant de la frequence d images`() {
        val a = simulate(SmoothDamp(0.6f), 0f, 1f, 100f, 1f, dt = 1f / 30f)
        val b = simulate(SmoothDamp(0.6f), 0f, 1f, 100f, 1f, dt = 1f / 120f)
        assertEquals(a, b, 0.02f)
    }

    @Test fun `pas de temps nul sans effet`() {
        val damp = SmoothDamp(0.6f)
        assertEquals(0.3f, damp.step(0.3f, 1f, 10f, 0f))
        assertEquals(0f, damp.velocity)
    }

    @Test fun `temps de lissage invalide refuse`() {
        assertFailsWith<IllegalArgumentException> { SmoothDamp(0f) }
    }
}
