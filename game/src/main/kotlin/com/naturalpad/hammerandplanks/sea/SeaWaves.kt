package com.naturalpad.hammerandplanks.sea

import kotlin.math.floor

/**
 * Réglages et calculs côté CPU de la mer animée (shader assets/shaders/sea.frag).
 * Les valeurs par défaut reprennent le matériau Sea de l'ancien jeu Unity (Assets/Materials/Sea.mat)
 * et les scripts qui l'animaient (PhaseShiftTexture : vitesse 0,2).
 */
data class SeaWaves(
    /** Vitesse d'évolution des vagues sur place, en unités de bruit par seconde (ancien jeu : 0,2). */
    val phaseSpeed: Float = 0.2f,
    /** Fréquences du bruit par petit côté de l'écran. Plus serré en y : crêtes de vagues allongées horizontalement. */
    val noiseScaleX: Float = 1.6f,
    val noiseScaleY: Float = 6.5f,
    /** Octaves de turbulence : 5 dans l'ancien jeu, 4 ici pour ménager les téléphones (le 5e est un détail fin). */
    val octaves: Int = 4,
    /** Lacunarité et gain de la turbulence (ancien _PerlinParams : 2 ; 0,5). */
    val lacunarity: Float = 2f,
    val gain: Float = 0.5f,
    /** Amplitude du décalage de texture par la hauteur de vague, en fraction du petit côté de l'écran. */
    val parallaxFraction: Float = 0.03f,
    /** Bande d'écume : position, largeur, lissage (ancien _HeightBandPos, _HeightBandWidth, _HeightSmoothBand). */
    val foamBandPos: Float = 1f,
    val foamBandWidth: Float = 0.02f,
    val foamSmoothBand: Float = 0.02f,
    /**
     * Couleur ajoutée par l'écume des crêtes. L'ancien _EcumeColor (0,358 ; 0,838 ; 1) était atténué par la
     * transparence et l'éclairage de l'ancienne mer : valeur calée sur la capture de l'ancien jeu (crête moins mer
     * autour : +50, +67, +68 sur 255). Intensité : ancien réglage cosmétique ecumeAlpha (1 par défaut).
     */
    val foamR: Float = 0.20f,
    val foamG: Float = 0.26f,
    val foamB: Float = 0.27f,
    val foamIntensity: Float = 1f,
    /**
     * Étalonnage de la couleur de la texture (avant l'écume) : couleur = texture × gradeScale + gradeOffset.
     * Valeurs calées sur une capture de l'ancien jeu (2014-12-03) : même moyenne et même contraste par canal que
     * sa mer, plus sombre et d'un bleu acier (l'ancienne mer était semi-transparente sur un fond sombre, et éclairée).
     */
    val gradeScaleR: Float = 1.08f,
    val gradeScaleG: Float = 0.61f,
    val gradeScaleB: Float = 0.66f,
    val gradeOffsetR: Float = -0.080f,
    val gradeOffsetG: Float = 0.078f,
    val gradeOffsetB: Float = 0.104f,
) {
    init {
        require(octaves in 1..MAX_OCTAVES) { "octaves doit être entre 1 et $MAX_OCTAVES : $octaves" }
        require(noiseScaleX > 0f && noiseScaleY > 0f) { "Les fréquences du bruit doivent être positives" }
        require(lacunarity > 0f && gain > 0f) { "Lacunarité et gain doivent être positifs" }
        require(parallaxFraction >= 0f) { "La parallaxe ne peut pas être négative" }
        require(foamIntensity in 0f..1f) { "L'intensité de l'écume doit être entre 0 et 1" }
    }

    /** Phase des vagues à l'instant [timeS], ramenée dans [0, 289[ (période du bruit) pour garder la précision. */
    fun phase(timeS: Float): Float = wrapNoise(timeS * phaseSpeed)

    /**
     * Décalage vertical du bruit pour qu'il défile avec la texture : [scrolledPx] pixels de défilement,
     * [unitPx] étant le petit côté de l'écran. Ramené dans [0, 289[.
     */
    fun noiseOffset(scrolledPx: Float, unitPx: Float): Float {
        require(unitPx > 0f) { "Unité de bruit invalide : $unitPx" }
        return wrapNoise(scrolledPx / unitPx * noiseScaleY)
    }

    /** Amplitude de la parallaxe en pixels, [unitPx] étant le petit côté de l'écran. */
    fun parallaxPx(unitPx: Float): Float = parallaxFraction * unitPx

    /**
     * Poids de l'écume pour une hauteur de vague [h], identique à la formule du shader.
     * Sert de référence testable : l'écume apparaît quand h approche [foamBandPos].
     */
    fun foamWeight(h: Float): Float {
        val a = smoothstep(foamBandPos - foamBandWidth - foamSmoothBand, foamBandPos - foamBandWidth, h)
        val b = smoothstep(-foamBandPos - foamBandWidth - foamSmoothBand, -foamBandPos - foamBandWidth, -h)
        return a * b
    }

    companion object {
        const val MAX_OCTAVES = 5
        /** Période du bruit simplex du shader (mod289). */
        const val NOISE_PERIOD = 289f

        fun wrapNoise(value: Float): Float = value - floor(value / NOISE_PERIOD) * NOISE_PERIOD

        private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
            val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
    }
}
