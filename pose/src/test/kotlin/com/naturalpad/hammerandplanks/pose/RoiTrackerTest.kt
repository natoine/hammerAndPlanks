package com.naturalpad.hammerandplanks.pose

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RoiTrackerTest {
    /** Points du haut du corps groupés dans le rectangle donné, le reste au centre. */
    private fun joints(x1: Float, y1: Float, x2: Float, y2: Float): FloatArray {
        val j = FloatArray(Mhr70.COUNT * 2) { if (it % 2 == 0) (x1 + x2) / 2 else (y1 + y2) / 2 }
        Mhr70.UPPER_BODY.forEachIndexed { k, idx ->
            j[2 * idx] = if (k % 2 == 0) x1 else x2
            j[2 * idx + 1] = if (k % 3 == 0) y1 else y2
        }
        return j
    }

    @Test fun `au depart l image entiere`() = assertEquals(Box(0f, 0f, 640f, 480f), RoiTracker().current(640, 480))

    @Test fun `suit le haut du corps avec une marge plus grande vers le bas`() {
        val t = RoiTracker(margin = 0.25f, smoothing = 1f)
        t.update(joints(200f, 100f, 400f, 300f), 640, 480)
        val b = t.current(640, 480)
        assertEquals(150f, b.x1, 1e-3f); assertEquals(450f, b.x2, 1e-3f)
        assertEquals(50f, b.y1, 1e-3f); assertEquals(400f, b.y2, 1e-3f)
    }

    @Test fun `lissage entre deux images`() {
        val t = RoiTracker(margin = 0f, smoothing = 0.5f)
        t.update(joints(100f, 100f, 300f, 300f), 640, 480)
        t.update(joints(200f, 100f, 400f, 300f), 640, 480)
        assertEquals(150f, t.current(640, 480).x1, 1e-3f)
    }

    @Test fun `points trop serres ou hors image perdent le suivi`() {
        val t = RoiTracker(smoothing = 1f)
        t.update(joints(200f, 100f, 400f, 300f), 640, 480)
        t.update(joints(300f, 200f, 305f, 205f), 640, 480)
        assertEquals(Box.fullFrame(640, 480), t.current(640, 480))
        t.update(joints(200f, 100f, 400f, 300f), 640, 480)
        t.update(joints(2000f, 2000f, 2400f, 2300f), 640, 480)
        assertEquals(Box.fullFrame(640, 480), t.current(640, 480))
    }

    @Test fun `valeur non finie perd le suivi`() {
        val t = RoiTracker()
        val j = joints(200f, 100f, 400f, 300f).also { it[2 * Mhr70.NOSE] = Float.NaN }
        t.update(j, 640, 480)
        assertEquals(Box.fullFrame(640, 480), t.current(640, 480))
    }

    @Test fun `la boite suivie est toujours valide`() {
        val t = RoiTracker()
        t.update(joints(200f, 100f, 400f, 300f), 640, 480)
        assertTrue(t.current(640, 480).isValid)
    }

    @Test fun `reglages invalides refuses`() {
        assertFailsWith<IllegalArgumentException> { RoiTracker(smoothing = 2f) }
    }
}
