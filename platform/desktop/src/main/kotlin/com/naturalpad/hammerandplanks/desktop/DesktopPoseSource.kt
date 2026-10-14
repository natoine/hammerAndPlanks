package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.Gdx
import com.naturalpad.hammerandplanks.pose.PoseEstimator
import com.naturalpad.hammerandplanks.pose.PoseFrame
import com.naturalpad.hammerandplanks.pose.PoseSource
import com.naturalpad.hammerandplanks.pose.PoseStatus
import com.naturalpad.hammerandplanks.pose.onnx.OrtPoseModel
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * InstantHMR sur les images de la webcam du PC, sur un thread dédié : le rendu ne l'attend jamais
 * (il lit toujours la dernière pose). Fréquence plafonnée pour laisser du processeur au jeu.
 */
class DesktopPoseSource(private val camera: DesktopCamera, private val modelFile: File) : PoseSource {
    @Volatile override var status: PoseStatus = PoseStatus.OFF
        private set

    private val latest = AtomicReference<PoseFrame?>(null)
    @Volatile private var running = false
    private var worker: Thread? = null
    private var model: OrtPoseModel? = null

    override fun latest(): PoseFrame? = latest.get()

    override fun nowS(): Double = System.nanoTime() / 1e9

    override fun start() {
        if (running) return
        if (!modelFile.isFile) {
            status = PoseStatus.NO_MODEL
            Gdx.app.error(TAG, "Modèle absent : ${modelFile.absolutePath} (./gradlew downloadModel)")
            return
        }
        running = true
        status = PoseStatus.LOADING
        worker = thread(name = "pose", isDaemon = true) { loop() }
    }

    private fun loop() {
        val estimator = try {
            val loaded = model ?: OrtPoseModel(modelFile.path).also { model = it }
            PoseEstimator(loaded)
        } catch (e: Throwable) {
            Gdx.app.error(TAG, "Chargement du modèle impossible : ${e.message}")
            status = PoseStatus.ERROR
            running = false
            return
        }
        Gdx.app.log(TAG, "Modèle chargé : ${modelFile.name}")
        var lastId = -1L
        while (running) {
            val frame = camera.peekFrame()
            if (frame == null || frame.id == lastId) { Thread.sleep(5); continue }
            lastId = frame.id
            val started = System.nanoTime()
            try {
                latest.set(estimator.estimate(frame.rgb, frame.width, frame.height, frame.timestampS))
                status = PoseStatus.RUNNING
            } catch (e: Exception) {
                Gdx.app.error(TAG, "Inférence impossible : ${e.message}")
                status = PoseStatus.ERROR
                running = false
                return
            }
            val elapsedMs = (System.nanoTime() - started) / 1_000_000
            if (elapsedMs < MIN_PERIOD_MS) Thread.sleep(MIN_PERIOD_MS - elapsedMs)
        }
    }

    override fun stop() {
        running = false
        worker?.join(2_000)
        worker = null
        latest.set(null)
        if (status != PoseStatus.NO_MODEL) status = PoseStatus.OFF
    }

    fun dispose() {
        stop()
        model?.close()
        model = null
    }

    private companion object {
        const val TAG = "Pose"
        /** 20 Hz au plus (voir README : thermique et batterie). */
        const val MIN_PERIOD_MS = 50L
    }
}
