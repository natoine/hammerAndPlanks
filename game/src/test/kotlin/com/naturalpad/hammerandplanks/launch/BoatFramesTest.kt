package com.naturalpad.hammerandplanks.launch

import java.io.File
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoatFramesTest {
    @Test fun `32 orientations de 11,25 degres`() {
        assertEquals(32, BoatFrames.FRAME_COUNT)
        assertEquals(11.25f, BoatFrames.STEP_DEG)
    }

    @Test fun `proue vers le haut donne l image 0`() = assertEquals(0, BoatFrames.indexForHeading(0f))
    @Test fun `proue a gauche donne l image 8`() = assertEquals(8, BoatFrames.indexForHeading(90f))
    @Test fun `proue en bas donne l image 16`() = assertEquals(16, BoatFrames.indexForHeading(180f))
    @Test fun `proue a droite donne l image 24`() = assertEquals(24, BoatFrames.indexForHeading(270f))

    @Test fun `diagonales a 45 degres`() {
        assertEquals(4, BoatFrames.indexForHeading(45f))
        assertEquals(28, BoatFrames.indexForHeading(-45f))
    }

    @Test fun `arrondi a l image la plus proche`() {
        assertEquals(1, BoatFrames.indexForHeading(10f))
        assertEquals(0, BoatFrames.indexForHeading(5f))
        assertEquals(0, BoatFrames.indexForHeading(-5f))
        assertEquals(31, BoatFrames.indexForHeading(-10f))
    }

    @Test fun `caps negatifs et superieurs a un tour`() {
        assertEquals(24, BoatFrames.indexForHeading(-90f))
        assertEquals(8, BoatFrames.indexForHeading(450f))
    }

    @Test fun `ancien decoupage en 8 images toujours calculable`() = assertEquals(2, BoatFrames.indexForHeading(90f, frameCount = 8))

    @Test fun `le reste de rotation ne depasse jamais une demi-etape`() {
        var heading = -60f
        while (heading <= 60f) {
            val r = BoatFrames.residualDeg(heading)
            assertTrue(abs(r) <= BoatFrames.STEP_DEG / 2f + 1e-3f, "Reste $r pour $heading")
            assertEquals(heading, BoatFrames.indexForHeading(heading).let { i -> (if (i > 16) i - 32 else i) * BoatFrames.STEP_DEG } + r, 1e-3f)
            heading += 0.7f
        }
    }

    @Test fun `images a charger pour un cap de plus ou moins 45 degres`() {
        assertEquals(listOf(28, 29, 30, 31, 0, 1, 2, 3, 4), BoatFrames.indicesWithin(45f))
        assertEquals(listOf(0), BoatFrames.indicesWithin(0f))
    }

    @Test fun `cap invalide refuse`() {
        assertFailsWith<IllegalArgumentException> { BoatFrames.indexForHeading(Float.NaN) }
        assertFailsWith<IllegalArgumentException> { BoatFrames.indicesWithin(-1f) }
    }

    @Test fun `chemin des images`() {
        assertEquals("boats/player/boat_player_34_00.png", BoatFrames.path(0))
        assertEquals("boats/player/boat_player_34_31.png", BoatFrames.path(31))
        assertFailsWith<IllegalArgumentException> { BoatFrames.path(32) }
    }

    @Test fun `toutes les images existent dans assets`() {
        val assets = File(System.getProperty("assetsDir") ?: "../assets")
        for (i in 0 until BoatFrames.FRAME_COUNT) {
            assertTrue(File(assets, BoatFrames.path(i)).isFile, "Image manquante : ${BoatFrames.path(i)}")
        }
    }
}
