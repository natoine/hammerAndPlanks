package com.naturalpad.hammerandplanks.web

import com.github.xpenatan.gdx.backends.teavm.TeaApplication
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration
import com.github.xpenatan.gdx.backends.teavm.TeaAssetPreloadListener
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks

/** Lanceur navigateur : langue de navigator.language, canvas plein écran, textes exposés pour l'accessibilité. */
object WebLauncher {
    @JvmStatic
    fun main(args: Array<String>) {
        val hooks = object : PlatformHooks {
            override val preferredLanguageTag: String = browserLanguage() ?: "en"

            override fun onLaunchScreenShown(language: String, title: String, tagline: String) {
                describePage(language, "$title - $tagline")
            }

            override fun onInputModeChanged(announcement: String) = announce(announcement)

            override fun onBoatMoved(offset: Float) = reportBoatOffset(offset)

            // Réglages de contrôle par l'URL, en attendant un menu d'options : ?control=velocity&speed=0.5
            override val forcedControlMode: String? = urlParameter("control")
            override val forcedSpeedFactor: String? = urlParameter("speed")

            override val gamepads = WebGamepads()
            override val camera = WebCamera()
        }
        val config = TeaApplicationConfiguration("canvas").apply {
            width = 0 // 0 = toute la fenêtre, redimensionnée avec elle
            height = 0
            showDownloadLogs = false
            usePhysicalPixels = true // texte net sur les écrans haute densité
            preloadListener = TeaAssetPreloadListener { loader -> loader.loadScript("freetype.js") }
        }
        TeaApplication(HammerAndPlanksGame(hooks), config)
    }
}
