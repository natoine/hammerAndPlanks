package com.naturalpad.hammerandplanks.fx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Écume autour du bateau, portée des systèmes « Trail » et « Splash Front » de l'ancien jeu et du script TrailAnim
 * qui pilotait leurs débits. Les quantités sont celles de l'ancien jeu : c'est un nuage d'écume dense et lumineux
 * qui entoure la coque et masque son contact avec l'eau.
 *
 * - Sillage : naît sous le milieu du bateau, sur toute sa largeur (ancien cône de rayon 0,45), reste sur l'eau et
 *   dérive avec elle (TrailAnim : vitesse de départ = vitesse de défilement). Débit max(100 × vitesse, 10) par seconde.
 * - Gerbes : naissent dans une demi-sphère de rayon 0,5 autour de l'avant de la coque et s'en écartent (vitesse 0,5),
 *   suivent le bateau. Débit max(400 × vitesse, 20) par seconde, plus une rafale de 50 particules à la fréquence
 *   1 + 5 × vitesse (par seconde).
 * La « vitesse » est celle de l'ancien moteur, en unités par seconde (bateau de 2,5 de long), défilement compris.
 */
class BoatWake(random: Random = Random.Default) {
    private val wake = FoamParticles(capacity = 384, random = random, style = WAKE_STYLE)
    private val splash = FoamParticles(capacity = 640, random = random, style = SPLASH_STYLE)
    private var texture: Texture? = null
    private var region: TextureRegion? = null
    private var sinceBurst = 0f

    /** Charge la texture (thread de rendu). Séparé du constructeur pour garder la logique testable sans GL. */
    fun load() {
        if (texture != null) return
        // Sans mipmaps : réduits, les petits traits d'écume restent un grain pétillant (comme l'ancien jeu)
        // au lieu de se fondre en brouillard.
        texture = Texture(Gdx.files.internal(TEXTURE_PATH), false).apply {
            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        }.also { region = TextureRegion(it, 0, PLATE_TOP, it.width, PLATE_HEIGHT) } // la planche, sans la marge
    }

    /** Vitesse du bateau dans les unités de l'ancien jeu, à partir de sa vitesse à l'écran (pixels par seconde). */
    fun oldGameSpeed(lateralSpeedPx: Float, seaSpeedPx: Float, boatSize: Float): Float {
        val boatLengthPx = BOAT_LENGTH * boatSize
        return kotlin.math.hypot(lateralSpeedPx, seaSpeedPx) / boatLengthPx * OLD_BOAT_LENGTH
    }

    /**
     * Avance de [dt] : bateau centré en ([boatX], [boatY]) avec un sprite de côté [boatSize], cap [headingDeg],
     * vitesse latérale [lateralSpeedPx] ; la mer défile vers le bas à [seaSpeedPx] pixels par seconde.
     */
    fun update(
        dt: Float, boatX: Float, boatY: Float, boatSize: Float, headingDeg: Float, lateralSpeedPx: Float, seaSpeedPx: Float,
    ) {
        val speed = oldGameSpeed(lateralSpeedPx, seaSpeedPx, boatSize)
        val unit = BOAT_LENGTH * boatSize / OLD_BOAT_LENGTH // pixels par unité de l'ancien jeu

        // Sillage : sous le milieu du bateau, sur toute sa largeur, en repère « monde » (reste sur l'eau).
        val (cx, cy) = hullPoint(headingDeg, boatSize, along = TRAIL_ALONG)
        val (beamX, beamY) = sideDirection(headingDeg, 1f)
        wake.emit(
            dt, maxOf(TRAIL_RATE_FACTOR * speed, TRAIL_MIN_RATE), boatX + cx, boatY + cy, 0f, 0f,
            life = WAKE_LIFE, size = WAKE_SIZE * unit,
            lineX = beamX * TRAIL_RADIUS * unit, lineY = beamY * TRAIL_RADIUS * unit,
        )
        wake.update(dt, driftY = -seaSpeedPx)

        // Gerbes : demi-sphère autour de l'avant de la coque, en repère du bateau (elles le suivent).
        val (sx, sy) = hullPoint(headingDeg, boatSize, along = SPLASH_ALONG)
        fun emitSplash(count: Int) = splash.burst(
            count, sx, sy, 0f, 0f, life = SPLASH_LIFE, size = SPLASH_SIZE * unit,
            spread = SPLASH_RADIUS * unit, rotationDeg = SPLASH_START_ROTATION_DEG,
            radialSpeed = SPLASH_SPEED * unit, squashY = FORESHORTENING,
        )
        splash.emit(
            dt, maxOf(SPLASH_RATE_FACTOR * speed, SPLASH_MIN_RATE), sx, sy, 0f, 0f,
            life = SPLASH_LIFE, size = SPLASH_SIZE * unit, spread = SPLASH_RADIUS * unit,
            rotationDeg = SPLASH_START_ROTATION_DEG, radialSpeed = SPLASH_SPEED * unit, squashY = FORESHORTENING,
        )
        sinceBurst += dt
        if (sinceBurst > 1f / (BURST_MIN_FREQ + BURST_FREQ_FACTOR * speed)) {
            sinceBurst = 0f
            emitSplash(BURST_COUNT)
        }
        splash.update(dt)
    }

