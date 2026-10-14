package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.Controllers
import com.naturalpad.hammerandplanks.input.StickPosition
import com.naturalpad.hammerandplanks.platform.GamepadDetector

/** Manettes sur desktop via gdx-controllers (SDL). Toute erreur native vaut « pas de manette ». */
class DesktopGamepads : GamepadDetector {
    private var failed = false

    override fun isConnected(): Boolean = active() != null

    override fun leftStick(): StickPosition {
        val controller = active() ?: return StickPosition.CENTER
        val mapping = controller.mapping
        return StickPosition(controller.getAxis(mapping.axisLeftX), controller.getAxis(mapping.axisLeftY))
    }

    /** Dernière manette utilisée, sinon la première branchée. */
    private fun active(): Controller? {
        if (failed) return null
        return try {
            Controllers.getCurrent() ?: Controllers.getControllers().firstOrNull()
        } catch (e: Throwable) {
            failed = true
            Gdx.app.error("Gamepads", "Manettes indisponibles : ${e.message}")
            null
        }
    }
}
