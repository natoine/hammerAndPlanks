package com.naturalpad.hammerandplanks.input

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardSteeringTest {
    @Test fun `fleche gauche`() = assertEquals(-1f, KeyboardSteering.axis(leftPressed = true, rightPressed = false))
    @Test fun `fleche droite`() = assertEquals(1f, KeyboardSteering.axis(leftPressed = false, rightPressed = true))
    @Test fun `aucune fleche`() = assertEquals(0f, KeyboardSteering.axis(leftPressed = false, rightPressed = false))
    @Test fun `deux fleches s annulent`() = assertEquals(0f, KeyboardSteering.axis(leftPressed = true, rightPressed = true))
}

class GameInputTest {
    @Test fun `le clavier produit un GameInput`() {
        assertEquals(GameInput(-1f), KeyboardSteering.read(leftPressed = true, rightPressed = false))
        assertEquals(GameInput.IDLE, KeyboardSteering.read(leftPressed = false, rightPressed = false))
    }

    @Test fun `axe non fini refuse`() {
        kotlin.test.assertFailsWith<IllegalArgumentException> { GameInput(Float.NaN) }
    }
}
