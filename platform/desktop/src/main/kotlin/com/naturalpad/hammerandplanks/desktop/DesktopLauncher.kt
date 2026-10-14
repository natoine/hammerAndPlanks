package com.naturalpad.hammerandplanks.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.naturalpad.hammerandplanks.HammerAndPlanksGame
import com.naturalpad.hammerandplanks.PlatformHooks
import com.naturalpad.hammerandplanks.gestures.MotionSteering
import com.naturalpad.hammerandplanks.gestures.PoseMotionInput
import com.naturalpad.hammerandplanks.gestures.TiltSource
import java.io.File
import java.util.Locale

/**
 * Lanceur de développement (LWJGL3). Fenêtre au format portrait d'un smartphone, redimensionnable.
 * Options : --lang=fr|en (force la langue), --size=LARGEURxHAUTEUR, --screenshot=fichier.png (capture puis quitte),
 * --input-mode=keyboard|gamepad|motion (impose le mode d'interaction au lancement),
 * --control=position|velocity|force et --speed=0..1 (contrôle du bateau),
 * --model=chemin/instanthmr.onnx et --tilt=shoulders|head (mode caméra).
 */
fun main(args: Array<String>) {
    val options = DesktopOptions.parse(args)
    val forcedLanguage = options.language
    val webcam = DesktopCamera()
    val tilt = TiltSource.entries.firstOrNull { it.name == options.tiltSource } ?: TiltSource.SHOULDERS
    // Chaîne du mode caméra : webcam → InstantHMR → inclinaison → GameInput.
    val modelFile = File(options.modelPath ?: "../models/instanthmr.onnx")
    val poseSource = DesktopPoseSource(webcam, modelFile)
    val motionInput = PoseMotionInput(poseSource, MotionSteering(source = tilt))
    if (options.debugPose) startPoseDebugLog(motionInput, poseSource)
    val hooks = object : PlatformHooks {
        override val preferredLanguageTag: String = forcedLanguage ?: Locale.getDefault().toLanguageTag()
        override val gamepads = DesktopGamepads()
        override val camera = webcam
        override val motion = motionInput
        override val tiltSource = tilt.name.lowercase()
        override val forcedInputMode: String? = options.inputMode
        override val forcedControlMode: String? = options.controlMode
        override val forcedSpeedFactor: String? = options.speedFactor
    }
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Hammer and Planks")
        setWindowedMode(options.width, options.height)
        setResizable(true)
        useVsync(true)
        setForegroundFPS(60)
    }
    val game = HammerAndPlanksGame(hooks)
    val listener = options.screenshotPath?.let { ScreenshotListener(game, it, delaySeconds = options.screenshotDelayS) } ?: game
    Lwjgl3Application(listener, config)
}

/** Outil de réglage (--debug-pose) : une ligne par seconde avec l'état, la posture neutre et l'inclinaison. */
private fun startPoseDebugLog(motion: PoseMotionInput, poses: DesktopPoseSource) {
    kotlin.concurrent.thread(name = "pose-debug", isDaemon = true) {
        while (true) {
            Thread.sleep(1000)
            val frame = poses.latest()
            val raw = frame?.let { com.naturalpad.hammerandplanks.gestures.BodyTilt.lateralTiltDeg(it, motion.steering.source) }
            println(
                "[PoseDebug] état=${motion.status} neutre=${motion.steering.neutralDeg?.let { "%.1f°".format(it) } ?: "-"} " +
                    "brut=${raw?.let { "%.1f°".format(it) } ?: "-"} filtré=${"%.1f°".format(motion.steering.tiltDeg)} " +
                    "axe=${"%.2f".format(motion.steering.axis(motion.steering.tiltDeg))}" +
                    (frame?.let { f ->
                        " épaules px=(%.0f,%.0f)-(%.0f,%.0f) nez px=(%.0f,%.0f)".format(
                            f.joints2d[2 * 6], f.joints2d[2 * 6 + 1], f.joints2d[2 * 5], f.joints2d[2 * 5 + 1],
                            f.joints2d[0], f.joints2d[1],
                        )
                    } ?: ""),
            )
        }
    }
}
