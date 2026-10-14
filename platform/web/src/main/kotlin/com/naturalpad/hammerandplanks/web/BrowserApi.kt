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
