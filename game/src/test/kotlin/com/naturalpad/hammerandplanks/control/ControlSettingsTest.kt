package com.naturalpad.hammerandplanks.control

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ControlSettingsTest {
    @Test fun `valeurs par defaut de l ancien jeu`() {
        assertEquals(ControlSettings(MovementControlMode.POSITION, 1f), ControlSettings())
        assertEquals(ControlSettings(), ControlSettings.parse(null, null))
    }

    @Test fun `lecture des trois modes, casse et espaces toleres`() {
        assertEquals(MovementControlMode.POSITION, ControlSettings.parse("position", null).mode)
        assertEquals(MovementControlMode.VELOCITY, ControlSettings.parse(" Velocity ", null).mode)
        assertEquals(MovementControlMode.FORCE, ControlSettings.parse("FORCE", null).mode)
    }

    @Test fun `facteur de vitesse lu dans 0 1`() {
        assertEquals(0.5f, ControlSettings.parse(null, "0.5").speedFactor)
        assertEquals(0f, ControlSettings.parse(null, "0").speedFactor)
    }

    @Test fun `valeurs invalides remplacees par les valeurs par defaut`() {
        assertEquals(ControlSettings(), ControlSettings.parse("turbo", "abc"))
        assertEquals(1f, ControlSettings.parse(null, "2").speedFactor)
        assertEquals(1f, ControlSettings.parse(null, "-0.1").speedFactor)
        assertEquals(1f, ControlSettings.parse(null, "NaN").speedFactor)
    }

    @Test fun `construction directe hors bornes refusee`() {
        assertFailsWith<IllegalArgumentException> { ControlSettings(speedFactor = 1.5f) }
    }
}
