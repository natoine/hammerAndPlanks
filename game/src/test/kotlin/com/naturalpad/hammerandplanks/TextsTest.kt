package com.naturalpad.hammerandplanks

import com.badlogic.gdx.files.FileHandle
import java.io.File
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TextsTest {
    private val base = FileHandle(File(System.getProperty("assetsDir") ?: "../assets", "i18n/messages"))
    private lateinit var systemLocale: Locale

    @BeforeTest fun saveLocale() { systemLocale = Locale.getDefault() }
    @AfterTest fun restoreLocale() = Locale.setDefault(systemLocale)

    @Test fun `francais charge le fichier francais`() {
        assertEquals("Un shooter de pirates qui se pilote avec le corps", Texts.load(base, "fr").get("launch.tagline"))
    }

    @Test fun `anglais demande sur un systeme francais reste en anglais`() {
        Locale.setDefault(Locale.FRANCE)
        assertEquals("A pirate shooter you steer with your body", Texts.load(base, "en").get("launch.tagline"))
    }

    @Test fun `francais demande sur un systeme anglais reste en francais`() {
        Locale.setDefault(Locale.US)
        assertEquals("Un shooter de pirates qui se pilote avec le corps", Texts.load(base, "fr").get("launch.tagline"))
    }

    @Test fun `parametre de version formate`() {
        assertEquals("Version 1.2.3", Texts.load(base, "fr").format("launch.version", "1.2.3"))
    }

    @Test fun `cle absente signalee`() {
        assertFailsWith<java.util.MissingResourceException> { Texts.load(base, "en").get("cle.inexistante") }
    }
}
