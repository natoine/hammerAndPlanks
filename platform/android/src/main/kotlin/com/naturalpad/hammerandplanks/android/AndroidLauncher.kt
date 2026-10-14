package com.naturalpad.hammerandplanks.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import java.util.Locale

/** Lanceur Android : langue du système, plein écran immersif, pas de capteurs inutiles. */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hooks = object : PlatformHooks {
            override val preferredLanguageTag: String = Locale.getDefault().toLanguageTag()
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }
        initialize(HammerAndPlanksGame(hooks), config)
    }
}
