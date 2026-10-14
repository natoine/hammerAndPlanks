package com.naturalpad.hammerandplanks.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch

/** Fond blanc aux coins arrondis en neuf morceaux, à teinter au dessin. */
class RoundedBox(radius: Int) {
    private val texture: Texture
    val patch: NinePatch

    init {
        val r = radius.coerceAtLeast(1)
        val size = 2 * r + 2
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888).apply {
            setColor(Color.WHITE)
            fillRectangle(r, 0, size - 2 * r, size)
            fillRectangle(0, r, size, size - 2 * r)
            fillCircle(r, r, r)
            fillCircle(size - r - 1, r, r)
            fillCircle(r, size - r - 1, r)
            fillCircle(size - r - 1, size - r - 1, r)
        }
        texture = Texture(pixmap).also { pixmap.dispose() }
        patch = NinePatch(texture, r, r, r, r)
    }

    fun dispose() = texture.dispose()
}
