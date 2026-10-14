package com.naturalpad.hammerandplanks.web

import com.naturalpad.hammerandplanks.pose.Mhr70
import com.naturalpad.hammerandplanks.pose.PoseFrame
import com.naturalpad.hammerandplanks.pose.PoseSource
import com.naturalpad.hammerandplanks.pose.PoseStatus

/** Poses estimées par webapp/pose.js (onnxruntime-web), relues côté Kotlin pour les gestes partagés. */
class WebPoseSource : PoseSource {
    private var cachedTime = -1.0
    private var cached: PoseFrame? = null

    override val status: PoseStatus
        get() = PoseStatus.entries.firstOrNull { it.name == poseStatus() } ?: PoseStatus.ERROR

    override fun latest(): PoseFrame? {
        val time = poseTime()
        if (time < 0) return null
        if (time != cachedTime) {
            cachedTime = time
            cached = PoseFrame(
                joints3d = FloatArray(Mhr70.COUNT * 3) { poseJoint3d(it) },
                joints2d = FloatArray(Mhr70.COUNT * 2) { poseJoint2d(it) },
                imageWidth = poseWidth(),
                imageHeight = poseHeight(),
                timestampS = time,
            )
        }
        return cached
    }

    override fun nowS(): Double = poseNow()

    override fun start() = poseStart()

    override fun stop() {
        poseStop()
        cached = null
        cachedTime = -1.0
    }
}
