package com.naturalpad.hammerandplanks.launch

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.naturalpad.hammerandplanks.BuildInfo
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import com.naturalpad.hammerandplanks.sea.SeaRenderer
import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.input.GamepadSteering
import com.naturalpad.hammerandplanks.input.InputMode
import com.naturalpad.hammerandplanks.input.KeyboardSteering
import com.naturalpad.hammerandplanks.input.MotionStatus
import com.naturalpad.hammerandplanks.ui.CameraPanel
import com.naturalpad.hammerandplanks.ui.CameraThumbnailLayout
import com.naturalpad.hammerandplanks.ui.InputModeBadge
import com.naturalpad.hammerandplanks.ui.InputModeBadgeLayout
import com.naturalpad.hammerandplanks.util.PeriodicCheck

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
    private val badge = InputModeBadge(game.texts, touchHint = isTouchDevice())
    private val touchPoint = Vector2()
    private val cameraPanel = CameraPanel(game.texts)
    private val camera = platform.camera
    private val motion = platform.motion
    private var announcedMotionStatus: MotionStatus? = null
    /** Mode pour lequel la caméra a été allumée ou éteinte en dernier. */
    private var cameraMode: InputMode? = null
    private var paused = false
    /** Une manette peut être branchée à tout moment : on vérifie une fois par seconde. */
    private val gamepadCheck = PeriodicCheck(1f) { platform.gamepads.isConnected() }

    /** Tab fait défiler les modes d'interaction ; un appui sur le badge aussi (écrans tactiles). */
    private val input = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            // Flèches consommées en mode clavier (lues à chaque image dans render) : la page web ne défile pas.
            if (keycode == Input.Keys.LEFT || keycode == Input.Keys.RIGHT) return game.inputModes.current == InputMode.KEYBOARD
            if (keycode != Input.Keys.TAB) return false
            badge.update(game.inputModes.next(), game.gamepadConnected)
            return true
        }

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            viewport.unproject(touchPoint.set(screenX.toFloat(), screenY.toFloat()))
            if (!badge.hitBox.contains(touchPoint.x, touchPoint.y)) return false
            badge.update(game.inputModes.next(), game.gamepadConnected)
            return true
        }
    }
    private val boatMotion = BoatMotion(game.controlSettings)
    private val boatRegion by lazy { TextureRegion(boat) }
    private var reportedBoatOffset = Float.NaN
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
        Gdx.input.inputProcessor = input
        platform.onLaunchScreenShown(game.language, title, tagline)
        game.announceInputMode()
    }

    override fun hide() {
        if (Gdx.input.inputProcessor === input) Gdx.input.inputProcessor = null
        stopCamera()
    }

    override fun pause() {
        paused = true
        stopCamera() // libère la caméra quand l'application passe en arrière-plan
    }

    override fun resume() {
        paused = false
    }

    /** Allume la caméra en mode caméra seulement, l'éteint sinon (caméra à la demande). */
    private fun syncCamera() {
        val mode = game.inputModes.current
        if (mode == cameraMode || paused) return
        cameraMode = mode
        if (mode == InputMode.MOTION) {
            placeCamera()
            camera.start()
            motion.start()
        } else {
            motion.stop()
            camera.stop()
        }
    }

    private fun stopCamera() {
        motion.stop()
        camera.stop()
        cameraMode = null
    }

    private fun placeCamera() {
        cameraLayout?.normalized()?.let { (left, top, width, height) -> camera.place(left, top, width, height) }
    }

    private var cameraLayout: CameraThumbnailLayout? = null

    override fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return // fenêtre minimisée
        viewport.update(width, height, true)
        val previous = layout
        layout = LaunchLayout.compute(width, height)
        badge.resize(InputModeBadgeLayout.compute(width, height, layout.margin))
        badge.update(game.inputModes.current, game.gamepadConnected)
        cameraLayout = CameraThumbnailLayout.compute(width, height, layout.margin, layout.versionFontPx * 1.8f).also {
            cameraPanel.resize(it)
        }
        placeCamera()
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
        if (gamepadCheck.update(delta)) game.gamepadConnected = gamepadCheck.value == true
        syncCamera()
        updateBoat(delta)
        ScreenUtils.clear(DEEP_SEA)
        viewport.apply()
        batch.projectionMatrix = viewport.camera.combined
        batch.begin()
        drawSea()
        drawBoat()
        drawTexts()
        val uiAlpha = LaunchAnimation.fadeIn(time, delayS = 0.2f)
        badge.draw(batch, game.inputModes.current, game.gamepadConnected, uiAlpha, motionStatusKey())
        if (game.inputModes.current == InputMode.MOTION) cameraPanel.draw(batch, camera, uiAlpha)
        batch.end()
    }

    private fun drawSea() {
        sea.draw(batch, layout.width.toFloat(), layout.height.toFloat(), time)
    }

    /** Le mode d'interaction donne le GameInput qui pilote le bateau : flèches, stick gauche ou inclinaison du corps. */
    /** Ligne d'état du mode caméra, annoncée à la plateforme quand elle change. */
    private fun motionStatusKey(): String? {
        val mode = game.inputModes.current
        val status = if (mode == InputMode.MOTION) motion.status else MotionStatus.OFF
        val key = InputModeBadgeLayout.motionStatusKey(mode, status, platform.tiltSource)
        if (status != announcedMotionStatus) {
            announcedMotionStatus = status
            platform.onMotionStatusChanged(status, key?.let(game.texts::get) ?: "")
        }
        return key
    }

    private fun updateBoat(delta: Float) {
        val input = when (game.inputModes.current) {
            InputMode.KEYBOARD ->
                KeyboardSteering.read(Gdx.input.isKeyPressed(Input.Keys.LEFT), Gdx.input.isKeyPressed(Input.Keys.RIGHT))
            InputMode.GAMEPAD ->
                if (game.gamepadConnected) GamepadSteering.read(platform.gamepads.leftStick()) else GameInput.IDLE
            InputMode.MOTION -> motion.read() // inclinaison du buste ou de la tête (InstantHMR)
        }
        boatMotion.update(input, delta)
        val offset = boatOffset()
        if (offset != reportedBoatOffset) {
            reportedBoatOffset = offset
            platform.onBoatMoved(offset)
        }
    }

    /** Décalage du bateau depuis le centre, en fraction de la largeur d'écran (position de rail × demi-rail). */
    private fun boatOffset(): Float =
        boatMotion.position * LaunchLayout.boatLimit(layout.width, layout.boatSize, layout.margin)

    private fun drawBoat() {
        val size = layout.boatSize
        val x = layout.boatCenterX + boatOffset() * layout.width
        val y = layout.boatCenterY + LaunchAnimation.bobOffset(time, size)
        // Rotation autour du centre du sprite : le bateau s'incline dans le sens du mouvement.
        batch.draw(boatRegion, x - size / 2f, y - size / 2f, size / 2f, size / 2f, size, size, 1f, 1f, boatMotion.tiltDeg)
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
        badge.dispose()
        cameraPanel.dispose()
        camera.dispose()
        titleGenerator.dispose()
        textGenerator.dispose()
        sea.dispose()
        boat.dispose()
        batch.dispose()
    }

    /** Écran tactile sans clavier probable : on invite à toucher le badge plutôt qu'à appuyer sur Tab. */
    private fun isTouchDevice(): Boolean =
        Gdx.app.type == Application.ApplicationType.Android ||
            Gdx.app.type == Application.ApplicationType.iOS ||
            (Gdx.input.isPeripheralAvailable(Input.Peripheral.MultitouchScreen) &&
                !Gdx.input.isPeripheralAvailable(Input.Peripheral.HardwareKeyboard))

    private companion object {
        val DEEP_SEA: Color = Color.valueOf("14213dff")
        val TITLE_COLOR: Color = Color.valueOf("f4e3b5ff")
        val OUTLINE_COLOR: Color = Color.valueOf("3b2414ff")
        val SHADOW_COLOR: Color = Color(0f, 0f, 0f, 0.45f)
        val VERSION_COLOR: Color = Color(1f, 1f, 1f, 0.75f)
    }
}