    fun draw(batch: SpriteBatch, boatX: Float, boatY: Float, alpha: Float) {
        val r = region ?: return
        val previous = batch.packedColor
        // Mélange additif, comme le shader de particules de l'ancien jeu : l'écume éclaircit l'eau.
        val srcFunc = batch.blendSrcFunc
        val dstFunc = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        wake.forEachAlive { drawParticle(batch, r, wake, it, 0f, 0f, alpha * WAKE_OPACITY) }
        splash.forEachAlive { drawParticle(batch, r, splash, it, boatX, boatY, alpha * SPLASH_OPACITY) }
        batch.setBlendFunction(srcFunc, dstFunc)
        batch.packedColor = previous
    }

    private fun drawParticle(
        batch: SpriteBatch, r: TextureRegion, system: FoamParticles, p: FoamParticles.Particle, ox: Float, oy: Float, alpha: Float,
    ) {
        // Comme l'ancien jeu : quad carré (la planche y est étirée) en panneau vertical tourné vers la caméra,
        // donc écrasé en hauteur par la plongée de la caméra.
        val w = system.sizeOf(p)
        val h = w * BILLBOARD_SQUASH
        batch.setColor(TINT_R, TINT_G, TINT_B, system.alphaOf(p) * alpha)
        batch.draw(r, ox + p.x - w / 2f, oy + p.y - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, system.rotationOf(p))
    }

    fun clear() {
        wake.clear()
        splash.clear()
        sinceBurst = 0f
    }

    fun dispose() {
        texture?.dispose()
        texture = null
        region = null
    }

    val wakeCount: Int get() = wake.aliveCount
    val splashCount: Int get() = splash.aliveCount

    /** Rotations des particules du sillage (tests). */
    internal fun wakeRotations(): List<Float> = buildList { wake.forEachAlive { add(wake.rotationOf(it)) } }

