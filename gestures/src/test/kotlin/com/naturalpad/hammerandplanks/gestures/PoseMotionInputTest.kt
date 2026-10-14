package com.naturalpad.hammerandplanks.gestures

import com.naturalpad.hammerandplanks.input.GameInput
import com.naturalpad.hammerandplanks.input.MotionStatus
import com.naturalpad.hammerandplanks.pose.PoseFrame
import com.naturalpad.hammerandplanks.pose.PoseSource
import com.naturalpad.hammerandplanks.pose.PoseStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PoseMotionInputTest {
    private class FakePoses : PoseSource {
        override var status = PoseStatus.OFF
        var frame: PoseFrame? = null
        var now = 0.0
        var started = 0
        override fun latest() = frame
        override fun nowS() = now
        override fun start() { started++; status = PoseStatus.RUNNING }
        override fun stop() { status = PoseStatus.OFF }
    }

    @Test fun `etats traduits et calibration signalee`() {
        val poses = FakePoses()
        val motion = PoseMotionInput(poses)
        assertEquals(MotionStatus.OFF, motion.status)
        poses.status = PoseStatus.NO_MODEL; assertEquals(MotionStatus.NO_MODEL, motion.status)
        poses.status = PoseStatus.ERROR; assertEquals(MotionStatus.ERROR, motion.status)
        poses.status = PoseStatus.LOADING; assertEquals(MotionStatus.LOADING, motion.status)
        motion.start()
        assertEquals(MotionStatus.CALIBRATING, motion.status)
    }

    @Test fun `pilotage de bout en bout`() {
        val poses = FakePoses()
        val motion = PoseMotionInput(poses)
        motion.start()
        for (i in 0 until 10) { poses.now = i / 30.0; poses.frame = pose(timeS = poses.now); motion.read() }
        assertEquals(MotionStatus.RUNNING, motion.status)
        var input = GameInput.IDLE
        for (i in 10 until 70) { poses.now = i / 30.0; poses.frame = pose(shoulderTiltDeg = -18f, timeS = poses.now); input = motion.read() }
        assertTrue(input.moveX < -0.5f)
    }

    @Test fun `revenir en mode camera recalibre`() {
        val poses = FakePoses()
        val motion = PoseMotionInput(poses)
        motion.start()
        for (i in 0 until 10) { poses.now = i / 30.0; poses.frame = pose(timeS = poses.now); motion.read() }
        motion.stop(); motion.start()
        assertEquals(MotionStatus.CALIBRATING, motion.status)
        assertEquals(2, poses.started)
    }
}
