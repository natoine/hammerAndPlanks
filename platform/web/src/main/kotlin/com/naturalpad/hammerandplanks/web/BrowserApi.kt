package com.naturalpad.hammerandplanks.web

import org.teavm.jso.JSBody

// Appels JavaScript du lanceur web. Fonctions de premier niveau : TeaVM les compile en fonctions statiques.

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
@JSBody(params = ["text"], script = "var e = document.getElementById('input-mode'); if (e) { e.textContent = text; }")
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

/** Position du bateau exposée sur le canvas (data-boat-offset) pour les tests d'interface. */
@JSBody(params = ["offset"], script = "var c = document.getElementById('canvas'); if (c) { c.dataset.boatOffset = offset.toFixed(3); }")
external fun reportBoatOffset(offset: Float)

/** Paramètre de l'URL de la page (ex. ?control=velocity&speed=0.5), ou null. */
@JSBody(params = ["name"], script = "return new URLSearchParams(window.location.search).get(name);")
external fun urlParameter(name: String): String?
