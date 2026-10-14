package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.github.sarxos.webcam.Webcam
import com.naturalpad.hammerandplanks.platform.CameraStatus
import com.naturalpad.hammerandplanks.platform.CameraThumbnail
import java.awt.Dimension
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * Webcam du PC (bibliothèque webcam-capture). La capture tourne sur un thread dédié ;
 * le thread de rendu ne fait que copier la dernière image dans une texture.
 */
class DesktopCamera : CameraThumbnail {
    @Volatile override var status: CameraStatus = CameraStatus.OFF
        private set

    private class Frame(val width: Int, val height: Int, val rgb: ByteArray)

    private val latest = AtomicReference<Frame?>(null)
    @Volatile private var running = false
    private var worker: Thread? = null
    private var texture: Texture? = null
    private var pixmap: Pixmap? = null

    override fun start() {
        if (running) return
        running = true
        status = CameraStatus.STARTING
        worker = thread(name = "webcam", isDaemon = true) { capture() }
    }

    private fun capture() {
        val webcam = try {
            Webcam.getDefault(CAMERA_TIMEOUT_MS)
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Recherche de webcam impossible : ${e.message}")
            null
        }
        if (webcam == null) {
            status = CameraStatus.UNAVAILABLE
            running = false
            return
        }
        try {
            webcam.viewSize = webcam.viewSizes.minByOrNull { kotlin.math.abs(it.width - PREFERRED_WIDTH) } ?: Dimension(640, 480)
            if (!webcam.open()) {
                status = CameraStatus.UNAVAILABLE
                return
            }
            Gdx.app.log(TAG, "Webcam ouverte : ${webcam.name} ${webcam.viewSize.width}x${webcam.viewSize.height}")
            while (running) {
                val buffer: ByteBuffer = webcam.imageBytes ?: continue // RGB 24 bits
                val size = webcam.viewSize
                val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                latest.set(Frame(size.width, size.height, bytes))
                status = CameraStatus.LIVE
            }
        } catch (e: Exception) {
            Gdx.app.error(TAG, "Capture interrompue : ${e.message}")
            status = CameraStatus.UNAVAILABLE
        } finally {
            webcam.close()
        }
    }

    override fun stop() {
        running = false
        worker?.join(STOP_TIMEOUT_MS)
        worker = null
        latest.set(null)
        status = CameraStatus.OFF
    }

    override fun frame(): Texture? {
        val frame = latest.getAndSet(null) ?: return texture
        val expected = frame.width * frame.height * 3
        if (frame.rgb.size < expected) return texture
        var target = pixmap
        if (target == null || target.width != frame.width || target.height != frame.height) {
            target?.dispose()
            texture?.dispose()
            target = Pixmap(frame.width, frame.height, Pixmap.Format.RGB888)
            pixmap = target
            texture = Texture(target).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
        target.pixels.apply { clear(); put(frame.rgb, 0, expected); flip() }
        texture?.draw(target, 0, 0)
        return texture
    }

    override fun dispose() {
        stop()
        texture?.dispose(); texture = null
        pixmap?.dispose(); pixmap = null
    }

    private companion object {
        const val TAG = "Camera"
        const val CAMERA_TIMEOUT_MS = 5_000L
        const val STOP_TIMEOUT_MS = 2_000L
        const val PREFERRED_WIDTH = 640
    }
}
