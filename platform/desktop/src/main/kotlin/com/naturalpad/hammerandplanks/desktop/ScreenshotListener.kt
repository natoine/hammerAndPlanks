package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.ApplicationListener
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.PixmapIO
import com.badlogic.gdx.utils.ScreenUtils

/**
 * Outil de développement : enregistre une capture PNG de la fenêtre après [delaySeconds], puis quitte.
 * Utile pour vérifier un rendu sans interaction (option --screenshot=chemin.png).
 */
class ScreenshotListener(
    private val delegate: ApplicationListener,
    private val outputPath: String,
    private val delaySeconds: Float,
) : ApplicationListener by delegate {
    private var elapsed = 0f
    private var done = false

    override fun render() {
        delegate.render()
        elapsed += Gdx.graphics.deltaTime
        if (!done && elapsed >= delaySeconds) {
            done = true
            val w = Gdx.graphics.backBufferWidth
            val h = Gdx.graphics.backBufferHeight
            val pixmap = Pixmap.createFromFrameBuffer(0, 0, w, h)
            // Le framebuffer OpenGL est lu de bas en haut : flipY remet l'image à l'endroit.
            PixmapIO.writePNG(Gdx.files.absolute(outputPath), pixmap, 6, true)
            pixmap.dispose()
            Gdx.app.log("Screenshot", "Capture enregistrée : $outputPath")
            Gdx.app.exit()
        }
    }
}
