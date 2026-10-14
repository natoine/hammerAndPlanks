package com.naturalpad.hammerandplanks.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.I18NBundle
import com.naturalpad.hammerandplanks.platform.CameraStatus
import com.naturalpad.hammerandplanks.platform.CameraThumbnail
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Cadre de l'imagette caméra en bas à droite. Dessine l'image si le jeu la reçoit (desktop) ;
 * sinon la vue native de la plateforme s'affiche par-dessus (Android, web). Hors flux, affiche l'état
 * (démarrage, accès refusé, pas de caméra).
 */
class CameraPanel(private val texts: I18NBundle) {
    private val textGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Nunito.ttf"))
    private val videocam = icon("ui/input_motion.png")
    private val cameraOff = icon("ui/camera_off.png")
    private var font: BitmapFont? = null
    private var box: RoundedBox? = null
    private var layout: CameraThumbnailLayout? = null
    private val tint = Color()

    private fun icon(path: String) = Texture(Gdx.files.internal(path), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }

    fun resize(newLayout: CameraThumbnailLayout) {
        val sizeChanged = layout?.width != newLayout.width
        layout = newLayout
        if (sizeChanged || font == null) {
            font?.dispose()
            box?.dispose()
            font = textGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                size = max(12, (newLayout.width * 0.09f).roundToInt())
                color = Color.WHITE
            })
            box = RoundedBox(max(2, (newLayout.width * 0.05f).roundToInt()))
        }
    }

    fun draw(batch: SpriteBatch, camera: CameraThumbnail, alpha: Float) {
        val l = layout ?: return
        val status = camera.status
        if (status == CameraStatus.OFF) return
        val border = max(2f, l.width * 0.015f)
        box?.patch?.let {
            it.color = tint.set(FRAME_COLOR).apply { a *= alpha }
            it.draw(batch, l.x - border, l.y - border, l.width + 2 * border, l.height + 2 * border)
            it.color = tint.set(BACKGROUND_COLOR).apply { a *= alpha }
            it.draw(batch, l.x, l.y, l.width, l.height)
        }
        val frame = if (status == CameraStatus.LIVE) camera.frame() else null
        if (frame != null) {
            val uv = CameraThumbnailLayout.coverUv(frame.width, frame.height, l.width, l.height, camera.mirrored)
            batch.setColor(1f, 1f, 1f, alpha)
            // Cette signature associe v au bas du quad : on passe v2 (bas de l'image) puis v (haut).
            batch.draw(frame, l.x, l.y, l.width, l.height, uv[0], uv[3], uv[2], uv[1])
            batch.setColor(1f, 1f, 1f, 1f)
            return
        }
        if (status == CameraStatus.LIVE) return // image native posée par-dessus par la plateforme
        val key = when (status) {
            CameraStatus.DENIED -> "camera.denied"
            CameraStatus.UNAVAILABLE -> "camera.unavailable"
            else -> "camera.starting"
        }
        val icon = if (status == CameraStatus.STARTING) videocam else cameraOff
        val iconSize = l.width * 0.3f
        batch.setColor(1f, 1f, 1f, 0.8f * alpha)
        batch.draw(icon, l.x + (l.width - iconSize) / 2f, l.y + l.height * 0.55f, iconSize, iconSize)
        batch.setColor(1f, 1f, 1f, 1f)
        font?.let {
            it.color.a = alpha
            val pad = l.width * 0.06f
            it.draw(batch, texts.get(key), l.x + pad, l.y + l.height * 0.45f, l.width - 2 * pad, Align.center, true)
        }
    }

    fun dispose() {
        font?.dispose()
        box?.dispose()
        textGenerator.dispose()
        videocam.dispose()
        cameraOff.dispose()
    }

    private companion object {
        val FRAME_COLOR: Color = Color.valueOf("f4e3b5ff")
        val BACKGROUND_COLOR: Color = Color(0.04f, 0.08f, 0.16f, 0.85f)
    }
}
