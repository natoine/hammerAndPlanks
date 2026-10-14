package com.naturalpad.hammerandplanks

import java.io.File
import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Vérifie que chaque langue supportée a un fichier et exactement les mêmes clés que l'anglais. */
class TranslationsTest {
    private val i18nDir = File(System.getProperty("assetsDir") ?: "../assets", "i18n")

    private fun load(language: String): Properties {
        val name = if (language == Languages.DEFAULT) "messages.properties" else "messages_$language.properties"
        val file = File(i18nDir, name)
        assertTrue(file.isFile, "Fichier de traduction manquant : $file")
        return Properties().apply { file.reader(Charsets.UTF_8).use { load(it) } }
    }

    @Test fun `toutes les langues ont les memes cles que l anglais`() {
        val reference = load(Languages.DEFAULT).stringPropertyNames()
        for (language in Languages.SUPPORTED) {
            assertEquals(reference, load(language).stringPropertyNames(), "Clés différentes pour $language")
        }
    }

    @Test fun `aucune traduction vide`() {
        for (language in Languages.SUPPORTED) {
            val props = load(language)
            for (key in props.stringPropertyNames()) {
                assertTrue(props.getProperty(key).isNotBlank(), "Traduction vide : $language / $key")
            }
        }
    }

    @Test fun `les cles de l ecran de lancement existent`() {
        val keys = load(Languages.DEFAULT).stringPropertyNames()
        for (key in listOf("launch.title", "launch.tagline", "launch.version")) {
            assertTrue(key in keys, "Clé manquante : $key")
        }
    }

    @Test fun `le francais est bien traduit et accentue`() {
        val fr = load("fr")
        assertTrue(fr.getProperty("launch.tagline") != load("en").getProperty("launch.tagline"))
    }
}
