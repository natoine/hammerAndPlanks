package com.naturalpad.hammerandplanks.input

/** État du pilotage par le corps, affiché au joueur. */
enum class MotionStatus {
    /** Arrêté. */
    OFF,
    /** Chargement du modèle de pose. */
    LOADING,
    /** Mesure de la posture neutre : le joueur doit se tenir droit. */
    CALIBRATING,
    /** Le corps pilote le bateau. */
    RUNNING,
    /** Modèle de pose absent. */
    NO_MODEL,
    /** Erreur du modèle de pose. */
    ERROR,
}

/**
 * Mode caméra vu du jeu : un GameInput et un état, rien de plus. La chaîne caméra → pose → gestes est
 * assemblée par chaque lanceur (voir README, règles de dépendance).
 */
interface MotionInput {
    val status: MotionStatus

    fun start()

    fun stop()

    /** Entrée de jeu courante (appelé à chaque image). */
    fun read(): GameInput

    companion object {
        /** Plateforme sans pilotage par le corps. */
        val NONE: MotionInput = object : MotionInput {
            override val status = MotionStatus.NO_MODEL
            override fun start() {}
            override fun stop() {}
            override fun read() = GameInput.IDLE
        }
    }
}
