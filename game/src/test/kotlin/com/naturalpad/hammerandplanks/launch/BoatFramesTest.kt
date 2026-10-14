package com.naturalpad.hammerandplanks.launch

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BoatFramesTest {
    @Test fun `proue vers le haut donne l image 0`() = assertEquals(0, BoatFrames.indexForHeading(0f))
    @Test fun `proue a gauche donne l image 2`() = assertEquals(2, BoatFrames.indexForHeading(90f))
    @Test fun `proue en bas donne l image 4`() = assertEquals(4, BoatFrames.indexForHeading(180f))
    @Test fun `proue a droite donne l image 6`() = assertEquals(6, BoatFrames.indexForHeading(270f))
    @Test fun `arrondi a l image la plus proche`() {
        assertEquals(1, BoatFrames.indexForHeading(30f))
        assertEquals(0, BoatFrames.indexForHeading(22f))
        assertEquals(0, BoatFrames.indexForHeading(350f))
    }
    @Test fun `caps negatifs et superieurs a un tour`() {
        assertEquals(6, BoatFrames.indexForHeading(-90f))
        assertEquals(2, BoatFrames.indexForHeading(450f))
    }
    @Test fun `cap invalide refuse`() {
        assertFailsWith<IllegalArgumentException> { BoatFrames.indexForHeading(Float.NaN) }
    }
    @Test fun `chemin des images`() {
        assertEquals("boats/player/boat_player_34_00.png", BoatFrames.path(0))
        assertEquals("boats/player/boat_player_34_07.png", BoatFrames.path(7))
        assertFailsWith<IllegalArgumentException> { BoatFrames.path(8) }
    }
    @Test fun `toutes les images existent dans assets`() {
        val assets = File(System.getProperty("assetsDir") ?: "../assets")
        for (i in 0 until BoatFrames.FRAME_COUNT) {
            assertTrue(File(assets, BoatFrames.path(i)).isFile, "Image manquante : ${BoatFrames.path(i)}")
        }
    }
}
