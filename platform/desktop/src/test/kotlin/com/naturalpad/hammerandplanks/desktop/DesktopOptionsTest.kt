package com.naturalpad.hammerandplanks.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class DesktopOptionsTest {
    @Test fun `sans option on garde le format portrait par defaut`() {
        val o = DesktopOptions.parse(emptyArray())
        assertEquals(DesktopOptions.DEFAULT_WIDTH, o.width)
        assertEquals(DesktopOptions.DEFAULT_HEIGHT, o.height)
        assertNull(o.language)
        assertNull(o.screenshotPath)
    }

    @Test fun `toutes les options combinees`() {
        val o = DesktopOptions.parse(arrayOf("--lang=fr", "--size=1920x1080", "--screenshot=/tmp/a.png"))
        assertEquals(DesktopOptions("fr", 1920, 1080, "/tmp/a.png"), o)
    }

    @Test fun `taille en majuscules acceptee`() = assertEquals(800, DesktopOptions.parse(arrayOf("--size=800X600")).width)

    @Test fun `langue vide ignoree`() = assertNull(DesktopOptions.parse(arrayOf("--lang=")).language)

    @Test fun `taille invalide refusee`() {
        for (bad in listOf("--size=abc", "--size=800", "--size=0x600", "--size=-5x600", "--size=")) {
            assertFailsWith<IllegalArgumentException>(bad) { DesktopOptions.parse(arrayOf(bad)) }
        }
    }

    @Test fun `mode d interaction impose`() {
        assertEquals("MOTION", DesktopOptions.parse(arrayOf("--input-mode=motion")).inputMode)
        assertEquals("GAMEPAD", DesktopOptions.parse(arrayOf("--input-mode=Gamepad")).inputMode)
    }

    @Test fun `mode d interaction inconnu refuse`() {
        assertFailsWith<IllegalArgumentException> { DesktopOptions.parse(arrayOf("--input-mode=touch")) }
    }

    @Test fun `option inconnue refusee`() {
        assertFailsWith<IllegalArgumentException> { DesktopOptions.parse(arrayOf("--plein-ecran")) }
    }
}
