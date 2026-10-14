package com.naturalpad.hammerandplanks.pose

/** Conversion des images caméra vers le RGB redressé attendu par [PoseEstimator]. */
object ImageRotation {
    /** Image RGB 24 bits sans padding. */
    class Rgb(val width: Int, val height: Int, val pixels: ByteArray)

    /**
     * Convertit une image RGBA ([rowStride] octets par ligne, 4 octets par pixel) en RGB tourné de
     * [rotationDegrees] dans le sens horaire (0, 90, 180 ou 270 : la rotation à appliquer pour remettre
     * l'image à l'endroit, comme ImageInfo.getRotationDegrees de CameraX).
     */
    fun rgbaToUprightRgb(rgba: ByteArray, width: Int, height: Int, rowStride: Int, rotationDegrees: Int, reuse: ByteArray? = null): Rgb {
        require(width > 0 && height > 0 && rowStride >= width * 4) { "Dimensions invalides" }
        require(rgba.size >= rowStride * (height - 1) + width * 4) { "Image trop petite" }
        val rotation = ((rotationDegrees % 360) + 360) % 360
        require(rotation % 90 == 0) { "Rotation non multiple de 90° : $rotationDegrees" }
        val outW = if (rotation == 90 || rotation == 270) height else width
        val outH = if (rotation == 90 || rotation == 270) width else height
        val out = reuse?.takeIf { it.size == outW * outH * 3 } ?: ByteArray(outW * outH * 3)
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Position du pixel source (x, y) après rotation horaire.
                val (ox, oy) = when (rotation) {
                    90 -> (height - 1 - y) to x
                    180 -> (width - 1 - x) to (height - 1 - y)
                    270 -> y to (width - 1 - x)
                    else -> x to y
                }
                val s = y * rowStride + x * 4
                val d = (oy * outW + ox) * 3
                out[d] = rgba[s]; out[d + 1] = rgba[s + 1]; out[d + 2] = rgba[s + 2]
            }
        }
        return Rgb(outW, outH, out)
    }
}
