package com.naturalpad.hammerandplanks

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.I18NBundle
import java.util.Locale

/** Chargement des textes traduits (assets/i18n/messages*.properties). */
object Texts {
    /**
     * Charge le bundle de [language] (code déjà résolu par [Languages.resolve]).
     * La langue par défaut est demandée en Locale.ROOT : sinon I18NBundle, faute de messages_en.properties,
     * retomberait sur la langue du système (le français sur un poste français) au lieu de l'anglais.
     */
    fun load(baseFile: FileHandle, language: String): I18NBundle {
        @Suppress("DEPRECATION") // Locale.of() n'existe ni sur Android minSdk 24 ni sous TeaVM
        val locale = if (language == Languages.DEFAULT) Locale.ROOT else Locale(language)
        return I18NBundle.createBundle(baseFile, locale)
    }
}
