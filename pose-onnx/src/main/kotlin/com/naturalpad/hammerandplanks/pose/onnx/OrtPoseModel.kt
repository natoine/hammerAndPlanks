package com.naturalpad.hammerandplanks.pose.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.naturalpad.hammerandplanks.pose.Mhr70
import com.naturalpad.hammerandplanks.pose.ModelOutput
import com.naturalpad.hammerandplanks.pose.PoseModel
import com.naturalpad.hammerandplanks.pose.PosePreprocessor
import java.nio.FloatBuffer

/**
 * InstantHMR exécuté par ONNX Runtime, sur le processeur (le fournisseur NPU viendra après les mesures,
 * voir README section 5). Une instance n'est pas réentrante : un seul thread d'inférence.
 */
class OrtPoseModel(modelPath: String, threads: Int = DEFAULT_THREADS) : PoseModel, AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(threads)
        },
    )
    // Comme le Python : 1re entrée = image, 2e = cliff_cond.
    private val inputNames = session.inputNames.toList()
    private val imageShape = longArrayOf(1, 3, PosePreprocessor.INPUT_SIZE.toLong(), PosePreprocessor.INPUT_SIZE.toLong())
    private val cliffShape = longArrayOf(1, 3)

    init {
        require(inputNames.size == 2) { "Modèle inattendu : entrées $inputNames" }
        require(JOINTS_2D in session.outputNames && JOINTS_3D in session.outputNames) {
            "Modèle sans sorties $JOINTS_2D / $JOINTS_3D : ${session.outputNames}"
        }
    }

    override fun run(image: FloatArray, cliff: FloatArray): ModelOutput {
        OnnxTensor.createTensor(env, FloatBuffer.wrap(image), imageShape).use { imageTensor ->
            OnnxTensor.createTensor(env, FloatBuffer.wrap(cliff), cliffShape).use { cliffTensor ->
                session.run(mapOf(inputNames[0] to imageTensor, inputNames[1] to cliffTensor), setOf(JOINTS_2D, JOINTS_3D)).use { result ->
                    return ModelOutput(
                        joints2dNorm = read(result, JOINTS_2D, Mhr70.COUNT * 2),
                        joints3d = read(result, JOINTS_3D, Mhr70.COUNT * 3),
                    )
                }
            }
        }
    }

    private fun read(result: OrtSession.Result, name: String, size: Int): FloatArray {
        val tensor = result.get(name).orElseThrow { IllegalStateException("Sortie $name absente") } as OnnxTensor
        val buffer = tensor.floatBuffer
        check(buffer.remaining() == size) { "Sortie $name : ${buffer.remaining()} valeurs au lieu de $size" }
        return FloatArray(size).also { buffer.get(it) }
    }

    override fun close() = session.close()

    companion object {
        const val JOINTS_2D = "joints_2d"
        const val JOINTS_3D = "joints_3d"
        val DEFAULT_THREADS = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(1, 4)
    }
}
