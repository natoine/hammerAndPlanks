package com.naturalpad.hammerandplanks.ui

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CameraThumbnailLayoutTest {
    @Test fun `en bas a droite au dessus de la ligne de version`() {
        val l = CameraThumbnailLayout.compute(540, 960, margin = 27f, versionLineHeight = 30f)
        assertEquals(540f - 27f, l.x + l.width, 0.01f)
        assertEquals(27f + 30f, l.y, 0.01f)
    }

    @Test fun `portrait en ecran portrait et paysage en ecran paysage`() {
        val portrait = CameraThumbnailLayout.compute(1080, 2400, 50f, 40f)
        assertTrue(portrait.height > portrait.width)
        val landscape = CameraThumbnailLayout.compute(1920, 1080, 50f, 40f)
        assertTrue(landscape.width > landscape.height)
    }

    @Test fun `reste dans l ecran pour toutes les tailles`() {
        for ((w, h) in listOf(360 to 640, 1080 to 2400, 1920 to 1080, 2400 to 1080, 768 to 1024)) {
            val l = CameraThumbnailLayout.compute(w, h, 0.05f * minOf(w, h), 30f)
            assertTrue(l.x >= 0f && l.y >= 0f && l.x + l.width <= w && l.y + l.height <= h, "Hors écran en ${w}x$h")
        }
    }

    @Test fun `zone normalisee origine en haut a gauche`() {
        val l = CameraThumbnailLayout(x = 300f, y = 100f, width = 200f, height = 400f, screenWidth = 1000, screenHeight = 1000)
        assertContentEquals(floatArrayOf(0.3f, 0.5f, 0.2f, 0.4f), l.normalized().map { Math.round(it * 1000) / 1000f }.toFloatArray())
    }

    @Test fun `recadrage cover d une image paysage dans une zone portrait`() {
        // 640x480 (4:3) dans 300x400 (3:4) : on garde la hauteur, on rogne les côtés.
        val uv = CameraThumbnailLayout.coverUv(640, 480, 300f, 400f, mirrored = false)
        assertEquals(0f, uv[1], 1e-4f); assertEquals(1f, uv[3], 1e-4f)
        val kept = uv[2] - uv[0]
        assertEquals((3f / 4f) / (4f / 3f), kept, 1e-4f)
        assertEquals(1f - uv[2], uv[0], 1e-4f) // centré
    }

    @Test fun `recadrage cover d une image haute dans une zone large`() {
        val uv = CameraThumbnailLayout.coverUv(480, 640, 400f, 300f, mirrored = false)
        assertEquals(0f, uv[0], 1e-4f); assertEquals(1f, uv[2], 1e-4f)
        assertTrue(uv[1] > 0f && uv[3] < 1f)
    }

    @Test fun `memes proportions sans recadrage`() {
        assertContentEquals(floatArrayOf(0f, 0f, 1f, 1f), CameraThumbnailLayout.coverUv(640, 480, 320f, 240f, false))
    }

    @Test fun `effet miroir echange gauche et droite`() {
        val normal = CameraThumbnailLayout.coverUv(640, 480, 300f, 400f, mirrored = false)
        val mirrored = CameraThumbnailLayout.coverUv(640, 480, 300f, 400f, mirrored = true)
        assertEquals(normal[0], mirrored[2]); assertEquals(normal[2], mirrored[0])
    }

    @Test fun `entrees invalides refusees`() {
        assertFailsWith<IllegalArgumentException> { CameraThumbnailLayout.compute(0, 100, 1f, 1f) }
        assertFailsWith<IllegalArgumentException> { CameraThumbnailLayout.compute(100, 100, -1f, 1f) }
        assertFailsWith<IllegalArgumentException> { CameraThumbnailLayout.coverUv(0, 480, 10f, 10f, false) }
    }
}
