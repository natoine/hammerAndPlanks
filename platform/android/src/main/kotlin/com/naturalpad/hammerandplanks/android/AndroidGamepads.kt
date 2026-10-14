package com.naturalpad.hammerandplanks.android

import android.util.Log
import android.view.InputDevice
import com.badlogic.gdx.controllers.Controller
import com.badlogic.gdx.controllers.Controllers
import com.naturalpad.hammerandplanks.input.StickPosition
import com.naturalpad.hammerandplanks.platform.GamepadDetector

/**
 * Manette branchée en Bluetooth ou USB : présence via InputDevice (un périphérique physique qui se déclare
 * manette ou joystick), stick via gdx-controllers (événements de mouvement des manettes).
 */
class AndroidGamepads : GamepadDetector {
    private var failed = false

    override fun isConnected(): Boolean = InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        !device.isVirtual && isGamepadSource(device.sources)
    }

    override fun leftStick(): StickPosition {
        val controller = active() ?: return StickPosition.CENTER
        val mapping = controller.mapping
        return StickPosition(controller.getAxis(mapping.axisLeftX), controller.getAxis(mapping.axisLeftY))
    }

    private fun active(): Controller? {
        if (failed) return null
        return try {
            Controllers.getCurrent() ?: Controllers.getControllers().firstOrNull()
        } catch (e: Throwable) {
            failed = true
            Log.e("Gamepads", "Lecture des manettes indisponible", e)
            null
        }
    }

    companion object {
        fun isGamepadSource(sources: Int): Boolean =
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }
}
