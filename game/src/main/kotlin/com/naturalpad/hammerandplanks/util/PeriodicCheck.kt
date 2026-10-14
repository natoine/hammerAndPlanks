package com.naturalpad.hammerandplanks.util

/**
 * Exécute une vérification au plus une fois par [intervalS] (ex. détecter une manette une fois par seconde
 * plutôt qu'à chaque image) et ne signale que les changements de résultat.
 */
class PeriodicCheck<T>(private val intervalS: Float, private val check: () -> T) {
    init {
        require(intervalS > 0f) { "Intervalle invalide : $intervalS" }
    }

    private var elapsed = 0f
    private var initialized = false

    var value: T? = null
        private set

    /** Fait avancer le temps de [deltaS] ; renvoie vrai si la valeur a changé (et au tout premier appel). */
    fun update(deltaS: Float): Boolean {
        elapsed += deltaS
        if (initialized && elapsed < intervalS) return false
        elapsed = 0f
        val next = check()
        val changed = !initialized || next != value
        value = next
        initialized = true
        return changed
    }

    /** Force une vérification au prochain [update]. */
    fun invalidate() {
        elapsed = intervalS
    }
}
