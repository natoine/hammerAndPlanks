package com.naturalpad.hammerandplanks.fx

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Particules d'écume, portées des systèmes « Trail » et « Splash Front » du bateau de l'ancien jeu
 * (texture FX_boat_splash_sheet_trim : chaque particule est une plaque de petits traits d'écume).
 * Logique pure : positions en pixels d'écran, pas de dessin ici. Nombre de particules borné ([capacity]).
 */
class FoamParticles(val capacity: Int = 128, private val random: Random = Random.Default) {
    init {
        require(capacity > 0) { "Capacité invalide : $capacity" }
    }

    /** Une particule ; recyclée quand elle meurt (pas d'allocation pendant le jeu). */
    class Particle {
        var x = 0f; var y = 0f
        var vx = 0f; var vy = 0f
        var age = 0f; var life = 1f
        var size = 1f; var rotationDeg = 0f
        var alive = false

        /** Opacité : apparition rapide (10 % de la vie), puis effacement progressif. */
        val alpha: Float
            get() {
                val t = (age / life).coerceIn(0f, 1f)
                val fadeIn = (t / FADE_IN).coerceAtMost(1f)
                return fadeIn * (1f - t)
            }

        /** Taille courante : la plaque s'étale un peu en vieillissant. */
        val currentSize: Float get() = size * (1f + GROWTH * (age / life).coerceIn(0f, 1f))
    }

    private val particles = Array(capacity) { Particle() }
    private var emitDebt = 0f

    val aliveCount: Int get() = particles.count { it.alive }

    /**
     * Émet des particules au débit [ratePerS] pendant [dt] (le reste fractionnaire est reporté),
     * autour de ([x], [y]) dans un disque de rayon [spread], avec la vitesse ([vx], [vy]) dispersée de
     * ±[coneDeg]. Si la capacité est atteinte, la plus vieille particule est recyclée.
     */
    fun emit(
        dt: Float, ratePerS: Float, x: Float, y: Float, vx: Float, vy: Float,
        life: Float, size: Float, spread: Float = 0f, coneDeg: Float = 0f,
    ) {
        require(dt >= 0f && ratePerS >= 0f && life > 0f && size > 0f && spread >= 0f) { "Paramètres d'émission invalides" }
        emitDebt += ratePerS * dt
        while (emitDebt >= 1f) {
            emitDebt -= 1f
            val p = particles.firstOrNull { !it.alive } ?: particles.maxBy { it.age / it.life }
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val radius = spread * kotlin.math.sqrt(random.nextFloat())
            val cone = Math.toRadians(((random.nextFloat() * 2f - 1f) * coneDeg).toDouble())
            p.x = x + radius * cos(angle); p.y = y + radius * sin(angle)
            p.vx = (vx * cos(cone) - vy * sin(cone)).toFloat()
            p.vy = (vx * sin(cone) + vy * cos(cone)).toFloat()
            p.age = 0f
            p.life = life * (0.8f + 0.4f * random.nextFloat())
            p.size = size * (0.8f + 0.4f * random.nextFloat())
            p.rotationDeg = (random.nextFloat() * 2f - 1f) * MAX_ROTATION_DEG
            p.alive = true
        }
    }

    /** Fait vieillir et bouger les particules ; ([driftX], [driftY]) s'ajoute à leur vitesse (courant, défilement). */
    fun update(dt: Float, driftX: Float = 0f, driftY: Float = 0f) {
        for (p in particles) {
            if (!p.alive) continue
            p.age += dt
            if (p.age >= p.life) { p.alive = false; continue }
            p.x += (p.vx + driftX) * dt
            p.y += (p.vy + driftY) * dt
        }
    }

    inline fun forEachAlive(action: (Particle) -> Unit) {
        for (i in 0 until capacity) { val p = get(i); if (p.alive) action(p) }
    }

    fun get(index: Int): Particle = particles[index]

    fun clear() {
        particles.forEach { it.alive = false }
        emitDebt = 0f
    }

    companion object {
        const val FADE_IN = 0.1f
        const val GROWTH = 0.6f
        /** Les traits d'écume de la texture sont horizontaux : légère rotation aléatoire seulement. */
        const val MAX_ROTATION_DEG = 20f
    }
}
