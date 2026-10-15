package com.naturalpad.hammerandplanks.android

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.view.View
import com.naturalpad.hammerandplanks.pose.HaloShape
import com.naturalpad.hammerandplanks.pose.HaloStroke
import com.naturalpad.hammerandplanks.pose.PlayerHalo
import kotlin.math.max

/**
 * Halo autour du joueur suivi, posé exactement sur l'aperçu caméra (même placement que la PreviewView) :
 * capsules élargies et floutées, intérieur effacé. L'aperçu de la caméra frontale est en miroir : le halo aussi.
 * L'image analysée est redressée et remplit l'aperçu sans déformation (comme PreviewView.ScaleType.FILL_CENTER).
 */
class HaloView(context: Context) : View(context) {
    @Volatile private var shape: HaloShape? = null
    private val clearRunnable = Runnable { shape = null; invalidate() }

    private val outer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb((PlayerHalo.COLOR_R * 255).toInt(), (PlayerHalo.COLOR_G * 255).toInt(), (PlayerHalo.COLOR_B * 255).toInt())
        strokeCap = Paint.Cap.ROUND
    }
    private val inner = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        strokeCap = Paint.Cap.ROUND
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // BlurMaskFilter n'est pas accéléré
        alpha = PlayerHalo.OPACITY
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /** Nouvelle forme (depuis n'importe quel fil) ; sans nouvelle pose pendant 0,5 s, le halo s'efface. */
    fun update(newShape: HaloShape?) {
        shape = newShape
        removeCallbacks(clearRunnable)
        postInvalidate()
        if (newShape != null) postDelayed(clearRunnable, STALE_MS)
    }

    override fun onDraw(canvas: Canvas) {
        val s = shape ?: return
        if (width == 0 || height == 0) return
        val scale = max(width.toFloat() / s.imageWidth, height.toFloat() / s.imageHeight)
        val ox = (width - s.imageWidth * scale) / 2f
        val oy = (height - s.imageHeight * scale) / 2f
        val layer = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.scale(-1f, 1f, width / 2f, height / 2f) // miroir, comme l'aperçu de la caméra frontale
        outer.maskFilter = BlurMaskFilter(max(1f, s.haloWidth * scale * 0.4f), BlurMaskFilter.Blur.NORMAL)
        s.strokes.forEach { draw(canvas, it, s.haloWidth, scale, ox, oy, outer) }
        s.strokes.forEach { draw(canvas, it, 0f, scale, ox, oy, inner) }
        canvas.restoreToCount(layer)
    }

    private fun draw(canvas: Canvas, s: HaloStroke, extra: Float, scale: Float, ox: Float, oy: Float, paint: Paint) {
        val r = (s.radius + extra) * scale
        val x1 = ox + s.x1 * scale; val y1 = oy + s.y1 * scale
        val x2 = ox + s.x2 * scale; val y2 = oy + s.y2 * scale
        if (x1 == x2 && y1 == y2) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(x1, y1, r, paint)
        } else {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2 * r
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }

    private companion object {
        const val STALE_MS = 500L
    }
}
