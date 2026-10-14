package com.naturalpad.hammerandplanks

import com.naturalpad.hammerandplanks.platform.CameraThumbnail
import com.naturalpad.hammerandplanks.platform.GamepadDetector

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

    /**
     * Appelé quand le bateau change de position, avec son décalage depuis le centre en fraction de la largeur
     * d'écran (négatif à gauche). Sert aux tests d'interface du web.
     */
    fun onBoatMoved(offset: Float) {}

    /** Détection des manettes branchées. */
    val gamepads: GamepadDetector get() = GamepadDetector.NONE

    /** Retour caméra en imagette, utilisé en mode caméra. */
    val camera: CameraThumbnail get() = CameraThumbnail.NONE

    /** Mode d'interaction imposé au lancement (outil de développement), ou null pour reprendre le mode sauvegardé. */
    val forcedInputMode: String? get() = null

    /** Mode de contrôle du bateau imposé (position, velocity, force), ou null pour le réglage sauvegardé. */
    val forcedControlMode: String? get() = null

    /** Facteur de vitesse imposé dans [0, 1], ou null pour le réglage sauvegardé. */
    val forcedSpeedFactor: String? get() = null
}
