package com.naturalpad.hammerandplanks

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import com.naturalpad.hammerandplanks.launch.LaunchScreen

/** Point d'entrée partagé par tous les lanceurs. */
class HammerAndPlanksGame(private val platform: PlatformHooks) : Game() {
    lateinit var language: String
        private set
    lateinit var texts: I18NBundle
        private set

    override fun create() {
        language = Languages.resolve(platform.preferredLanguageTag)
        texts = Texts.load(Gdx.files.internal("i18n/messages"), language)
        setScreen(LaunchScreen(this, platform))
    }

    override fun dispose() {
        screen?.dispose()
        super.dispose()
    }
}
