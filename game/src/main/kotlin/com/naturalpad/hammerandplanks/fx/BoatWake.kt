package com.naturalpad.hammerandplanks.fx

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Écume autour du bateau, comme dans l'ancien jeu : un sillage qui part de la poupe et reste sur l'eau
 * (il dérive avec la mer), et des éclaboussures qui naissent autour de la proue et suivent le bateau.
 * Les deux sont dessinées sous le sprite du bateau, dont la carène immergée est semi-transparente.
 */
class BoatWake(random: Random = Random.Default) {
    private val wake = FoamParticles(capacity = 160, random = random)
    private val splash = FoamParticles(capacity = 64, random = random)
    private var texture: Texture? = null
    private var region: TextureRegion? = null

    /** Charge la texture (thread de rendu). Séparé du constructeur pour garder la logique testable sans GL. */
    fun load() {
        if (texture != null) return
        texture = Texture(Gdx.files.internal(TEXTURE_PATH), true).apply {
            setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear)
        }.also { region = TextureRegion(it) }
    }

    /**
     * Avance de [dt] : bateau centré en ([boatX], [boatY]) avec un sprite de côté [boatSize], cap [headingDeg],
     * à [speedRatio] de sa vitesse de pointe ; la mer défile vers le bas à [seaSpeedPx] pixels par seconde.
     */
    fun update(dt: Float, boatX: Float, boatY: Float, boatSize: Float, headingDeg: Float, speedRatio: Float, seaSpeedPx: Float) {
        val boost = 1f + WAKE_SPEED_BOOST * abs(speedRatio).coerceIn(0f, 1f)
        // Sillage : à la poupe, en repère « monde » (la particule reste sur l'eau et dérive avec elle).
        val (sx, sy) = hullPoint(headingDeg, boatSize, along = -STERN_RADIUS)
        val (dirX, dirY) = direction(headingDeg)
        val backSpeed = WAKE_SPEED * boatSize
        wake.emit(
            dt, WAKE_RATE * boost, boatX + sx, boatY + sy, -dirX * backSpeed, -dirY * backSpeed,
            life = WAKE_LIFE, size = WAKE_SIZE * boatSize, spread = 0.03f * boatSize, coneDeg = WAKE_CONE_DEG,
        )
        wake.update(dt, driftY = -seaSpeedPx)
        // Vague d'étrave : sur les deux flancs de la proue, vers l'extérieur et l'arrière, en repère du bateau
        // (elle le suit). Vue de trois quarts arrière, la flottaison de la proue est cachée par la coque :
        // ce sont ces gerbes latérales qui dépassent.
        for (side in SIDES) {
            val (px, py) = sidePoint(headingDeg, boatSize, along = SPLASH_ALONG, side = side)
            val (ox, oy) = sideDirection(headingDeg, side)
            val speed = SPLASH_SPEED * boatSize
            splash.emit(
                dt, SPLASH_RATE * boost / 2f, px, py, (ox - dirX * 0.5f) * speed, (oy - dirY * 0.5f) * speed,
                life = SPLASH_LIFE, size = SPLASH_SIZE * boatSize, spread = SPLASH_SPREAD * boatSize,
            )
        }
        splash.update(dt)
    }

    fun draw(batch: SpriteBatch, boatX: Float, boatY: Float, alpha: Float) {
        val r = region ?: return
        val previous = batch.packedColor
        wake.forEachAlive { drawParticle(batch, r, it, 0f, 0f, alpha * WAKE_OPACITY) }
        splash.forEachAlive { drawParticle(batch, r, it, boatX, boatY, alpha * SPLASH_OPACITY) }
        batch.packedColor = previous
    }

    private fun drawParticle(batch: SpriteBatch, r: TextureRegion, p: FoamParticles.Particle, ox: Float, oy: Float, alpha: Float) {
        val w = p.currentSize
        val h = w * r.regionHeight / r.regionWidth * FORESHORTENING // plaque à plat sur l'eau, vue de biais
        batch.setColor(1f, 1f, 1f, p.alpha * alpha)
        batch.draw(r, ox + p.x - w / 2f, oy + p.y - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, p.rotationDeg)
    }

    fun clear() {
        wake.clear()
        splash.clear()
    }

    fun dispose() {
        texture?.dispose()
        texture = null
        region = null
    }

    val wakeCount: Int get() = wake.aliveCount
    val splashCount: Int get() = splash.aliveCount

    companion object {
        const val TEXTURE_PATH = "fx/foam.png"

        /** Raccourci vertical de la vue 3/4 (caméra inclinée de 35° : cos 35°). */
        const val FORESHORTENING = 0.82f
        /**
         * Distances au centre de la flottaison, sur l'eau, en fraction du côté du sprite, mesurées sur le rendu
         * de profil (boat_player_34_08 : coque de x = 96 à 415 sur 512).
         */
        const val BOW_RADIUS = 0.30f
        const val STERN_RADIUS = 0.26f
        /**
         * La flottaison est sous le centre du sprite (rendu de profil : bas de la coque émergée à la ligne 331 sur 512).
         * En vue 3/4 la hauteur se projette vers le haut de l'écran : la proue émergée paraît plus haute que l'eau.
         */
        const val WATERLINE_OFFSET_Y = -0.146f

        // Sillage (ancien « Trail » : vie 1,5 s, cône de 8°) et éclaboussures (ancien « Splash Front » : vie 0,5 s).
        const val WAKE_RATE = 24f
        const val WAKE_LIFE = 1.5f
        const val WAKE_SIZE = 0.3f
        const val WAKE_SPEED = 0.12f
        const val WAKE_CONE_DEG = 8f
        const val WAKE_OPACITY = 0.7f
        const val WAKE_SPEED_BOOST = 1.5f
        const val SPLASH_RATE = 24f
        const val SPLASH_LIFE = 0.5f
        const val SPLASH_SIZE = 0.16f
        const val SPLASH_SPREAD = 0.02f
        const val SPLASH_SPEED = 0.25f
        const val SPLASH_OPACITY = 0.85f
        /** Gerbes de la vague d'étrave : un peu en arrière de la pointe, sur les flancs. */
        const val SPLASH_ALONG = 0.2f
        /** Demi-largeur de la coque à la flottaison, en fraction du côté du sprite (vue de dessus : 0,12). */
        const val HALF_BEAM = 0.11f
        private val SIDES = floatArrayOf(-1f, 1f)

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
