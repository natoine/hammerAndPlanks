package com.naturalpad.hammerandplanks

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import com.naturalpad.hammerandplanks.input.InputMode
import com.naturalpad.hammerandplanks.input.InputModeSelector
import com.naturalpad.hammerandplanks.launch.LaunchScreen

/** Point d'entrée partagé par tous les lanceurs. */
class HammerAndPlanksGame(private val platform: PlatformHooks) : Game() {
    lateinit var language: String
        private set
    lateinit var texts: I18NBundle
        private set
    /** Mode d'interaction choisi, sauvegardé entre deux lancements. */
    lateinit var inputModes: InputModeSelector
        private set

    override fun create() {
        language = Languages.resolve(platform.preferredLanguageTag)
        texts = Texts.load(Gdx.files.internal("i18n/messages"), language)
        val prefs = Gdx.app.getPreferences(PREFERENCES)
        inputModes = InputModeSelector(InputModeSelector.parse(prefs.getString(KEY_INPUT_MODE, null))) { mode ->
            prefs.putString(KEY_INPUT_MODE, mode.name)
            prefs.flush()
            announceInputMode(mode)
        }
        setScreen(LaunchScreen(this, platform))
    }

    /** Annonce le mode courant (lecteur d'écran sur le web, par exemple). */
    fun announceInputMode(mode: InputMode = inputModes.current) {
        val name = texts.get("input.mode." + mode.name.lowercase())
        platform.onInputModeChanged(texts.format("input.mode.announce", name))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }

    private companion object {
        const val PREFERENCES = "hammer-and-planks"
        const val KEY_INPUT_MODE = "inputMode"
    }
}
