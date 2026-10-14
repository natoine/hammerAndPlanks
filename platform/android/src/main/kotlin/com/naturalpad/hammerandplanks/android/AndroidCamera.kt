package com.naturalpad.hammerandplanks.android

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Outline
import android.util.Log
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.naturalpad.hammerandplanks.platform.CameraStatus
import com.naturalpad.hammerandplanks.platform.CameraThumbnail

/**
 * Caméra frontale via CameraX, affichée dans une PreviewView posée par-dessus la vue du jeu.
 * L'autorisation n'est demandée qu'au premier [start], c'est-à-dire au passage en mode caméra.
 * Construit sur le thread UI ; [start], [stop] et [place] peuvent être appelés depuis le thread de rendu.
 */
class AndroidCamera(private val activity: Activity, private val root: FrameLayout) : CameraThumbnail, LifecycleOwner {
    private val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.CREATED }
    override val lifecycle: Lifecycle get() = registry

    @Volatile override var status: CameraStatus = CameraStatus.OFF
        private set

    // PreviewView applique déjà l'effet miroir de la caméra frontale.
    override val mirrored: Boolean get() = false

    private var wanted = false
    private val analysisExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "camera-analysis") }

    /** Reçoit les images d'analyse (RGBA, non miroir) sur un fil dédié ; null si personne ne les lit. */
    @Volatile var frameConsumer: ((androidx.camera.core.ImageProxy) -> Unit)? = null
    private var previewView: PreviewView? = null
    private var placement = floatArrayOf(0f, 0f, 0f, 0f)

    override fun start() = activity.runOnUiThread {
        if (wanted) return@runOnUiThread
        wanted = true
        when {
            !activity.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) -> status = CameraStatus.UNAVAILABLE
            ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> bind()
            else -> {
                status = CameraStatus.STARTING
                activity.requestPermissions(arrayOf(Manifest.permission.CAMERA), PERMISSION_REQUEST)
            }
        }
    }

    /** À appeler depuis Activity.onRequestPermissionsResult. */
    fun onPermissionResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != PERMISSION_REQUEST || !wanted) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) bind() else status = CameraStatus.DENIED
    }

    private fun bind() {
        status = CameraStatus.STARTING
        val view = previewView ?: createPreviewView().also { previewView = it }
        view.visibility = View.VISIBLE
        applyPlacement()
        val future = ProcessCameraProvider.getInstance(activity)
        future.addListener({
            try {
                val provider = future.get()
                if (!wanted) return@addListener
                provider.unbindAll()
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                // Images pour l'estimation de pose : RGBA, seule la plus récente est gardée si l'inférence est lente.
                val analysis = androidx.camera.core.ImageAnalysis.Builder()
                    .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor) { image ->
                    image.use { frameConsumer?.invoke(it) }
                }
                registry.currentState = Lifecycle.State.RESUMED
                provider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e(TAG, "Caméra indisponible", e)
                status = CameraStatus.UNAVAILABLE
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    private fun createPreviewView(): PreviewView = PreviewView(activity).apply {
        // TextureView : seule implémentation qui se superpose proprement à la SurfaceView OpenGL du jeu.
        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        scaleType = PreviewView.ScaleType.FILL_CENTER
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.05f)
            }
        }
        previewStreamState.observe(this@AndroidCamera) { state ->
            if (wanted && state == PreviewView.StreamState.STREAMING) status = CameraStatus.LIVE
        }
        root.addView(this, FrameLayout.LayoutParams(1, 1))
    }

    override fun stop() = activity.runOnUiThread {
        if (!wanted && status == CameraStatus.OFF) return@runOnUiThread
        wanted = false
        try {
            ProcessCameraProvider.getInstance(activity).addListener({
                if (!wanted) ProcessCameraProvider.getInstance(activity).get().unbindAll()
            }, ContextCompat.getMainExecutor(activity))
        } catch (e: Exception) {
            Log.w(TAG, "Arrêt de la caméra", e)
        }
        registry.currentState = Lifecycle.State.CREATED
        previewView?.visibility = View.GONE
        status = CameraStatus.OFF
    }

    override fun place(left: Float, top: Float, width: Float, height: Float) {
        placement = floatArrayOf(left, top, width, height)
        activity.runOnUiThread { applyPlacement() }
    }

    private fun applyPlacement() {
        val view = previewView ?: return
        if (root.width == 0 || root.height == 0) {
            root.post { applyPlacement() } // pas encore mesurée
            return
        }
        val (left, top, width, height) = placement
        view.layoutParams = FrameLayout.LayoutParams((width * root.width).toInt(), (height * root.height).toInt()).apply {
            leftMargin = (left * root.width).toInt()
            topMargin = (top * root.height).toInt()
        }
        view.invalidateOutline()
    }

    companion object {
        const val PERMISSION_REQUEST = 42
        private const val TAG = "Camera"
    }
}
