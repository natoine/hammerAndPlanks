package com.naturalpad.hammerandplanks.input

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GamepadSteeringTest {
    @Test fun `stick au repos`() = assertEquals(GameInput.IDLE, GamepadSteering.read(StickPosition.CENTER))

    @Test fun `stick a fond a gauche et a droite`() {
        assertEquals(-1f, GamepadSteering.read(StickPosition(-1f, 0f)).moveX, 1e-6f)
        assertEquals(1f, GamepadSteering.read(StickPosition(1f, 0f)).moveX, 1e-6f)
    }

    @Test fun `petite derive du stick ignoree par la zone morte`() {
        assertEquals(0f, GamepadSteering.read(StickPosition(0.1f, -0.12f)).moveX)
        assertEquals(0f, GamepadSteering.read(StickPosition(-0.149f, 0f)).moveX)
    }

    @Test fun `pas de saut a la sortie de la zone morte`() {
        val justOut = GamepadSteering.applyDeadZone(0.16f)
        assertTrue(justOut > 0f && justOut < 0.02f)
    }

    @Test fun `remise a l echelle lineaire`() {
        // Mi-chemin entre la zone morte et la butée donne 0,5.
        assertEquals(0.5f, GamepadSteering.applyDeadZone(0.15f + 0.85f / 2f), 1e-5f)
        assertEquals(-0.5f, GamepadSteering.applyDeadZone(-(0.15f + 0.85f / 2f)), 1e-5f)
    }

    @Test fun `en diagonale le vecteur est borne a 1 comme dans l ancien jeu`() {
        val diag = GamepadSteering.read(StickPosition(1f, 1f)).moveX
        assertEquals(1f / sqrt(2f), diag, 1e-5f)
    }

    @Test fun `valeurs hors bornes ou non finies`() {
        assertEquals(1f, GamepadSteering.applyDeadZone(3f))
        assertEquals(0f, GamepadSteering.applyDeadZone(Float.NaN))
        assertEquals(0f, GamepadSteering.read(StickPosition(Float.NaN, 0f)).moveX)
    }

    @Test fun `zone morte nulle laisse passer la valeur`() = assertEquals(0.05f, GamepadSteering.applyDeadZone(0.05f, 0f))

    @Test fun `zone morte invalide refusee`() {
        assertFailsWith<IllegalArgumentException> { GamepadSteering.applyDeadZone(0.5f, 1f) }
        assertFailsWith<IllegalArgumentException> { GamepadSteering.applyDeadZone(0.5f, -0.1f) }
    }
}
