package com.naturalpad.hammerandplanks.pose

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PoseEstimatorTest {
    /** Faux modèle : tous les points 2D au centre du carré, sauf le haut du corps réparti, et mémorise ses entrées. */
    private class FakeModel : PoseModel {
        val cliffs = mutableListOf<FloatArray>()
        override fun run(image: FloatArray, cliff: FloatArray): ModelOutput {
            cliffs += cliff.copyOf()
            val j2 = FloatArray(Mhr70.COUNT * 2)
            Mhr70.UPPER_BODY.forEachIndexed { k, idx ->
                j2[2 * idx] = if (k % 2 == 0) -0.2f else 0.2f
                j2[2 * idx + 1] = if (k % 2 == 0) -0.2f else 0.1f
            }
            return ModelOutput(j2, FloatArray(Mhr70.COUNT * 3) { it * 0.01f })
        }
    }

    @Test fun `premiere image sur l image entiere puis zone suivie`() {
        val model = FakeModel()
        val estimator = PoseEstimator(model)
        val rgb = ByteArray(640 * 480 * 3)
        estimator.estimate(rgb, 640, 480, 0.0)
        assertContentEquals(PosePreprocessor.cliff(Box.fullFrame(640, 480), 640, 480), model.cliffs[0])
        estimator.estimate(rgb, 640, 480, 0.1)
        assertTrue(model.cliffs[1][2] < model.cliffs[0][2], "La zone suivie est plus petite que l'image")
    }

    @Test fun `la pose porte les points 3D du modele et les points 2D en pixels`() {
        val estimator = PoseEstimator(FakeModel())
        val frame = estimator.estimate(ByteArray(640 * 480 * 3), 640, 480, 2.5)
        assertEquals(2.5, frame.timestampS)
        assertEquals(0.01f, frame.joints3d[1], 1e-6f)
        // Centre du carré (point 2D 0,0) = centre de la boîte entière.
        assertEquals(320f, frame.joints2d[2 * Mhr70.LEFT_HIP], 1e-3f)
        assertEquals(240f, frame.joints2d[2 * Mhr70.LEFT_HIP + 1], 1e-3f)
    }

    @Test fun `reset revient a l image entiere`() {
        val model = FakeModel()
        val estimator = PoseEstimator(model)
        val rgb = ByteArray(640 * 480 * 3)
        estimator.estimate(rgb, 640, 480, 0.0)
        estimator.reset()
        estimator.estimate(rgb, 640, 480, 0.1)
        assertContentEquals(model.cliffs[0], model.cliffs[1])
    }
}
