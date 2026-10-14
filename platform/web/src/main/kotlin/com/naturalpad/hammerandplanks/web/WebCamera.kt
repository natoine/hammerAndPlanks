package com.naturalpad.hammerandplanks.web

import com.naturalpad.hammerandplanks.input.StickPosition
import com.naturalpad.hammerandplanks.platform.CameraStatus
import com.naturalpad.hammerandplanks.platform.CameraThumbnail
import com.naturalpad.hammerandplanks.platform.GamepadDetector

/** Caméra du navigateur : élément vidéo posé par-dessus le canvas (voir webapp/index.html, window.hapCamera). */
class WebCamera : CameraThumbnail {
    override val status: CameraStatus
        get() = CameraStatus.entries.firstOrNull { it.name == cameraStatus() } ?: CameraStatus.UNAVAILABLE

    override fun start() = cameraStart()

    override fun stop() = cameraStop()

    override fun place(left: Float, top: Float, width: Float, height: Float) = cameraPlace(left, top, width, height)
}

/**
 * Manettes via l'API Gamepad. Les navigateurs ne signalent une manette qu'après un premier appui
 * sur l'un de ses boutons (protection de la vie privée).
 */
class WebGamepads : GamepadDetector {
    override fun isConnected(): Boolean = gamepadConnected()

    override fun leftStick(): StickPosition = StickPosition(gamepadStickX(), gamepadStickY())
}
