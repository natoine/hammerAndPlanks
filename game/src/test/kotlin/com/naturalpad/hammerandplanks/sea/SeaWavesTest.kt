package com.naturalpad.hammerandplanks.sea

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SeaWavesTest {
    private val waves = SeaWaves()

    @Test fun `reglages par defaut repris de l ancien materiau`() {
        assertEquals(0.2f, waves.phaseSpeed)
        assertEquals(2f, waves.lacunarity)
        assertEquals(0.5f, waves.gain)
        assertEquals(1f, waves.foamBandPos)
        assertEquals(0.02f, waves.foamBandWidth)
        assertEquals(0.02f, waves.foamSmoothBand)
    }

    @Test fun `la phase avance avec le temps`() {
        assertEquals(0f, waves.phase(0f))
        assertEquals(2f, waves.phase(10f), 1e-4f)
    }

    @Test fun `la phase reste dans la periode du bruit apres des heures`() {
        for (t in listOf(1444f, 1445f, 36_000f, 1_000_000f)) {
            val p = waves.phase(t)
            assertTrue(p >= 0f && p < SeaWaves.NOISE_PERIOD, "Phase $p hors période à t=$t")
        }
    }

    @Test fun `le bruit defile avec la texture`() {
        assertEquals(0f, waves.noiseOffset(0f, 1000f))
        // Défiler d'une unité (petit côté de l'écran) avance le bruit de noiseScaleY unités.
        assertEquals(waves.noiseScaleY, waves.noiseOffset(1000f, 1000f), 1e-4f)
        val far = waves.noiseOffset(1e7f, 1000f)
        assertTrue(far >= 0f && far < SeaWaves.NOISE_PERIOD)
    }

    @Test fun `largeur d ecran invalide refusee`() {
        assertFailsWith<IllegalArgumentException> { waves.noiseOffset(10f, 0f) }
    }

    @Test fun `la parallaxe est proportionnelle a l ecran`() {
        assertEquals(2 * waves.parallaxPx(500f), waves.parallaxPx(1000f), 1e-4f)
    }

    @Test fun `ecume sur les cretes uniquement`() {
        assertEquals(1f, waves.foamWeight(1f), 1e-4f)      // crête : turbulence nulle
        assertEquals(0f, waves.foamWeight(0.9f))           // creux : pas d'écume
        assertEquals(0f, waves.foamWeight(0f))
        val edge = waves.foamWeight(0.97f)                 // dans la zone de lissage
        assertTrue(edge > 0f && edge < 1f)
    }

    @Test fun `wrapNoise ramene les valeurs negatives dans la periode`() {
        assertEquals(288f, SeaWaves.wrapNoise(-1f), 1e-3f)
    }

    @Test fun `reglages invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { SeaWaves(octaves = 0) }
        assertFailsWith<IllegalArgumentException> { SeaWaves(octaves = SeaWaves.MAX_OCTAVES + 1) }
        assertFailsWith<IllegalArgumentException> { SeaWaves(noiseScaleY = 0f) }
        assertFailsWith<IllegalArgumentException> { SeaWaves(parallaxFraction = -1f) }
        assertFailsWith<IllegalArgumentException> { SeaWaves(foamIntensity = 2f) }
    }
}
