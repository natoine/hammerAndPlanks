package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import java.util.Locale

/**
 * Lanceur de développement (LWJGL3). Fenêtre au format portrait d'un smartphone, redimensionnable.
 * Options : --lang=fr|en (force la langue), --size=LARGEURxHAUTEUR, --screenshot=fichier.png (capture puis quitte),
 * --input-mode=keyboard|gamepad|motion (impose le mode d'interaction au lancement).
 */
fun main(args: Array<String>) {
    val options = DesktopOptions.parse(args)
    val forcedLanguage = options.language
    val hooks = object : PlatformHooks {
        override val preferredLanguageTag: String = forcedLanguage ?: Locale.getDefault().toLanguageTag()
        override val gamepads = DesktopGamepads()
        override val camera = DesktopCamera()
        override val forcedInputMode: String? = options.inputMode
    }
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Hammer and Planks")
        setWindowedMode(options.width, options.height)
        setResizable(true)
        useVsync(true)
        setForegroundFPS(60)
    }
    val game = HammerAndPlanksGame(hooks)
    val listener = options.screenshotPath?.let { ScreenshotListener(game, it, delaySeconds = 3f) } ?: game
    Lwjgl3Application(listener, config)
}
