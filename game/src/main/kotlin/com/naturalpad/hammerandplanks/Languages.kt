package com.naturalpad.hammerandplanks

/** Choix de la langue du jeu à partir de la langue du système ou du navigateur. */
object Languages {
    val SUPPORTED: List<String> = listOf("en", "fr")
    const val DEFAULT: String = "en"

    /**
     * Renvoie le code de langue supporté correspondant à [tag] (ex. "fr-CA" donne "fr").
     * Retombe sur l'anglais pour une langue non supportée, vide ou mal formée.
     */
    fun resolve(tag: String?): String {
        val primary = tag
            ?.trim()
            ?.replace('_', '-')
            ?.substringBefore('-')
            ?.lowercase()
            .orEmpty()
        return if (primary in SUPPORTED) primary else DEFAULT
    }
}
