package com.naturalpad.hammerandplanks.android

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import java.util.Locale

/**
 * Lanceur Android : langue du système, plein écran immersif. La vue du jeu est placée dans un FrameLayout
 * pour pouvoir poser l'aperçu caméra par-dessus.
 */
class AndroidLauncher : AndroidApplication() {
    private lateinit var camera: AndroidCamera

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        camera = AndroidCamera(this, root)
        val hooks = object : PlatformHooks {
            override val preferredLanguageTag: String = Locale.getDefault().toLanguageTag()
            override val gamepads = AndroidGamepads()
            override val camera = this@AndroidLauncher.camera
        }
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
            useAccelerometer = false
            useCompass = false
        }
        val gameView = initializeForView(HammerAndPlanksGame(hooks), config)
        root.addView(gameView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    @Deprecated("API Activity historique, utilisée par AndroidApplication")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        camera.onPermissionResult(requestCode, grantResults)
    }
}