    companion object {
        const val TEXTURE_PATH = "fx/foam.png"

        /** Raccourci vertical de la vue 3/4 (caméra inclinée de 35° : cos 35°). */
        const val FORESHORTENING = 0.82f
        /**
         * Hauteur à l'écran d'un panneau vertical de côté 1 (mode « Vertical Billboard » de l'ancien jeu), vu par
         * une caméra plongeant de 58° sous l'horizontale (rotation x de la caméra de jeu : cos 57,8°).
         */
        const val BILLBOARD_SQUASH = 0.53f
        /** Planche d'écume dans assets/fx/foam.png (512×256) : lignes 21 à 235. */
        const val PLATE_TOP = 21
        const val PLATE_HEIGHT = 214

        /** Longueur du bateau dans l'ancien jeu (unités Unity) et dans le sprite (fraction de son côté). */
        const val OLD_BOAT_LENGTH = 2.5f
        const val BOAT_LENGTH = 0.6f

        /** Teinte de l'ancien matériau Ecume (_TintColor 0,36 ; 0,84 ; 1), doublée comme dans les shaders de particules Unity. */
        const val TINT_R = 0.71f
        const val TINT_G = 1f
        const val TINT_B = 1f

        /** Sillage : opaque jusqu'à 29 % de sa vie, grandit de 46 % à 100 % en ralentissant, sans rotation. */
        val WAKE_STYLE = FoamStyle(opaqueUntil = 0.29f, sizeStart = 0.46f, sizeEnd = 1f, easeOut = true)
        /** Gerbes : opaques jusqu'à 34 % de leur vie, rétrécissent jusqu'à disparaître, tournent de 45° par seconde. */
        val SPLASH_STYLE = FoamStyle(opaqueUntil = 0.34f, sizeStart = 1f, sizeEnd = 0f, spinDegPerS = 45f)

        // Ancien « Trail » (unités de l'ancien jeu) : vie 1,5 s, taille 0,8, cône de rayon 0,45, émetteur à z = 0,176.
        const val WAKE_LIFE = 1.5f
        const val WAKE_SIZE = 0.8f
        const val TRAIL_RADIUS = 0.45f
        const val TRAIL_ALONG = 0.176f / OLD_BOAT_LENGTH * BOAT_LENGTH
        /** Opacité calée sur la capture de l'ancien jeu (le sillage y éclaircit la mer sans la saturer). */
        const val WAKE_OPACITY = 0.12f
        // Ancien TrailAnim : débits selon la vitesse.
        const val TRAIL_RATE_FACTOR = 100f
        const val TRAIL_MIN_RATE = 10f
        const val SPLASH_RATE_FACTOR = 400f
        const val SPLASH_MIN_RATE = 20f
        const val BURST_COUNT = 50
        const val BURST_MIN_FREQ = 1f
        const val BURST_FREQ_FACTOR = 5f
        // Ancien « Splash Front » : vie 0,5 s, taille 0,5, vitesse 0,5, demi-sphère de rayon 0,5 à z = 0,712, rotation 20°.
        const val SPLASH_LIFE = 0.5f
        const val SPLASH_SIZE = 0.5f
        const val SPLASH_SPEED = 0.5f
        const val SPLASH_RADIUS = 0.5f
        const val SPLASH_ALONG = 0.712f / OLD_BOAT_LENGTH * BOAT_LENGTH
        const val SPLASH_START_ROTATION_DEG = 20f
        const val SPLASH_OPACITY = 0.12f

        /** Distances de la poupe à la proue, sur l'eau, en fraction du côté du sprite (rendu de profil). */
        const val BOW_RADIUS = 0.30f
        const val STERN_RADIUS = 0.26f
        /** Demi-largeur de la coque à la flottaison, en fraction du côté du sprite (vue de dessus : 0,12). */
        const val HALF_BEAM = 0.11f
        /**
         * La flottaison est sous le centre du sprite (rendu de profil : bas de la coque émergée à la ligne 331 sur 512).
         * En vue 3/4 la hauteur se projette vers le haut de l'écran : la proue émergée paraît plus haute que l'eau.
         */
        const val WATERLINE_OFFSET_Y = -0.146f

        /** Direction de la proue à l'écran pour un cap (0 : vers le haut, positif : vers la gauche), raccourcie par la vue. */
        fun direction(headingDeg: Float): Pair<Float, Float> {
            val h = Math.toRadians(headingDeg.toDouble())
            return (-sin(h)).toFloat() to (cos(h) * FORESHORTENING).toFloat()
        }

        /** Direction perpendiculaire à la proue, vers bâbord (-1) ou tribord (1), à l'écran. */
        fun sideDirection(headingDeg: Float, side: Float): Pair<Float, Float> {
            val h = Math.toRadians(headingDeg.toDouble())
            return (side * cos(h)).toFloat() to (side * sin(h) * FORESHORTENING).toFloat()
        }

        /** Point du flanc [side] à la distance [along] du centre (fractions de [boatSize]), relatif au centre du sprite. */
        fun sidePoint(headingDeg: Float, boatSize: Float, along: Float, side: Float): Pair<Float, Float> {
            val (cx, cy) = hullPoint(headingDeg, boatSize, along)
            val (sx, sy) = sideDirection(headingDeg, side)
            return cx + sx * HALF_BEAM * boatSize to cy + sy * HALF_BEAM * boatSize
        }

        /**
         * Point de la flottaison à la distance [along] du centre (positif vers la proue), en fraction de [boatSize],
         * relatif au centre du sprite, en pixels (y vers le haut).
         */
        fun hullPoint(headingDeg: Float, boatSize: Float, along: Float): Pair<Float, Float> {
            val (dx, dy) = direction(headingDeg)
            return dx * along * boatSize to (WATERLINE_OFFSET_Y + dy * along) * boatSize
        }
    }
}
