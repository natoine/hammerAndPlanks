package com.naturalpad.hammerandplanks

/**
 * Ce que chaque lanceur (desktop, Android, iOS, web) fournit au jeu partagé.
 * Implémenté par injection, sans expect/actual (voir README, section 4).
 */
interface PlatformHooks {
    /** Langue préférée du système ou du navigateur, au format BCP 47 (ex. "fr-FR", "en-US"). */
    val preferredLanguageTag: String

    /** Appelé quand l'écran de lancement s'affiche, avec les textes traduits (accessibilité web, par exemple). */
    fun onLaunchScreenShown(language: String, title: String, tagline: String) {}

    /** Appelé à l'affichage et à chaque changement de mode d'interaction, avec une phrase traduite à annoncer. */
    fun onInputModeChanged(announcement: String) {}
}
