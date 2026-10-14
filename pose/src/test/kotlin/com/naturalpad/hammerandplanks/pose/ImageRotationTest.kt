package com.naturalpad.hammerandplanks.pose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ImageRotationTest {
    /** Image 3×2 RGBA où le rouge de chaque pixel vaut 10·y + x. */
    private fun image(stride: Int = 3 * 4): ByteArray {
        val rgba = ByteArray(stride * 2)
        for (y in 0 until 2) for (x in 0 until 3) {
            val s = y * stride + x * 4
            rgba[s] = (10 * y + x).toByte(); rgba[s + 1] = 7; rgba[s + 2] = 9; rgba[s + 3] = -1
        }
        return rgba
    }

    private fun red(img: ImageRotation.Rgb, x: Int, y: Int) = img.pixels[(y * img.width + x) * 3].toInt()

    @Test fun `sans rotation conversion RGBA vers RGB`() {
        val out = ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 0)
        assertEquals(3, out.width); assertEquals(2, out.height)
        assertEquals(12, red(out, 2, 1))
        assertEquals(7, out.pixels[1].toInt()); assertEquals(9, out.pixels[2].toInt())
    }

    @Test fun `rotation de 90 degres horaire`() {
        val out = ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 90)
        assertEquals(2, out.width); assertEquals(3, out.height)
        // Le coin haut gauche source (0) part en haut à droite ; le bas gauche (10) en haut à gauche.
        assertEquals(0, red(out, 1, 0)); assertEquals(10, red(out, 0, 0)); assertEquals(12, red(out, 0, 2))
    }

    @Test fun `rotation de 180 degres`() {
        val out = ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 180)
        assertEquals(12, red(out, 0, 0)); assertEquals(0, red(out, 2, 1))
    }

    @Test fun `rotation de 270 degres`() {
        val out = ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 270)
        assertEquals(2, out.width); assertEquals(3, out.height)
        assertEquals(2, red(out, 0, 0)); assertEquals(10, red(out, 1, 2))
    }

    @Test fun `lignes avec padding et rotation negative`() {
        val out = ImageRotation.rgbaToUprightRgb(image(stride = 16), 3, 2, 16, -90)
        assertEquals(2, red(out, 0, 0))
    }

    @Test fun `tampon reutilise si la taille convient`() {
        val buffer = ByteArray(18)
        val out = ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 90, reuse = buffer)
        assertEquals(true, out.pixels === buffer)
    }

    @Test fun `entrees invalides refusees`() {
        assertFailsWith<IllegalArgumentException> { ImageRotation.rgbaToUprightRgb(image(), 3, 2, 12, 45) }
        assertFailsWith<IllegalArgumentException> { ImageRotation.rgbaToUprightRgb(ByteArray(4), 3, 2, 12, 0) }
        assertFailsWith<IllegalArgumentException> { ImageRotation.rgbaToUprightRgb(image(), 3, 2, 8, 0) }
    }
}
