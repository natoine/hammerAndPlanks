package com.naturalpad.hammerandplanks.launch

import com.naturalpad.hammerandplanks.control.ControlSettings
import com.naturalpad.hammerandplanks.control.MovementControlMode
import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.util.SmoothDamp

/**
 * Déplacement latéral du bateau, repris de PlayerHorizontalMovement de l'ancien jeu : trois modes de contrôle
 * (position, vitesse, force), lissage SmoothDamp de 0,6 s, vitesse maximale et rail borné de l'ancien jeu.
 * Ajout : l'inclinaison dans le virage.
 *
 * Le mode position est un portage exact. Les modes vitesse et force reprennent l'intention et non la formule :
 * dans l'ancien code, la cible n'avançait que d'un pas d'image avant le lissage, si bien que le bateau ne
 * bougeait presque pas (moins d'une unité en 5 s sur un demi-rail de 16 à 60 images/s) et dépendait de la
 * fréquence d'images. Ici, le mode vitesse lisse la vitesse vers celle demandée, le mode force intègre une
 * accélération sans frottement.
 *
 * Coordonnées de rail : -1 au bord gauche, 0 au centre, 1 au bord droit. L'écran convertit en pixels
 * (voir [LaunchLayout.boatLimit]), ce qui reste juste après un redimensionnement.
 */
class BoatMotion(
    var settings: ControlSettings = ControlSettings(),
    /** Vitesse maximale en demi-rails par seconde : 6 unités/s sur un rail de 32 dans l'ancien jeu. */
    val maxSpeed: Float = OLD_MAX_SPEED / OLD_HALF_RAIL,
    smoothTimeS: Float = 0.6f,
    /** Inclinaison maximale du sprite à pleine vitesse, en degrés. */
    val maxTiltDeg: Float = 14f,
) {
    init {
        require(maxSpeed > 0f) { "Vitesse maximale invalide : $maxSpeed" }
    }

    /** Lissage de la position (mode position) et de la vitesse (mode vitesse). */
    private val positionDamp = SmoothDamp(smoothTimeS)
    private val velocityDamp = SmoothDamp(smoothTimeS)

    /** Position sur le rail, dans [-1, 1]. */
    var position: Float = 0f
        private set

    /** Vitesse en demi-rails par seconde (négative vers la gauche). */
    var velocity: Float = 0f
        private set

    private val speedCap: Float get() = maxSpeed * settings.speedFactor

    /** Rotation du sprite en degrés (sens libGDX : positif = antihoraire, proue vers la gauche quand on va à gauche). */
    val tiltDeg: Float
        get() = if (velocity == 0f || speedCap == 0f) 0f
        else (-velocity / speedCap).coerceIn(-1f, 1f) * maxTiltDeg

    /** Avance de [deltaS] secondes. Un grand écart de temps (application en pause) est borné pour éviter un saut. */
    fun update(input: GameInput, deltaS: Float) {
        val dt = deltaS.coerceIn(0f, MAX_STEP_S)
        if (dt == 0f) return
        val axis = input.moveX.coerceIn(-1f, 1f)
        when (settings.mode) {
            MovementControlMode.POSITION -> {
                // L'axe désigne la position visée sur le rail (portage exact de l'ancien jeu).
                positionDamp.velocity = velocity
                position = positionDamp.step(position, axis, speedCap, dt)
                velocity = positionDamp.velocity
            }
            MovementControlMode.VELOCITY -> {
                // L'axe désigne la vitesse visée, rejointe en douceur.
                velocity = velocityDamp.step(velocity, axis * speedCap, Float.MAX_VALUE, dt)
                position += velocity * dt
            }
            MovementControlMode.FORCE -> {
                // L'axe accélère le bateau ; sans appui, il garde sa lancée.
                velocity = (velocity + axis * maxSpeed * dt).coerceIn(-speedCap, speedCap)
                position += velocity * dt
            }
        }
        // Le rail est borné : le bateau s'arrête contre le bord.
        if (position > 1f) { position = 1f; if (velocity > 0f) velocity = 0f }
        if (position < -1f) { position = -1f; if (velocity < 0f) velocity = 0f }
    }

    /** Ramène le bateau au centre, immobile. */
    fun reset() {
        position = 0f
        velocity = 0f
        positionDamp.reset()
        velocityDamp.reset()
    }

    companion object {
        const val MAX_STEP_S = 0.1f
        /** Valeurs de l'ancien jeu, en unités Unity : PlayerHorizontalMovement.maxPlayerSpeed et demi-largeur du rail. */
        const val OLD_MAX_SPEED = 6f
        const val OLD_HALF_RAIL = 16f
    }
}
