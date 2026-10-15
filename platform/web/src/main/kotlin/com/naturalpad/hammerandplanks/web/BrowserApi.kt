package com.naturalpad.hammerandplanks.web

import org.teavm.jso.JSBody

// Appels JavaScript du lanceur web. Fonctions de premier niveau : TeaVM les compile en fonctions statiques.
// Pas de variable locale dans les scripts : TeaVM renomme les paramètres en lettres simples (c, e...)
// à la minification, et une variable locale du même nom les masquerait.

@JSBody(script = "return (navigator.languages && navigator.languages[0]) || navigator.language || null;")
external fun browserLanguage(): String?

/** Le canvas est opaque pour un lecteur d'écran : on lui donne un libellé et on règle la langue de la page. */
@JSBody(
    params = ["language", "description"],
    script = "document.documentElement.setAttribute('lang', language);" +
        "document.getElementById('canvas').setAttribute('role', 'img');" +
        "document.getElementById('canvas').setAttribute('aria-label', description);",
)
external fun describePage(language: String, description: String)

/** Annonce le mode d'interaction dans une zone aria-live (lue par les lecteurs d'écran). */
@JSBody(params = ["text"], script = "if (document.getElementById('input-mode')) { document.getElementById('input-mode').textContent = text; }")
external fun announce(text: String)

@JSBody(script = "return window.hapGamepads ? window.hapGamepads() : false;")
external fun gamepadConnected(): Boolean

@JSBody(script = "return window.hapCamera ? window.hapCamera.status() : 'UNAVAILABLE';")
external fun cameraStatus(): String

@JSBody(script = "if (window.hapCamera) { window.hapCamera.start(); }")
external fun cameraStart()

@JSBody(script = "if (window.hapCamera) { window.hapCamera.stop(); }")
external fun cameraStop()

@JSBody(
    params = ["left", "top", "width", "height"],
    script = "if (window.hapCamera) { window.hapCamera.place(left, top, width, height); }",
)
external fun cameraPlace(left: Float, top: Float, width: Float, height: Float)

/** Position et cap du bateau exposés sur le canvas (data-boat-offset, data-boat-heading) pour les tests d'interface. */
@JSBody(
    params = ["offset", "heading"],
    script = "if (document.getElementById('canvas')) { document.getElementById('canvas').dataset.boatOffset = offset.toFixed(3);" +
        " document.getElementById('canvas').dataset.boatHeading = heading.toFixed(1); }",
)
external fun reportBoatOffset(offset: Float, heading: Float)

/** Paramètre de l'URL de la page (ex. ?control=velocity&speed=0.5), ou null. */
@JSBody(params = ["name"], script = "return new URLSearchParams(window.location.search).get(name);")
external fun urlParameter(name: String): String?

@JSBody(script = "return window.hapGamepadStick ? window.hapGamepadStick()[0] : 0;")
external fun gamepadStickX(): Float

@JSBody(script = "return window.hapGamepadStick ? window.hapGamepadStick()[1] : 0;")
external fun gamepadStickY(): Float

// Estimation de pose du navigateur (webapp/pose.js, window.hapPose).
@JSBody(script = "return window.hapPose ? window.hapPose.status() : 'NO_MODEL';")
external fun poseStatus(): String

@JSBody(script = "if (window.hapPose) { window.hapPose.start(); }")
external fun poseStart()

@JSBody(script = "if (window.hapPose) { window.hapPose.stop(); }")
external fun poseStop()

@JSBody(script = "return window.hapPose ? window.hapPose.time() : -1;")
external fun poseTime(): Double

@JSBody(script = "return window.hapPose ? window.hapPose.now() : performance.now() / 1000;")
external fun poseNow(): Double

@JSBody(params = ["i"], script = "return window.hapPose.joint3d(i);")
external fun poseJoint3d(i: Int): Float

@JSBody(params = ["i"], script = "return window.hapPose.joint2d(i);")
external fun poseJoint2d(i: Int): Float

@JSBody(script = "return window.hapPose ? window.hapPose.width() : 0;")
external fun poseWidth(): Int

@JSBody(script = "return window.hapPose ? window.hapPose.height() : 0;")
external fun poseHeight(): Int

/** État du pilotage par le corps exposé sur le canvas (data-motion-status) et annoncé (zone aria-live). */
@JSBody(
    params = ["status", "text"],
    script = "if (document.getElementById('canvas')) { document.getElementById('canvas').dataset.motionStatus = status; }" +
        "if (document.getElementById('motion-status')) { document.getElementById('motion-status').textContent = text; }",
)
external fun reportMotionStatus(status: String, text: String)

/** Dessine le halo du joueur sur l'imagette (forme encodée par PlayerHalo.encode). */
@JSBody(params = ["encoded"], script = "if (window.hapHalo) { window.hapHalo.draw(encoded); }")
external fun haloDraw(encoded: String)

@JSBody(script = "if (window.hapHalo) { window.hapHalo.clear(); }")
external fun haloClear()
