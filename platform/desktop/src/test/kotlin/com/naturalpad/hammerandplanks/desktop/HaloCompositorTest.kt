package com.naturalpad.hammerandplanks.desktop

import com.naturalpad.hammerandplanks.pose.HaloShape
import com.naturalpad.hammerandplanks.pose.HaloStroke
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HaloCompositorTest {
    private val w = 200
    private val h = 200
    private val gray = ByteArray(w * h * 3) { 100 }
    /** Une tête (cercle de rayon 40) au centre, halo de 20 px. */
    private val shape = HaloShape(listOf(HaloStroke(100f, 100f, 100f, 100f, 40f)), haloWidth = 20f, imageWidth = w, imageHeight = h)

    private fun pixel(img: ByteArray, x: Int, y: Int) = (0 until 3).map { img[(y * w + x) * 3 + it].toInt() and 0xFF }

    @Test fun `la personne elle-meme n est pas recouverte`() {
        val out = HaloCompositor.compose(gray, w, h, shape)
        assertEquals(listOf(100, 100, 100), pixel(out, 100, 100))
        assertEquals(listOf(100, 100, 100), pixel(out, 100, 70))
    }

    @Test fun `un liseré dore entoure la forme`() {
        val out = HaloCompositor.compose(gray, w, h, shape)
        val ring = pixel(out, 100, 100 - 44) // juste au-delà du rayon
        assertTrue(ring[0] > 140 && ring[0] > ring[2], "Liseré doré attendu : $ring")
    }

    @Test fun `le halo s estompe vers l exterieur`() {
        val out = HaloCompositor.compose(gray, w, h, shape)
        val near = pixel(out, 100, 100 - 43)[0]
        val far = pixel(out, 100, 100 - 58)[0]
        assertTrue(near > far, "Bord intérieur $near plus lumineux que bord extérieur $far")
        assertEquals(listOf(100, 100, 100), pixel(out, 5, 5)) // loin : intact
    }

    @Test fun `l image d origine n est pas modifiee`() {
        val copy = gray.copyOf()
        HaloCompositor.compose(gray, w, h, shape)
        assertContentEquals(copy, gray)
    }

    @Test fun `capsules allongees pour les bras`() {
        val arm = HaloShape(listOf(HaloStroke(40f, 100f, 160f, 100f, 10f)), 10f, w, h)
        val out = HaloCompositor.compose(gray, w, h, arm)
        assertEquals(listOf(100, 100, 100), pixel(out, 100, 100))      // sur le bras
        assertTrue(pixel(out, 100, 112)[0] > 120)                        // juste au-dessous
    }

    @Test fun `image trop petite refusee`() {
        assertFailsWith<IllegalArgumentException> { HaloCompositor.compose(ByteArray(10), w, h, shape) }
    }
}
