package com.naturalpad.hammerandplanks.android

import android.view.InputDevice
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidGamepadsTest {
    @Test fun `manette reconnue`() = assertTrue(AndroidGamepads.isGamepadSource(InputDevice.SOURCE_GAMEPAD))

    @Test fun `joystick reconnu`() = assertTrue(AndroidGamepads.isGamepadSource(InputDevice.SOURCE_JOYSTICK))

    @Test fun `manette qui se declare aussi clavier`() =
        assertTrue(AndroidGamepads.isGamepadSource(InputDevice.SOURCE_GAMEPAD or InputDevice.SOURCE_KEYBOARD))

    @Test fun `ecran tactile et clavier seuls ignores`() {
        assertFalse(AndroidGamepads.isGamepadSource(InputDevice.SOURCE_TOUCHSCREEN))
        assertFalse(AndroidGamepads.isGamepadSource(InputDevice.SOURCE_KEYBOARD))
        assertFalse(AndroidGamepads.isGamepadSource(0))
    }
}
