package com.naturalpad.hammerandplanks.ui

import com.naturalpad.hammerandplanks.Texts
import com.naturalpad.hammerandplanks.Languages
import com.naturalpad.hammerandplanks.input.InputMode
import com.badlogic.gdx.files.FileHandle
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputModeBadgeLayoutTest {
    private val screens = listOf(360 to 640, 1080 to 2400, 1920 to 1080, 540 to 960)

    @Test fun `badge ancre en bas a gauche dans la marge`() {
        val l = InputModeBadgeLayout.compute(540, 960, margin = 27f)
        assertEquals(27f, l.x)
        assertEquals(27f, l.y)
    }

    @Test fun `zone tactile assez haute sur tous les ecrans`() {
        for ((w, h) in screens) {
            val l = InputModeBadgeLayout.compute(w, h, 10f)
            assertTrue(l.height(1f) >= 44f, "Zone tactile trop petite en ${w}x$h")
        }
    }

    @Test fun `le badge grandit avec son contenu`() {
        val l = InputModeBadgeLayout.compute(540, 960, 10f)
        assertTrue(l.width(200f) > l.width(100f))
        assertTrue(l.height(500f) > l.height(10f))
    }

    @Test fun `textes jamais sous le minimum lisible`() {
        val l = InputModeBadgeLayout.compute(100, 100, 5f)
        assertTrue(l.labelPx >= InputModeBadgeLayout.MIN_FONT_PX && l.hintPx >= InputModeBadgeLayout.MIN_FONT_PX)
    }

    @Test fun `un picto distinct et present dans assets pour chaque mode`() {
        val paths = InputMode.entries.map(InputModeBadgeLayout::iconPath)
        assertEquals(paths.size, paths.toSet().size)
        val assets = File(System.getProperty("assetsDir") ?: "../assets")
        for (path in paths) assertTrue(File(assets, path).isFile, "Picto manquant : $path")
    }

    @Test fun `chaque mode a son nom traduit en francais et en anglais`() {
        val base = FileHandle(File(System.getProperty("assetsDir") ?: "../assets", "i18n/messages"))
        for (language in Languages.SUPPORTED) {
            val texts = Texts.load(base, language)
            for (mode in InputMode.entries) {
                assertTrue(texts.get(InputModeBadgeLayout.labelKey(mode)).isNotBlank())
            }
        }
    }

    @Test fun `annonce traduite avec apostrophe conservee`() {
        val base = FileHandle(File(System.getProperty("assetsDir") ?: "../assets", "i18n/messages"))
        assertEquals("Mode d'interaction : Manette", Texts.load(base, "fr").format("input.mode.announce", "Manette"))
        assertEquals("Interaction mode: Gamepad", Texts.load(base, "en").format("input.mode.announce", "Gamepad"))
    }

    @Test fun `dimensions invalides refusees`() {
        assertFailsWith<IllegalArgumentException> { InputModeBadgeLayout.compute(0, 100, 5f) }
        assertFailsWith<IllegalArgumentException> { InputModeBadgeLayout.compute(100, 100, -1f) }
    }

    @Test fun `zone d appui inclusive sur les bords`() {
        val box = HitBox(10f, 20f, 100f, 50f)
        assertTrue(box.contains(10f, 20f))
        assertTrue(box.contains(110f, 70f))
        assertTrue(box.contains(60f, 40f))
        assertFalse(box.contains(9f, 40f))
        assertFalse(box.contains(60f, 71f))
    }
}
