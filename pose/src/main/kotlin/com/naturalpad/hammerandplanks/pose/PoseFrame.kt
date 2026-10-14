package com.naturalpad.hammerandplanks.pose

/**
 * Résultat d'une inférence InstantHMR pour une image.
 * [joints3d] : 70×3, mètres, centré sur le corps, axes de la caméra (x à droite de l'image, y vers le bas).
 * [joints2d] : 70×2, pixels de l'image source (origine en haut à gauche).
 */
class PoseFrame(
    val joints3d: FloatArray,
    val joints2d: FloatArray,
    val imageWidth: Int,
    val imageHeight: Int,
    /** Horodatage de l'image, en secondes (horloge monotone de la plateforme). */
    val timestampS: Double,
) {
    init {
        require(joints3d.size == Mhr70.COUNT * 3) { "joints3d : ${joints3d.size} valeurs au lieu de ${Mhr70.COUNT * 3}" }
        require(joints2d.size == Mhr70.COUNT * 2) { "joints2d : ${joints2d.size} valeurs au lieu de ${Mhr70.COUNT * 2}" }
    }

    fun x3(joint: Int) = joints3d[3 * joint]
    fun y3(joint: Int) = joints3d[3 * joint + 1]
    fun z3(joint: Int) = joints3d[3 * joint + 2]
}
