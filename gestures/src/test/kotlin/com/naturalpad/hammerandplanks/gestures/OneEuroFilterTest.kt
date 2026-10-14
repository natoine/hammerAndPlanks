package com.naturalpad.hammerandplanks.gestures

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class OneEuroFilterTest {
    @Test fun `premiere valeur transmise telle quelle`() = assertEquals(5f, OneEuroFilter().filter(5f, 0.0))

    @Test fun `signal constant reste constant`() {
        val f = OneEuroFilter()
        repeat(30) { assertEquals(3f, f.filter(3f, it / 30.0), 1e-5f) }
    }

    @Test fun `le tremblement est attenue`() {
        val f = OneEuroFilter(minCutoff = 1f, beta = 0f)
        var maxOut = 0f
        for (i in 1..300) {
            val noisy = if (i % 2 == 0) 1f else -1f
            val out = f.filter(noisy, i / 30.0)
            if (i > 30) maxOut = maxOf(maxOut, abs(out)) // après amorçage (la première valeur passe telle quelle)
        }
        assertTrue(maxOut < 0.6f, "Amplitude filtrée $maxOut")
    }

    @Test fun `un changement franc est suivi`() {
        val f = OneEuroFilter()
        f.filter(0f, 0.0)
        var out = 0f
        for (i in 1..60) out = f.filter(10f, i / 30.0)
        assertEquals(10f, out, 0.1f)
    }

    @Test fun `beta reduit le retard sur un mouvement rapide`() {
        fun lagAfterRamp(beta: Float): Float {
            val f = OneEuroFilter(minCutoff = 0.5f, beta = beta)
            var out = 0f
            for (i in 0..15) out = f.filter(i * 2f, i / 30.0)
            return 30f - out
        }
        assertTrue(lagAfterRamp(0.5f) < lagAfterRamp(0f))
    }

    @Test fun `temps qui n avance pas sans effet`() {
        val f = OneEuroFilter()
        f.filter(1f, 1.0)
        assertEquals(1f, f.filter(50f, 1.0))
        assertEquals(1f, f.filter(50f, 0.5))
    }

    @Test fun `reset repart de zero`() {
        val f = OneEuroFilter()
        f.filter(1f, 0.0); f.filter(1f, 0.1)
        f.reset()
        assertEquals(7f, f.filter(7f, 0.2))
    }

    @Test fun `reglages invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { OneEuroFilter(minCutoff = 0f) }
    }
}
