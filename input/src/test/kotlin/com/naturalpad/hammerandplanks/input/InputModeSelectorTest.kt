package com.naturalpad.hammerandplanks.input

import kotlin.test.Test
import kotlin.test.assertEquals

class InputModeSelectorTest {
    @Test fun `le clavier est le mode par defaut`() = assertEquals(InputMode.KEYBOARD, InputModeSelector().current)

    @Test fun `tab fait le tour clavier manette camera puis revient au clavier`() {
        val selector = InputModeSelector()
        assertEquals(InputMode.GAMEPAD, selector.next())
        assertEquals(InputMode.MOTION, selector.next())
        assertEquals(InputMode.KEYBOARD, selector.next())
    }

    @Test fun `le changement est notifie a chaque bascule`() {
        val seen = mutableListOf<InputMode>()
        val selector = InputModeSelector(onChange = { seen += it })
        repeat(4) { selector.next() }
        assertEquals(listOf(InputMode.GAMEPAD, InputMode.MOTION, InputMode.KEYBOARD, InputMode.GAMEPAD), seen)
    }

    @Test fun `choisir le mode courant ne notifie pas`() {
        val seen = mutableListOf<InputMode>()
        val selector = InputModeSelector(InputMode.MOTION, onChange = { seen += it })
        selector.select(InputMode.MOTION)
        assertEquals(emptyList(), seen)
    }

    @Test fun `aller retour par la sauvegarde`() {
        for (mode in InputMode.entries) {
            assertEquals(mode, InputModeSelector.parse(InputModeSelector(mode).storageKey))
        }
    }

    @Test fun `sauvegarde tolerante a la casse et aux espaces`() =
        assertEquals(InputMode.GAMEPAD, InputModeSelector.parse(" gamepad "))

    @Test fun `sauvegarde absente vide ou inconnue donne le clavier`() {
        assertEquals(InputMode.KEYBOARD, InputModeSelector.parse(null))
        assertEquals(InputMode.KEYBOARD, InputModeSelector.parse(""))
        assertEquals(InputMode.KEYBOARD, InputModeSelector.parse("TOUCH"))
    }
}
