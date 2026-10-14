package com.naturalpad.hammerandplanks.pose

/**
 * Chaîne d'estimation pour une image : zone suivie ([RoiTracker]), prétraitement ([PosePreprocessor]),
 * modèle ([PoseModel]), points 2D ramenés dans l'image, puis mise à jour du suivi.
 * Pas de multithread ici : la plateforme appelle [estimate] depuis son thread d'inférence.
 */
class PoseEstimator(private val model: PoseModel, private val tracker: RoiTracker = RoiTracker()) {
    private val tensor = FloatArray(3 * PosePreprocessor.INPUT_SIZE * PosePreprocessor.INPUT_SIZE)

    fun estimate(rgb: ByteArray, width: Int, height: Int, timestampS: Double, rowStride: Int = width * 3): PoseFrame {
        val box = tracker.current(width, height)
        val crop = PosePreprocessor.crop(box)
        PosePreprocessor.fillTensor(rgb, width, height, crop, tensor, rowStride)
        val output = model.run(tensor, PosePreprocessor.cliff(box, width, height))
        val joints2d = PosePreprocessor.jointsToImage(output.joints2dNorm, crop)
        tracker.update(joints2d, width, height)
        return PoseFrame(output.joints3d, joints2d, width, height, timestampS)
    }

    /** Oublie la zone suivie (nouvelle session caméra). */
    fun reset() = tracker.reset()
}
