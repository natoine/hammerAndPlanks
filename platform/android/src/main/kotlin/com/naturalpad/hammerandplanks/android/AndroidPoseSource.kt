package com.naturalpad.hammerandplanks.android

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageProxy
import com.naturalpad.hammerandplanks.pose.ImageRotation
import com.naturalpad.hammerandplanks.pose.PoseEstimator
import com.naturalpad.hammerandplanks.pose.PoseFrame
import com.naturalpad.hammerandplanks.pose.PoseSource
import com.naturalpad.hammerandplanks.pose.PoseStatus
import com.naturalpad.hammerandplanks.pose.onnx.OrtPoseModel
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * InstantHMR sur les images d'analyse de CameraX (fil d'analyse de [AndroidCamera]). Le modèle est embarqué
 * dans les assets de l'APK (models/instanthmr.onnx), copié une fois dans le stockage de l'application.
 * ONNX Runtime sur le processeur ; les fournisseurs NPU (QNN) viendront après les mesures (README, section 5).
 */
class AndroidPoseSource(private val context: Context, private val camera: AndroidCamera) : PoseSource {
    @Volatile override var status: PoseStatus = PoseStatus.OFF
        private set

    private val latest = AtomicReference<PoseFrame?>(null)
    private var estimator: PoseEstimator? = null
    private var rgbBuffer: ByteArray? = null
    private var rgbaBuffer: ByteArray? = null
    private var lastRunMs = 0L

    override fun latest(): PoseFrame? = latest.get()

    override fun nowS(): Double = SystemClock.elapsedRealtimeNanos() / 1e9

    override fun start() {
        if (status == PoseStatus.ERROR || status == PoseStatus.NO_MODEL) return
        status = PoseStatus.LOADING
        camera.frameConsumer = ::onFrame
    }

    override fun stop() {
        camera.frameConsumer = null
        latest.set(null)
        estimator?.reset()
        if (status != PoseStatus.ERROR && status != PoseStatus.NO_MODEL) status = PoseStatus.OFF
    }

    /** Fil d'analyse de CameraX. */
    private fun onFrame(image: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRunMs < MIN_PERIOD_MS) return
        lastRunMs = now
        val pose = estimator ?: loadEstimator() ?: return
        try {
            val plane = image.planes[0]
            val buffer = plane.buffer
            val size = buffer.remaining()
            val rgba = rgbaBuffer?.takeIf { it.size == size } ?: ByteArray(size).also { rgbaBuffer = it }
            buffer.get(rgba)
            val upright = ImageRotation.rgbaToUprightRgb(
                rgba, image.width, image.height, plane.rowStride, image.imageInfo.rotationDegrees, rgbBuffer,
            )
            rgbBuffer = upright.pixels
            val timestamp = image.imageInfo.timestamp / 1e9 // même horloge que elapsedRealtimeNanos
            latest.set(pose.estimate(upright.pixels, upright.width, upright.height, timestamp))
            status = PoseStatus.RUNNING
        } catch (e: Exception) {
            Log.e(TAG, "Inférence impossible", e)
            status = PoseStatus.ERROR
            camera.frameConsumer = null
        }
    }

    private fun loadEstimator(): PoseEstimator? = try {
        val file = File(context.filesDir, MODEL_ASSET)
        val assetSize = context.assets.openFd(MODEL_ASSET).use { it.length }
        if (!file.isFile || file.length() != assetSize) {
            context.assets.open(MODEL_ASSET).use { input -> file.outputStream().use { input.copyTo(it) } }
        }
        PoseEstimator(OrtPoseModel(file.path)).also { estimator = it }
    } catch (e: java.io.FileNotFoundException) {
        Log.e(TAG, "Modèle absent de l'APK : ./gradlew downloadModel puis reconstruire")
        status = PoseStatus.NO_MODEL
        camera.frameConsumer = null
        null
    } catch (e: Throwable) {
        Log.e(TAG, "Chargement du modèle impossible", e)
        status = PoseStatus.ERROR
        camera.frameConsumer = null
        null
    }

    private companion object {
        const val TAG = "Pose"
        const val MODEL_ASSET = "instanthmr.onnx"
        /** 20 Hz au plus (thermique et batterie). */
        const val MIN_PERIOD_MS = 50L
    }
}
