package com.naturalpad.hammerandplanks.launch

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LaunchAnimationTest {
    @Test fun `la mer part de zero`() = assertEquals(0f, LaunchAnimation.seaOffset(0f, 1000f))

    @Test fun `la mer avance avec le temps`() {
        assertTrue(LaunchAnimation.seaOffset(2f, 1000f) > LaunchAnimation.seaOffset(1f, 1000f))
    }

    @Test fun `le decalage de la mer reste dans la tuile meme apres longtemps`() {
        for (t in listOf(0f, 1f, 24.9f, 25f, 1000f, 123456f)) {
            val o = LaunchAnimation.seaOffset(t, 1000f)
            assertTrue(o >= 0f && o < 1000f, "Décalage $o hors tuile à t=$t")
        }
    }

    @Test fun `la mer defile a 0,4 longueur de bateau par seconde`() {
        // 1 unité/s pour un bateau de 2,5 de long : 0,4 longueur de bateau par seconde ; le bateau fait 0,6 du sprite.
        assertEquals(0.4f * 0.6f * 500f, LaunchAnimation.seaScrollSpeedPx(500f), 1e-3f)
        assertEquals(2f * LaunchAnimation.seaScrollSpeedPx(250f), LaunchAnimation.seaScrollSpeedPx(500f), 1e-3f)
    }

    @Test fun `tuile de hauteur nulle refusee`() {
        assertFailsWith<IllegalArgumentException> { LaunchAnimation.seaOffset(1f, 0f) }
    }

    @Test fun `la houle est periodique et bornee`() {
        val size = 400f
        val p = LaunchAnimation.BOB_PERIOD_S
        assertTrue(abs(LaunchAnimation.bobOffset(0.7f, size) - LaunchAnimation.bobOffset(0.7f + p, size)) < 0.01f)
        for (i in 0..100) {
            assertTrue(abs(LaunchAnimation.bobOffset(i * 0.1f, size)) <= LaunchAnimation.BOB_AMPLITUDE * size + 0.001f)
        }
    }

    @Test fun `fondu borne entre 0 et 1`() {
        assertEquals(0f, LaunchAnimation.fadeIn(0f))
        assertEquals(0f, LaunchAnimation.fadeIn(0.5f, delayS = 1f))
        assertEquals(0.5f, LaunchAnimation.fadeIn(LaunchAnimation.FADE_IN_S / 2f), 0.001f)
        assertEquals(1f, LaunchAnimation.fadeIn(100f))
    }
}
