package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.input.MotionInput
import com.naturalpad.hammerandplanks.input.MotionStatus
import com.naturalpad.hammerandplanks.pose.PoseSource
import com.naturalpad.hammerandplanks.pose.PoseStatus

/** Assemblage pose → gestes → GameInput, utilisé par les lanceurs pour le mode caméra. */
class PoseMotionInput(
    private val poses: PoseSource,
    val steering: MotionSteering = MotionSteering(),
) : MotionInput {
    override val status: MotionStatus
        get() = when (poses.status) {
            PoseStatus.OFF -> MotionStatus.OFF
            PoseStatus.LOADING -> MotionStatus.LOADING
            PoseStatus.NO_MODEL -> MotionStatus.NO_MODEL
            PoseStatus.ERROR -> MotionStatus.ERROR
            PoseStatus.RUNNING -> if (steering.neutralDeg == null) MotionStatus.CALIBRATING else MotionStatus.RUNNING
        }

    override fun start() {
        steering.recalibrate() // chaque entrée en mode caméra remesure la posture neutre
        poses.start()
    }

    override fun stop() = poses.stop()

    override fun read(): GameInput = steering.update(poses.latest(), poses.nowS())
}
