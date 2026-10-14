package com.naturalpad.hammerandplanks.launch

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.naturalpad.hammerandplanks.BuildInfo
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import com.naturalpad.hammerandplanks.sea.SeaRenderer

/**
 * Écran de lancement : la mer animée de l'ancien jeu (vagues et écume) défile, le bateau joueur (vue 3/4) tangue au centre,
 * le titre et l'accroche apparaissent en fondu. Les textes viennent du bundle i18n.
 */
class LaunchScreen(
    private val game: HammerAndPlanksGame,
    private val platform: PlatformHooks,
) : ScreenAdapter() {

    private val viewport = ScreenViewport()
    private val batch = SpriteBatch()
    private val sea = SeaRenderer()
    private val boat = Texture(Gdx.files.internal(BoatFrames.path(BoatFrames.indexForHeading(0f))), true).apply {
        setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
    }
    private val titleGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/PirataOne-Regular.ttf"))
    private val textGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Nunito.ttf"))

    private val title = game.texts.get("launch.title")
    private val tagline = game.texts.get("launch.tagline")
    private val versionText = game.texts.format("launch.version", BuildInfo.VERSION)

    private var layout = LaunchLayout.compute(Gdx.graphics.width.coerceAtLeast(1), Gdx.graphics.height.coerceAtLeast(1))
    private var titleFont: BitmapFont? = null
    private var taglineFont: BitmapFont? = null
    private var versionFont: BitmapFont? = null
    private val glyphs = GlyphLayout()
    private var time = 0f

    override fun show() {
        platform.onLaunchScreenShown(game.language, title, tagline)
    }

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return // fenêtre minimisée
        viewport.update(width, height, true)
        val previous = layout
        layout = LaunchLayout.compute(width, height)
        if (titleFont == null || previous.titleFontPx != layout.titleFontPx ||
            previous.taglineFontPx != layout.taglineFontPx || previous.versionFontPx != layout.versionFontPx
        ) {
            regenerateFonts()
        }
    }

    private fun regenerateFonts() {
        disposeFonts()
        titleFont = titleGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = layout.titleFontPx
            color = TITLE_COLOR
            borderWidth = (layout.titleFontPx / 24f).coerceAtLeast(1f)
            borderColor = OUTLINE_COLOR
            shadowOffsetY = (layout.titleFontPx / 18).coerceAtLeast(1)
            shadowColor = SHADOW_COLOR
        })
        taglineFont = textGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = layout.taglineFontPx
            color = Color.WHITE
            borderWidth = (layout.taglineFontPx / 14f).coerceAtLeast(1f)
            borderColor = OUTLINE_COLOR
        })
        versionFont = textGenerator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = layout.versionFontPx
            color = VERSION_COLOR
        })
    }

    override fun render(delta: Float) {
        time += delta
        ScreenUtils.clear(DEEP_SEA)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        drawSea()
        drawBoat()
        drawTexts()
        batch.end()
    }

    private fun drawSea() {
        sea.draw(batch, layout.width.toFloat(), layout.height.toFloat(), time)
    }

    private fun drawBoat() {
        val size = layout.boatSize
        val y = layout.boatCenterY + LaunchAnimation.bobOffset(time, size)
        batch.draw(boat, layout.boatCenterX - size / 2f, y - size / 2f, size, size)
    }

    private fun drawTexts() {
        val m = layout.margin
        val alpha = LaunchAnimation.fadeIn(time, delayS = 0.2f)
        var taglineY = layout.taglineBaselineY
        titleFont?.let { font ->
            font.color.a = alpha
            glyphs.setText(font, title, font.color, layout.textWidth, Align.center, true)
            font.draw(batch, glyphs, m, layout.titleBaselineY)
            taglineY = LaunchLayout.taglineBaselineBelow(layout.titleBaselineY, glyphs.height, layout.taglineFontPx)
        }
        taglineFont?.let { font ->
            font.color.a = LaunchAnimation.fadeIn(time, delayS = 0.8f)
            glyphs.setText(font, tagline, font.color, layout.textWidth, Align.center, true)
            font.draw(batch, glyphs, m, taglineY)
        }
        versionFont?.let { font ->
            font.color.a = alpha
            font.draw(batch, versionText, m, m + font.capHeight, layout.textWidth, Align.right, false)
        }
    }

    private fun disposeFonts() {
        titleFont?.dispose(); titleFont = null
        taglineFont?.dispose(); taglineFont = null
        versionFont?.dispose(); versionFont = null
    }

    override fun dispose() {
        disposeFonts()
        titleGenerator.dispose()
        textGenerator.dispose()
        sea.dispose()
        boat.dispose()
        batch.dispose()
    }

    private companion object {
        val DEEP_SEA: Color = Color.valueOf("14213dff")
        val TITLE_COLOR: Color = Color.valueOf("f4e3b5ff")
        val OUTLINE_COLOR: Color = Color.valueOf("3b2414ff")
        val SHADOW_COLOR: Color = Color(0f, 0f, 0f, 0.45f)
        val VERSION_COLOR: Color = Color(1f, 1f, 1f, 0.75f)
    }
}
