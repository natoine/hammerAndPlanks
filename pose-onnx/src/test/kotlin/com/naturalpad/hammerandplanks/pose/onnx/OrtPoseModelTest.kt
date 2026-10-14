package com.naturalpad.hammerandplanks.pose.onnx

import com.naturalpad.hammerandplanks.pose.Box
import com.naturalpad.hammerandplanks.pose.PoseEstimator
import com.naturalpad.hammerandplanks.pose.PosePreprocessor
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Test d'intégration sur le vrai modèle ; ignoré si models/instanthmr.onnx n'est pas téléchargé. */
class OrtPoseModelTest {
    private val modelFile = File(System.getProperty("modelPath") ?: "../models/instanthmr.onnx")

    @Test fun `le modele publie s execute et renvoie 70 points finis`() {
        assumeTrue(modelFile.isFile, "Modèle absent : ${modelFile.path}")
        OrtPoseModel(modelFile.path).use { model ->
            val image = FloatArray(3 * PosePreprocessor.INPUT_SIZE * PosePreprocessor.INPUT_SIZE)
            val out = model.run(image, PosePreprocessor.cliff(Box.fullFrame(640, 480), 640, 480))
            assertTrue(out.joints2dNorm.all { it.isFinite() })
            assertTrue(out.joints3d.all { it.isFinite() })
        }
    }

    @Test fun `chaine complete sur une image grise et temps d inference`() {
        assumeTrue(modelFile.isFile, "Modèle absent : ${modelFile.path}")
        OrtPoseModel(modelFile.path).use { model ->
            val estimator = PoseEstimator(model)
            val rgb = ByteArray(640 * 480 * 3) { 120 }
            estimator.estimate(rgb, 640, 480, 0.0) // chauffe
            val runs = 5
            val start = System.nanoTime()
            repeat(runs) { estimator.estimate(rgb, 640, 480, it.toDouble()) }
            val ms = (System.nanoTime() - start) / 1e6 / runs
            println("InstantHMR, ONNX Runtime CPU, ${OrtPoseModel.DEFAULT_THREADS} threads : %.1f ms par image".format(ms))
            assertTrue(ms < 2000, "Inférence anormalement lente : $ms ms")
        }
    }
}
