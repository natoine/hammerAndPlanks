package com.naturalpad.hammerandplanks.android

import android.view.InputDevice
import com.naturalpad.hammerandplanks.platform.GamepadDetector

/** Manette branchée en Bluetooth ou USB : un périphérique physique qui se déclare manette ou joystick. */
class AndroidGamepads : GamepadDetector {
    override fun isConnected(): Boolean = InputDevice.getDeviceIds().any { id ->
        val device = InputDevice.getDevice(id) ?: return@any false
        !device.isVirtual && isGamepadSource(device.sources)
    }

    companion object {
        fun isGamepadSource(sources: Int): Boolean =
            sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ||
                sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
    }
}
