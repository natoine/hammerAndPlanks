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

    @Test fun `rotation fixee par l emetteur, sans tirage aleatoire`() {
        val f = foam()
        f.emit(1f, 20f, 0f, 0f, 0f, 0f, life = 10f, size = 5f, rotationDeg = 20f)
        f.forEachAlive { assertEquals(20f, it.rotationDeg) }
        val g = foam()
        g.emit(1f, 20f, 0f, 0f, 0f, 0f, life = 10f, size = 5f)
        g.forEachAlive { assertEquals(0f, it.rotationDeg) }
    }

    @Test fun `naissance le long d un segment`() {
        val f = foam()
        f.emit(1f, 60f, 100f, 50f, 0f, 0f, life = 10f, size = 5f, lineX = 30f, lineY = 0f)
        var minX = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE
        f.forEachAlive {
            assertEquals(50f, it.y, 1e-4f)
            assertTrue(it.x in 70f..130f)
            minX = minOf(minX, it.x); maxX = maxOf(maxX, it.x)
        }
        assertTrue(maxX - minX > 40f, "Répartition sur le segment : $minX à $maxX")
    }

    @Test fun `opacite pleine puis effacement selon le style`() {
        val f = FoamParticles(style = FoamStyle(opaqueUntil = 0.3f))
        val p = FoamParticles.Particle().apply { life = 1f }
        p.age = 0f; assertEquals(1f, f.alphaOf(p))
        p.age = 0.3f; assertEquals(1f, f.alphaOf(p))
        p.age = 0.65f; assertEquals(0.5f, f.alphaOf(p), 1e-4f)
        p.age = 1f; assertEquals(0f, f.alphaOf(p))
    }

    @Test fun `taille du sillage grandit vite puis ralentit`() {
        val f = FoamParticles(style = FoamStyle(sizeStart = 0.46f, sizeEnd = 1f, easeOut = true))
        val p = FoamParticles.Particle().apply { life = 1f; size = 10f }
        assertEquals(4.6f, f.sizeOf(p), 1e-4f)
        p.age = 0.455f; assertEquals(9.1f, f.sizeOf(p), 0.1f) // ancienne courbe : 0,91 à 45 % de la vie
        p.age = 1f; assertEquals(10f, f.sizeOf(p), 1e-4f)
    }

    @Test fun `gerbes qui retrecissent et tournent`() {
        val f = FoamParticles(style = FoamStyle(sizeStart = 1f, sizeEnd = 0f, spinDegPerS = 45f))
        val p = FoamParticles.Particle().apply { life = 0.5f; size = 10f; rotationDeg = 20f }
        p.age = 0.25f
        assertEquals(5f, f.sizeOf(p), 1e-4f)
        assertEquals(20f + 45f * 0.25f, f.rotationOf(p), 1e-4f)
    }

    @Test fun `style invalide refuse`() {
        assertFailsWith<IllegalArgumentException> { FoamStyle(opaqueUntil = 1f) }
    }

    @Test fun `rafale et vitesse radiale qui ecarte du centre`() {
        val f = foam()
        f.burst(20, 0f, 0f, 0f, 0f, life = 10f, size = 5f, spread = 10f, radialSpeed = 5f)
        assertEquals(20, f.aliveCount)
        val before = mutableListOf<Float>(); f.forEachAlive { before += hypot(it.x, it.y) }
        f.update(1f)
        val after = mutableListOf<Float>(); f.forEachAlive { after += hypot(it.x, it.y) }
        assertTrue(after.zip(before).all { (a, b) -> a > b - 1e-3f })
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
