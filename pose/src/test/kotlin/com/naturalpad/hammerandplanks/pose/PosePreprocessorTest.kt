package com.naturalpad.hammerandplanks.pose

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PosePreprocessorTest {
    private val n = PosePreprocessor.INPUT_SIZE

    private fun image(width: Int, height: Int, pixel: (Int, Int) -> Triple<Int, Int, Int>): ByteArray {
        val rgb = ByteArray(width * height * 3)
        for (y in 0 until height) for (x in 0 until width) {
            val (r, g, b) = pixel(x, y)
            val i = (y * width + x) * 3
            rgb[i] = r.toByte(); rgb[i + 1] = g.toByte(); rgb[i + 2] = b.toByte()
        }
        return rgb
    }

    private fun normalized(value: Int, channel: Int) =
        (value / 255f - PosePreprocessor.MEAN[channel]) / PosePreprocessor.STD[channel]

    @Test fun `carre 1,2 fois centre sur la boite`() {
        val crop = PosePreprocessor.crop(Box(100f, 50f, 300f, 450f))
        assertEquals(480f, crop.size, 1e-3f)       // côté max 400 × 1,2
        assertEquals(200f - 240f, crop.x1, 1e-3f)  // centre x 200
        assertEquals(250f - 240f, crop.y1, 1e-3f)  // centre y 250
    }

    @Test fun `conditionnement cliff en pixels normalises comme le Python`() {
        // Exemple du README des poids : bbox 120,40,380,600 dans une image 640x720.
        val cliff = PosePreprocessor.cliff(Box(120f, 40f, 380f, 600f), 640, 720)
        assertEquals(2f * 250f / 640f - 1f, cliff[0], 1e-6f)
        assertEquals(2f * 320f / 720f - 1f, cliff[1], 1e-6f)
        assertEquals(560f / 720f, cliff[2], 1e-6f)
    }

    @Test fun `image unie donne un tenseur uni normalise ImageNet`() {
        val rgb = image(320, 240) { _, _ -> Triple(200, 100, 50) }
        val out = FloatArray(3 * n * n)
        // Boîte entièrement dans l'image, carré compris.
        PosePreprocessor.fillTensor(rgb, 320, 240, PosePreprocessor.crop(Box(110f, 70f, 210f, 170f)), out)
        assertEquals(normalized(200, 0), out[0], 1e-5f)
        assertEquals(normalized(100, 1), out[n * n + 5000], 1e-5f)
        assertEquals(normalized(50, 2), out[2 * n * n + n * n - 1], 1e-5f)
    }

    @Test fun `hors de l image le carre est noir`() {
        val rgb = image(100, 100) { _, _ -> Triple(255, 255, 255) }
        val out = FloatArray(3 * n * n)
        // Carré qui déborde à gauche : la colonne de gauche du tenseur est noire, celle de droite blanche.
        PosePreprocessor.fillTensor(rgb, 100, 100, PosePreprocessor.Crop(-100f, 0f, 200f), out)
        assertEquals(normalized(0, 0), out[n * (n / 4)], 1e-5f)        // ligne vers y = 50, dans l image
        assertEquals(normalized(255, 0), out[n * (n / 4) + n - 1], 1e-5f)
    }

    @Test fun `orientation conservee haut gauche`() {
        // Quart haut gauche rouge, reste bleu.
        val rgb = image(224, 224) { x, y -> if (x < 112 && y < 112) Triple(255, 0, 0) else Triple(0, 0, 255) }
        val out = FloatArray(3 * n * n)
        PosePreprocessor.fillTensor(rgb, 224, 224, PosePreprocessor.Crop(0f, 0f, 224f), out)
        assertEquals(normalized(255, 0), out[10 * n + 10], 1e-5f)   // R en haut à gauche
        assertEquals(normalized(0, 0), out[200 * n + 200], 1e-5f)   // pas de R en bas à droite
    }

    @Test fun `copie exacte quand la piece fait deja 224 pixels`() {
        val rgb = image(224, 224) { x, y -> Triple(x, y, (x + y) / 2) }
        val out = FloatArray(3 * n * n)
        PosePreprocessor.fillTensor(rgb, 224, 224, PosePreprocessor.Crop(0f, 0f, 224f), out)
        assertEquals(normalized(37, 0), out[100 * n + 37], 1e-5f)
        assertEquals(normalized(100, 1), out[n * n + 100 * n + 37], 1e-5f)
    }

    @Test fun `pas de ligne avec padding`() {
        val stride = 10 * 3 + 2
        val rgb = ByteArray(stride * 10) { 0 }
        for (y in 0 until 10) for (x in 0 until 10) rgb[y * stride + x * 3] = 120.toByte()
        val out = FloatArray(3 * n * n)
        PosePreprocessor.fillTensor(rgb, 10, 10, PosePreprocessor.Crop(0f, 0f, 10f), out, rowStride = stride)
        assertEquals(normalized(120, 0), out[n * n / 2], 1e-5f)
    }

    @Test fun `points 2D ramenes en pixels de l image`() {
        val crop = PosePreprocessor.Crop(100f, 50f, 448f)
        val px = PosePreprocessor.jointsToImage(floatArrayOf(-1f, -1f, 1f, 1f, 0f, 0f), crop)
        assertContentEquals(floatArrayOf(100f, 50f, 548f, 498f, 324f, 274f), px)
    }

    @Test fun `entrees invalides refusees`() {
        assertFailsWith<IllegalArgumentException> { PosePreprocessor.crop(Box(10f, 10f, 10f, 20f)) }
        assertFailsWith<IllegalArgumentException> { PosePreprocessor.cliff(Box(0f, 0f, 1f, 1f), 0, 10) }
        assertFailsWith<IllegalArgumentException> {
            PosePreprocessor.fillTensor(ByteArray(10), 10, 10, PosePreprocessor.Crop(0f, 0f, 10f), FloatArray(3 * n * n))
        }
        assertFailsWith<IllegalArgumentException> {
            PosePreprocessor.fillTensor(ByteArray(300), 10, 10, PosePreprocessor.Crop(0f, 0f, 10f), FloatArray(10))
        }
    }
}
