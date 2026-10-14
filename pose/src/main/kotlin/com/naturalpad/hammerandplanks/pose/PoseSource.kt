package com.naturalpad.hammerandplanks.pose

/** État de l'estimation de pose. */
enum class PoseStatus {
    /** Arrêtée (mode d'interaction autre que la caméra). */
    OFF,
    /** Chargement du modèle ou attente des premières images. */
    LOADING,
    /** Poses produites. */
    RUNNING,
    /** Fichier instanthmr.onnx absent (non versionné, voir README). */
    NO_MODEL,
    /** Le modèle n'a pas pu être chargé ou exécuté sur cet appareil. */
    ERROR,
}

/** Source de poses d'une plateforme (InstantHMR sur les images de la caméra). */
interface PoseSource {
    val status: PoseStatus

    /** Dernière pose estimée, ou null. Appelé depuis le thread de rendu. */
    fun latest(): PoseFrame?

    /** Instant présent, sur la même horloge que [PoseFrame.timestampS]. */
    fun nowS(): Double

    fun start()

    fun stop()
}

/** Ce que le modèle renvoie pour une image : points 2D normalisés dans le carré et points 3D. */
class ModelOutput(val joints2dNorm: FloatArray, val joints3d: FloatArray)

/** Exécution du modèle InstantHMR : tenseur image 1×3×224×224 et vecteur CLIFF 1×3. */
fun interface PoseModel {
    fun run(image: FloatArray, cliff: FloatArray): ModelOutput
}
