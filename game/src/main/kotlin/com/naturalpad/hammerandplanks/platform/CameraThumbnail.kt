package com.naturalpad.hammerandplanks.platform

import com.badlogic.gdx.graphics.Texture

/** État de la caméra frontale, tel que l'affiche l'imagette. */
enum class CameraStatus {
    /** Caméra éteinte (mode d'interaction autre que la caméra). */
    OFF,
    /** Demande d'autorisation ou ouverture en cours. */
    STARTING,
    /** Images reçues. */
    LIVE,
    /** Le joueur ou le système a refusé l'accès. */
    DENIED,
    /** Aucune caméra utilisable sur l'appareil. */
    UNAVAILABLE,
}

/**
 * Retour de la caméra en imagette. La caméra n'est ouverte que sur [start], c'est-à-dire seulement
 * quand le joueur choisit le mode caméra (voir README : caméra à la demande).
 *
 * Deux façons d'afficher l'image selon la plateforme :
 * - le jeu dessine [frame] dans la zone de l'imagette (desktop) ;
 * - ou la plateforme pose sa propre vue native par-dessus le jeu, dans la zone donnée par [place]
 *   (Android : PreviewView, web : élément vidéo) ; [frame] renvoie alors null.
 */
interface CameraThumbnail {
    val status: CameraStatus

    /** Vrai si l'image doit être retournée horizontalement (effet miroir de la caméra frontale). */
    val mirrored: Boolean get() = true

    fun start()

    fun stop()

    /** Zone de l'imagette en fractions de l'écran, origine en haut à gauche (convention des vues natives et du DOM). */
    fun place(left: Float, top: Float, width: Float, height: Float) {}

    /** Dernière image à dessiner par le jeu, ou null si la plateforme l'affiche elle-même. Appelé sur le thread de rendu. */
    fun frame(): Texture? = null

    fun dispose() = stop()

    companion object {
        /** Plateforme sans caméra. */
        val NONE: CameraThumbnail = object : CameraThumbnail {
            override val status = CameraStatus.UNAVAILABLE
            override fun start() {}
            override fun stop() {}
        }
    }
}
