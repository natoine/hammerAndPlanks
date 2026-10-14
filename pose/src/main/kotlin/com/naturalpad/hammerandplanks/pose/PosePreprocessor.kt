package com.naturalpad.hammerandplanks.pose

import kotlin.math.floor

/**
 * Prétraitement d'InstantHMR, porté de instanthmr/inference.py (InstantHMR._preprocess) :
 * carré 1,2× autour de la boîte de la personne, bords noirs hors image, redimensionné en 224×224
 * (bilinéaire, centres de pixels comme cv2.INTER_LINEAR), normalisation ImageNet, tenseur NCHW ;
 * et vecteur CLIFF sous la forme « pixels normalisés » (le modèle publié n'a pas de métadonnée cliff_focal).
 */
object PosePreprocessor {
    const val INPUT_SIZE = 224
    const val CROP_EXPAND = 1.2f
    val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

    /** Carré découpé dans l'image : coin haut gauche et côté, en pixels de l'image. */
    data class Crop(val x1: Float, val y1: Float, val size: Float)

    fun crop(box: Box): Crop {
        require(box.isValid) { "Boîte invalide : $box" }
        val size = box.side * CROP_EXPAND
        return Crop(box.centerX - size / 2f, box.centerY - size / 2f, size)
    }

    /** Conditionnement CLIFF : [cx_norm, cy_norm, b_scale] de la boîte dans l'image entière. */
    fun cliff(box: Box, imageWidth: Int, imageHeight: Int): FloatArray {
        require(imageWidth > 0 && imageHeight > 0) { "Image vide" }
        return floatArrayOf(
            2f * box.centerX / imageWidth - 1f,
            2f * box.centerY / imageHeight - 1f,
            box.side / maxOf(imageWidth, imageHeight),
        )
    }

    /**
     * Remplit [out] (3×224×224, NCHW) à partir d'une image RGB 8 bits ([rgb], [width]×[height], 3 octets par pixel,
     * lignes de haut en bas, avec un pas de [rowStride] octets).
     */
    fun fillTensor(rgb: ByteArray, width: Int, height: Int, crop: Crop, out: FloatArray, rowStride: Int = width * 3) {
        val n = INPUT_SIZE
        require(out.size >= 3 * n * n) { "Tenseur trop petit : ${out.size}" }
        require(rgb.size >= rowStride * (height - 1) + width * 3) { "Image trop petite pour ${width}x$height" }
        // Comme le Python : découpe sur des bornes entières (floor / ceil), puis redimensionnement de cette pièce.
        val ix1 = floor(crop.x1).toInt()
        val iy1 = floor(crop.y1).toInt()
        val ix2 = kotlin.math.ceil(crop.x1 + crop.size).toInt()
        val iy2 = kotlin.math.ceil(crop.y1 + crop.size).toInt()
        val patchW = ix2 - ix1
        val patchH = iy2 - iy1
        val scaleX = patchW.toFloat() / n
        val scaleY = patchH.toFloat() / n
        val plane = n * n
        for (dy in 0 until n) {
            val sy = (dy + 0.5f) * scaleY - 0.5f
            val y0 = floor(sy).toInt()
            val fy = sy - y0
            for (dx in 0 until n) {
                val sx = (dx + 0.5f) * scaleX - 0.5f
                val x0 = floor(sx).toInt()
                val fx = sx - x0
                val o = dy * n + dx
                for (c in 0 until 3) {
                    val p00 = sample(rgb, width, height, rowStride, ix1, iy1, patchW, patchH, x0, y0, c)
                    val p10 = sample(rgb, width, height, rowStride, ix1, iy1, patchW, patchH, x0 + 1, y0, c)
                    val p01 = sample(rgb, width, height, rowStride, ix1, iy1, patchW, patchH, x0, y0 + 1, c)
                    val p11 = sample(rgb, width, height, rowStride, ix1, iy1, patchW, patchH, x0 + 1, y0 + 1, c)
                    val top = p00 + (p10 - p00) * fx
                    val bottom = p01 + (p11 - p01) * fx
                    val value = (top + (bottom - top) * fy) / 255f
                    out[c * plane + o] = (value - MEAN[c]) / STD[c]
                }
            }
        }
    }

    /**
     * Pixel (px, py) de la pièce découpée, canal [c], de 0 à 255. Hors de la pièce : bord répliqué
     * (comme cv2.resize) ; dans la pièce mais hors de l'image : noir (bordure de copyMakeBorder).
     */
    private fun sample(
        rgb: ByteArray, width: Int, height: Int, rowStride: Int,
        ix1: Int, iy1: Int, patchW: Int, patchH: Int, px: Int, py: Int, c: Int,
    ): Float {
        val x = ix1 + px.coerceIn(0, patchW - 1)
        val y = iy1 + py.coerceIn(0, patchH - 1)
        if (x < 0 || y < 0 || x >= width || y >= height) return 0f
        return (rgb[y * rowStride + x * 3 + c].toInt() and 0xFF).toFloat()
    }

    /** Convertit les points 2D du modèle ([-1, 1] dans le carré) en pixels de l'image entière. */
    fun jointsToImage(joints2dNorm: FloatArray, crop: Crop, out: FloatArray = FloatArray(joints2dNorm.size)): FloatArray {
        val scale = crop.size / INPUT_SIZE
        for (i in 0 until joints2dNorm.size / 2) {
            out[2 * i] = (joints2dNorm[2 * i] + 1f) * 0.5f * INPUT_SIZE * scale + crop.x1
            out[2 * i + 1] = (joints2dNorm[2 * i + 1] + 1f) * 0.5f * INPUT_SIZE * scale + crop.y1
        }
        return out
    }
}
