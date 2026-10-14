package com.naturalpad.hammerandplanks.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.I18NBundle
import com.naturalpad.hammerandplanks.input.InputMode
import kotlin.math.max

/** Dessine le badge du mode d'interaction et dit si un appui le touche. */
class InputModeBadge(private val texts: I18NBundle, private val touchHint: Boolean) {
    private val icons: Map<InputMode, Texture> = InputMode.entries.associateWith { mode ->
        Texture(Gdx.files.internal(InputModeBadgeLayout.iconPath(mode)), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }
    }
    private val textGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Nunito.ttf"))
    private var labelFont: BitmapFont? = null
    private var hintFont: BitmapFont? = null
    private var background: NinePatch? = null
    private var backgroundTexture: Texture? = null
    private var layout: InputModeBadgeLayout? = null
    private val glyphs = GlyphLayout()
    private val backgroundColor = Color()

    /** Zone d'appui du dernier badge dessiné. */
    var hitBox: HitBox = HitBox(0f, 0f, 0f, 0f)
        private set

    fun resize(newLayout: InputModeBadgeLayout) {
        if (newLayout == layout) return
        val sizesChanged = layout?.let {
            it.iconPx != newLayout.iconPx || it.labelPx != newLayout.labelPx || it.hintPx != newLayout.hintPx
        } ?: true
        layout = newLayout
        if (sizesChanged) regenerate(newLayout)
        measured = null
    }

    private fun regenerate(l: InputModeBadgeLayout) {
        disposeGraphics()
        labelFont = textGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = l.labelPx
            color = Color.WHITE
        })
        hintFont = textGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = l.hintPx
            color = HINT_COLOR
        })
        // Fond arrondi en neuf morceaux : un petit carré aux coins ronds, étiré au dessin.
        val r = l.cornerRadius
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
        backgroundTexture = Texture(pixmap).also { pixmap.dispose() }
        background = NinePatch(backgroundTexture, r, r, r, r)
    }

    /** Mesures du badge pour un mode : textes, taille, zone d'appui. */
    private class Measure(val label: String, val hint: String, val width: Float, val height: Float, val textHeight: Float)

    private var measured: Pair<InputMode, Measure>? = null

    /**
     * Calcule taille et zone d'appui pour [mode] sans dessiner. Appelé dès le redimensionnement,
     * pour qu'un appui arrivé avant la première image soit déjà reconnu.
     */
    fun update(mode: InputMode) {
        val l = layout ?: return
        val label = labelFont ?: return
        val hint = hintFont ?: return
        val labelText = texts.get(InputModeBadgeLayout.labelKey(mode))
        val hintText = texts.get(if (touchHint) "input.switch.touch" else "input.switch.key")
        glyphs.setText(label, labelText)
        val labelWidth = glyphs.width
        glyphs.setText(hint, hintText)
        val textWidth = max(labelWidth, glyphs.width)
        val textHeight = label.capHeight + l.gap * 0.6f + hint.capHeight
        val height = l.height(max(l.iconPx.toFloat(), textHeight))
        val width = l.width(textWidth)
        hitBox = HitBox(l.x, l.y, width, height)
        measured = mode to Measure(labelText, hintText, width, height, textHeight)
    }

    fun draw(batch: SpriteBatch, mode: InputMode, alpha: Float) {
        val l = layout ?: return
        val label = labelFont ?: return
        val hint = hintFont ?: return
        if (measured?.first != mode) update(mode)
        val m = measured?.second ?: return
        val width = m.width
        val height = m.height
        val textHeight = m.textHeight
        val labelText = m.label
        val hintText = m.hint

        background?.let {
            it.color = backgroundColor.set(BACKGROUND_COLOR).apply { a *= alpha }
            it.draw(batch, l.x, l.y, width, height)
        }
        val centerY = l.y + height / 2f
        val previous = batch.packedColor
        batch.setColor(1f, 1f, 1f, alpha)
        val iconSize = l.iconPx.toFloat()
        batch.draw(icons.getValue(mode), l.x + l.padding, centerY - iconSize / 2f, iconSize, iconSize)
        batch.packedColor = previous
        val textX = l.x + l.padding + l.iconPx + l.gap
        label.color.a = alpha
        label.draw(batch, labelText, textX, centerY + textHeight / 2f)
        hint.color.a = HINT_COLOR.a * alpha
        hint.draw(batch, hintText, textX, centerY - textHeight / 2f + hint.capHeight)
    }

    private fun disposeGraphics() {
        labelFont?.dispose(); labelFont = null
        hintFont?.dispose(); hintFont = null
        backgroundTexture?.dispose(); backgroundTexture = null
        background = null
    }

    fun dispose() {
        disposeGraphics()
        icons.values.forEach(Texture::dispose)
        textGenerator.dispose()
    }

    private companion object {
        val BACKGROUND_COLOR: Color = Color(0.04f, 0.08f, 0.16f, 0.75f)
        val HINT_COLOR: Color = Color(1f, 1f, 1f, 0.75f)
    }
}
