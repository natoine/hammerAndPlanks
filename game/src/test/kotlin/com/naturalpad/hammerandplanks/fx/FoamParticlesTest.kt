package com.naturalpad.hammerandplanks.fx

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FoamParticlesTest {
    private fun foam(capacity: Int = 64) = FoamParticles(capacity, Random(42))

    @Test fun `debit respecte avec report de la fraction`() {
        val f = foam()
        repeat(10) { f.emit(0.1f, 25f, 0f, 0f, 0f, 0f, life = 10f, size = 5f) } // 25 par seconde pendant 1 s
        assertEquals(25, f.aliveCount)
        f.emit(0.02f, 25f, 0f, 0f, 0f, 0f, life = 10f, size = 5f) // 0,5 particule : rien encore
        assertEquals(25, f.aliveCount)
    }

    @Test fun `les particules meurent au bout de leur vie`() {
        val f = foam()
        f.emit(1f, 10f, 0f, 0f, 0f, 0f, life = 1f, size = 5f)
        f.update(0.5f)
        assertEquals(10, f.aliveCount)
        f.update(1f) // vie aléatoire entre 0,8 et 1,2 s
        assertEquals(0, f.aliveCount)
    }

    @Test fun `capacite bornee, la plus vieille est recyclee`() {
        val f = foam(capacity = 8)
        f.emit(1f, 8f, 0f, 0f, 0f, 0f, life = 10f, size = 5f)
        f.update(1f)
        f.emit(1f, 3f, 100f, 100f, 0f, 0f, life = 10f, size = 5f)
        assertEquals(8, f.aliveCount)
        var fresh = 0
        f.forEachAlive { if (it.age == 0f) fresh++ }
        assertEquals(3, fresh)
    }

    @Test fun `vitesse et derive deplacent les particules`() {
        val f = foam()
        f.emit(1f, 1f, 0f, 0f, vx = 10f, vy = 0f, life = 10f, size = 5f)
        f.update(1f, driftY = -20f)
        f.forEachAlive { assertEquals(10f, it.x, 1e-3f); assertEquals(-20f, it.y, 1e-3f) }
    }

    @Test fun `dispersion dans le disque et le cone demandes`() {
        val f = foam()
        f.emit(1f, 50f, 100f, 100f, vx = 0f, vy = -10f, life = 10f, size = 5f, spread = 4f, coneDeg = 8f)
        f.forEachAlive {
            assertTrue(hypot(it.x - 100f, it.y - 100f) <= 4f + 1e-3f)
            val angle = Math.toDegrees(kotlin.math.atan2(it.vx.toDouble(), -it.vy.toDouble()))
            assertTrue(abs(angle) <= 8.001, "Angle $angle")
            assertEquals(10f, hypot(it.vx, it.vy), 1e-3f)
        }
    }

    @Test fun `opacite apparait puis s efface`() {
        val p = FoamParticles.Particle().apply { life = 1f }
        p.age = 0f; assertEquals(0f, p.alpha)
        p.age = 0.1f; assertEquals(0.9f, p.alpha, 1e-4f)
        p.age = 0.5f; val mid = p.alpha
        p.age = 0.9f; assertTrue(p.alpha < mid)
        p.age = 1f; assertEquals(0f, p.alpha)
    }

    @Test fun `la plaque s etale en vieillissant`() {
        val p = FoamParticles.Particle().apply { life = 1f; size = 10f }
        assertEquals(10f, p.currentSize)
        p.age = 1f
        assertEquals(10f * (1f + FoamParticles.GROWTH), p.currentSize, 1e-4f)
    }

    @Test fun `debit nul aucune particule`() {
        val f = foam()
        f.emit(5f, 0f, 0f, 0f, 0f, 0f, life = 1f, size = 1f)
        assertEquals(0, f.aliveCount)
    }

    @Test fun `vider`() {
        val f = foam()
        f.emit(1f, 10f, 0f, 0f, 0f, 0f, life = 5f, size = 1f)
        f.clear()
        assertEquals(0, f.aliveCount)
    }

    @Test fun `parametres invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { FoamParticles(0) }
        assertFailsWith<IllegalArgumentException> { foam().emit(1f, 1f, 0f, 0f, 0f, 0f, life = 0f, size = 1f) }
        assertFailsWith<IllegalArgumentException> { foam().emit(-1f, 1f, 0f, 0f, 0f, 0f, life = 1f, size = 1f) }
    }
}
