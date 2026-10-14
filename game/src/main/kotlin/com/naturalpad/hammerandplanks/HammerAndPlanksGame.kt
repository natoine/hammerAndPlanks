package com.naturalpad.hammerandplanks

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import com.naturalpad.hammerandplanks.control.ControlSettings
import com.naturalpad.hammerandplanks.input.InputMode
import com.naturalpad.hammerandplanks.input.InputModeSelector
import com.naturalpad.hammerandplanks.launch.LaunchScreen
import com.naturalpad.hammerandplanks.ui.InputModeBadgeLayout

/** Point d'entrée partagé par tous les lanceurs. */
class HammerAndPlanksGame(private val platform: PlatformHooks) : Game() {
    lateinit var language: String
        private set
    lateinit var texts: I18NBundle
        private set
    /** Réglages de contrôle du bateau (mode position, vitesse ou force ; facteur de vitesse). */
    lateinit var controlSettings: ControlSettings
        private set

    /** Mode d'interaction choisi, sauvegardé entre deux lancements. */
    lateinit var inputModes: InputModeSelector
        private set

    override fun create() {
        language = Languages.resolve(platform.preferredLanguageTag)
        texts = Texts.load(Gdx.files.internal("i18n/messages"), language)
        gamepadConnected = platform.gamepads.isConnected()
        val prefs = Gdx.app.getPreferences(PREFERENCES)
        // Pas encore de menu d'options : les réglages de contrôle se lisent dans les préférences ou une option de lancement.
        controlSettings = ControlSettings.parse(
            platform.forcedControlMode ?: prefs.getString(KEY_CONTROL_MODE, null),
            platform.forcedSpeedFactor ?: prefs.getString(KEY_SPEED_FACTOR, null),
        )
        val initial = platform.forcedInputMode ?: prefs.getString(KEY_INPUT_MODE, null)
        inputModes = InputModeSelector(InputModeSelector.parse(initial)) { mode ->
            prefs.putString(KEY_INPUT_MODE, mode.name)
            prefs.flush()
            announceInputMode(mode)
        }
        setScreen(LaunchScreen(this, platform))
    }

    /** Dernière présence de manette connue (mise à jour par l'écran courant). */
    var gamepadConnected: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (::inputModes.isInitialized && inputModes.current == InputMode.GAMEPAD) announceInputMode()
        }

    /** Annonce le mode courant (lecteur d'écran sur le web, par exemple). */
    fun announceInputMode(mode: InputMode = inputModes.current) {
        val name = texts.get(InputModeBadgeLayout.labelKey(mode, gamepadConnected))
        platform.onInputModeChanged(texts.format("input.mode.announce", name))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }

    private companion object {
        const val PREFERENCES = "hammer-and-planks"
        const val KEY_INPUT_MODE = "inputMode"
        const val KEY_CONTROL_MODE = "controlMode"
        const val KEY_SPEED_FACTOR = "speedFactor"
    }
}
