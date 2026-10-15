package com.naturalpad.hammerandplanks.pose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerHaloTest {
    /** Joueur face caméra dans une image 640×480 : épaules de 200 px de large centrées en (320, 300). */
    private fun frame(shoulderWidth: Float = 200f): PoseFrame {
        val j2 = FloatArray(Mhr70.COUNT * 2) { 320f }
        fun put(i: Int, x: Float, y: Float) { j2[2 * i] = x; j2[2 * i + 1] = y }
        val h = shoulderWidth / 2f
        put(Mhr70.RIGHT_SHOULDER, 320f - h, 300f); put(Mhr70.LEFT_SHOULDER, 320f + h, 300f)
        put(Mhr70.RIGHT_ELBOW, 320f - h - 20f, 400f); put(Mhr70.LEFT_ELBOW, 320f + h + 20f, 400f)
        put(Mhr70.RIGHT_WRIST, 320f - h, 470f); put(Mhr70.LEFT_WRIST, 320f + h, 470f)
        put(Mhr70.RIGHT_HIP, 280f, 520f); put(Mhr70.LEFT_HIP, 360f, 520f)
        put(Mhr70.NOSE, 320f, 180f); put(Mhr70.RIGHT_EAR, 280f, 170f); put(Mhr70.LEFT_EAR, 360f, 170f)
        return PoseFrame(FloatArray(Mhr70.COUNT * 3), j2, 640, 480, 0.0)
    }

    @Test fun `forme faite de la tete, du cou, du tronc, des epaules et des bras`() {
        val shape = assertNotNull(PlayerHalo.shape(frame()))
        assertEquals(8, shape.strokes.size)
        assertEquals(640, shape.imageWidth); assertEquals(480, shape.imageHeight)
    }

    @Test fun `la tete est un cercle autour du visage`() {
        val head = PlayerHalo.shape(frame())!!.strokes[0]
        assertEquals(head.x1, head.x2); assertEquals(head.y1, head.y2)
        assertEquals(320f, head.x1, 1e-3f)
        assertTrue(head.radius >= 60f, "Rayon ${head.radius} : au moins les oreilles")
    }

    @Test fun `proportions et epaisseur du halo suivent la largeur des epaules`() {
        val small = PlayerHalo.shape(frame(100f))!!
        val big = PlayerHalo.shape(frame(200f))!!
        assertEquals(2f * small.haloWidth, big.haloWidth, 1e-3f)
        assertEquals(200f * PlayerHalo.HALO_WIDTH, big.haloWidth, 1e-3f)
        assertEquals(2f * small.strokes[2].radius, big.strokes[2].radius, 1e-3f) // tronc
    }

    @Test fun `le tronc va du milieu des epaules au milieu des hanches`() {
        val torso = PlayerHalo.shape(frame())!!.strokes[2]
        assertEquals(320f, torso.x1, 1e-3f); assertEquals(300f, torso.y1, 1e-3f)
        assertEquals(320f, torso.x2, 1e-3f); assertEquals(520f, torso.y2, 1e-3f)
    }

    @Test fun `pose inexploitable sans halo`() {
        assertNull(PlayerHalo.shape(frame(shoulderWidth = 2f)))
        val broken = frame().also { it.joints2d[2 * Mhr70.LEFT_WRIST] = Float.NaN }
        assertNull(PlayerHalo.shape(broken))
    }

    @Test fun `serialisation pour le web`() {
        val shape = PlayerHalo.shape(frame())!!
        val parts = PlayerHalo.encode(shape).split(';')
        assertEquals(1 + shape.strokes.size, parts.size)
        assertEquals("640,480,${shape.haloWidth}", parts[0])
        assertEquals(5, parts[1].split(',').size)
    }
}
