package com.naturalpad.hammerandplanks.launch

import com.naturalpad.hammerandplanks.control.ControlSettings
import com.naturalpad.hammerandplanks.control.MovementControlMode
import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.util.SmoothDamp

/**
 * Déplacement latéral du bateau. Mode élan par défaut (arcade, voir [MomentumTuning]) ; modes position, vitesse
 * et force repris de PlayerHorizontalMovement de l'ancien jeu (lissage SmoothDamp de 0,6 s, vitesse maximale
 * de l'ancien jeu). Rail borné. Ajout : l'inclinaison dans le virage.
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
    /** Réglages du mode élan. */
    val momentum: MomentumTuning = MomentumTuning(),
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

    /** Vitesse de référence de l'inclinaison : vitesse de pointe du mode courant. */
    private val tiltSpeed: Float
        get() = if (settings.mode == MovementControlMode.MOMENTUM) momentum.maxSpeed * settings.speedFactor else speedCap

    /** Rotation du sprite en degrés (sens libGDX : positif = antihoraire, proue vers la gauche quand on va à gauche). */
    val tiltDeg: Float
        get() = if (velocity == 0f || tiltSpeed == 0f) 0f
        else (-velocity / tiltSpeed).coerceIn(-1f, 1f) * maxTiltDeg

    /** Mode élan : durée de poussée continue dans la même direction, en secondes, et cette direction. */
    var holdTimeS: Float = 0f
        private set
    private var holdDirection = 0f

    /** Avance de [deltaS] secondes. Un grand écart de temps (application en pause) est borné pour éviter un saut. */
    fun update(input: GameInput, deltaS: Float) {
        val dt = deltaS.coerceIn(0f, MAX_STEP_S)
        if (dt == 0f) return
        val axis = input.moveX.coerceIn(-1f, 1f)
        when (settings.mode) {
            MovementControlMode.MOMENTUM -> updateMomentum(axis, dt)
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
        // Le rail est borné : arrêt contre le bord, ou rebond en mode élan.
        val restitution = if (settings.mode == MovementControlMode.MOMENTUM) momentum.bounce else 0f
        if (position > 1f) { position = 1f; if (velocity > 0f) velocity = -velocity * restitution }
        if (position < -1f) { position = -1f; if (velocity < 0f) velocity = -velocity * restitution }
    }

    /**
     * Élan : la poussée grandit avec la durée d'appui continu dans la même direction (de [MomentumTuning.baseAccel]
     * à [MomentumTuning.boost] fois plus), l'axe analogique (stick, inclinaison) la dose ; à contresens, freinage
     * renforcé ; sans appui, rappel vers le centre amorti par le frottement de l'eau (glisse puis retour au centre).
     */
    private fun updateMomentum(axis: Float, dt: Float) {
        val m = momentum
        val direction = if (kotlin.math.abs(axis) < m.inputThreshold) 0f else kotlin.math.sign(axis)
        if (direction != 0f && direction == holdDirection) holdTimeS += dt else { holdTimeS = 0f; holdDirection = direction }
        val cap = m.maxSpeed * settings.speedFactor
        if (direction == 0f) {
            // Sans appui : rappel vers le centre et frottement de l'eau (le bateau glisse un peu puis revient).
            velocity += -m.centering * position * dt
            velocity *= kotlin.math.exp(-m.drag * dt)
            // En revenant vers le centre, le bateau va un peu moins vite qu'à l'aller (la glisse n'est pas bridée).
            if (velocity != 0f && position != 0f && kotlin.math.sign(velocity) != kotlin.math.sign(position)) {
                val returnCap = m.maxSpeed * m.returnSpeedRatio * settings.speedFactor
                velocity = velocity.coerceIn(-returnCap, returnCap)
            }
            if (kotlin.math.abs(velocity) < m.stopSpeed && kotlin.math.abs(position) < m.centerSnap) {
                velocity = 0f
                position = 0f
            }
        } else {
            val ramp = (holdTimeS / m.rampTimeS).coerceIn(0f, 1f)
            var accel = axis * m.baseAccel * (1f + (m.boost - 1f) * ramp)
            if (velocity != 0f && kotlin.math.sign(velocity) != direction) {
                // Contre-braquage : poussée renforcée et frottement de l'eau, plus efficace que de relâcher.
                accel *= m.counterBrake
                velocity *= kotlin.math.exp(-m.drag * dt)
            }
            velocity = (velocity + accel * dt * settings.speedFactor).coerceIn(-cap, cap)
        }
        position += velocity * dt
    }

    /** Ramène le bateau au centre, immobile. */
    fun reset() {
        position = 0f
        velocity = 0f
        holdTimeS = 0f
        holdDirection = 0f
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

/** Réglages du mode élan, en demi-rails (rail de -1 à 1) et secondes. À ajuster au ressenti. */
data class MomentumTuning(
    /** Poussée au premier instant, en demi-rails par seconde². */
    val baseAccel: Float = 0.625f,
    /** Multiplicateur atteint après [rampTimeS] de poussée continue : l'accélération ne cesse de croître. */
    val boost: Float = 3f,
    val rampTimeS: Float = 1.2f,
    /** Vitesse de pointe, en demi-rails par seconde (l'ancien jeu plafonnait à 0,375). */
    val maxSpeed: Float = 0.75f,
    /** Frottement sans appui, par seconde : la vitesse est divisée par e en 1/drag seconde. */
    val drag: Float = 5f,
    /**
     * Rappel vers le centre sans appui, en demi-rails par seconde² par demi-rail d'écart. Avec [drag], retour
     * sans dépasser le centre, bridé par [returnSpeedRatio]. 0 : pas de retour.
     */
    val centering: Float = 8f,
    /** Vitesse maximale du retour au centre, en fraction de [maxSpeed] : un peu plus lent que l'aller. */
    val returnSpeedRatio: Float = 0.6f,
    /** Écart au centre en dessous duquel le bateau, presque arrêté, se cale au centre. */
    val centerSnap: Float = 0.005f,
    /** Multiplicateur de poussée à contresens (freinage nerveux). */
    val counterBrake: Float = 6f,
    /** Part de la vitesse rendue au rebond sur un bord. */
    val bounce: Float = 0.35f,
    /** Axe ignoré en dessous de ce seuil (bruit d'un stick ou d'une pose). */
    val inputThreshold: Float = 0.05f,
    /** En dessous, le bateau est arrêté. */
    val stopSpeed: Float = 0.01f,
) {
    init {
        require(baseAccel > 0f && boost >= 1f && rampTimeS > 0f && maxSpeed > 0f && drag >= 0f) { "Réglages d'élan invalides" }
        require(counterBrake >= 1f && bounce in 0f..1f && inputThreshold in 0f..<1f && stopSpeed >= 0f) { "Réglages d'élan invalides" }
        require(centering >= 0f && centerSnap >= 0f && returnSpeedRatio > 0f) { "Réglages d'élan invalides" }
    }
}
