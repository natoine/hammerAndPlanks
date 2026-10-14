package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.controllers.Controllers
import com.naturalpad.hammerandplanks.platform.GamepadDetector

/** Manettes sur desktop via gdx-controllers (SDL). Toute erreur native vaut « pas de manette ». */
class DesktopGamepads : GamepadDetector {
    private var failed = false

    override fun isConnected(): Boolean {
        if (failed) return false
        return try {
            Controllers.getControllers().size > 0
        } catch (e: Throwable) {
            failed = true
            Gdx.app.error("Gamepads", "Détection des manettes indisponible : ${e.message}")
            false
        }
    }
}
