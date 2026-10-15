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
class FoamParticles(
    val capacity: Int = 128,
    private val random: Random = Random.Default,
    val style: FoamStyle = FoamStyle(),
) {
    init {
        require(capacity > 0) { "Capacité invalide : $capacity" }
    }

    /** Une particule ; recyclée quand elle meurt (pas d'allocation pendant le jeu). */
    class Particle {
        var x = 0f; var y = 0f
        var vx = 0f; var vy = 0f
        var age = 0f; var life = 1f
        var size = 1f; var rotationDeg = 0f
        /** Plaque retournée au hasard : les traits de la planche ne s'alignent pas d'une plaque à l'autre. */
        var flipX = false; var flipY = false
        var alive = false
    }

    private val particles = Array(capacity) { Particle() }
    private var emitDebt = 0f

    val aliveCount: Int get() = particles.count { it.alive }

    /**
     * Émet des particules au débit [ratePerS] pendant [dt] (le reste fractionnaire est reporté),
     * autour de ([x], [y]) dans un disque de rayon [spread] et le long du segment ±([lineX], [lineY]),
     * avec la vitesse ([vx], [vy]) dispersée de ±[coneDeg] et la rotation [rotationDeg].
     * Si la capacité est atteinte, la plus vieille particule est recyclée.
     */
    fun emit(
        dt: Float, ratePerS: Float, x: Float, y: Float, vx: Float, vy: Float,
        life: Float, size: Float, spread: Float = 0f, coneDeg: Float = 0f,
        lineX: Float = 0f, lineY: Float = 0f, rotationDeg: Float = 0f, radialSpeed: Float = 0f, squashY: Float = 1f,
    ) {
        require(dt >= 0f && ratePerS >= 0f && life > 0f && size > 0f && spread >= 0f) { "Paramètres d'émission invalides" }
        emitDebt += ratePerS * dt
        val count = emitDebt.toInt()
        emitDebt -= count
        burst(count, x, y, vx, vy, life, size, spread, coneDeg, lineX, lineY, rotationDeg, radialSpeed, squashY)
    }

    /**
     * Émet [count] particules d'un coup (rafale). [radialSpeed] : vitesse qui écarte la particule du centre
     * (émetteur en demi-sphère de l'ancien jeu) ; [squashY] : écrasement vertical du disque (vue de biais).
     */
    fun burst(
        count: Int, x: Float, y: Float, vx: Float, vy: Float, life: Float, size: Float,
        spread: Float = 0f, coneDeg: Float = 0f, lineX: Float = 0f, lineY: Float = 0f,
        rotationDeg: Float = 0f, radialSpeed: Float = 0f, squashY: Float = 1f,
    ) {
        require(count >= 0 && life > 0f && size > 0f && spread >= 0f) { "Paramètres d'émission invalides" }
        repeat(count) {
            val p = particles.firstOrNull { !it.alive } ?: particles.maxBy { it.age / it.life }
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val radius = spread * kotlin.math.sqrt(random.nextFloat())
            val cone = Math.toRadians(((random.nextFloat() * 2f - 1f) * coneDeg).toDouble())
            val t = random.nextFloat() * 2f - 1f
            p.x = x + radius * cos(angle) + t * lineX; p.y = y + radius * sin(angle) * squashY + t * lineY
            p.vx = (vx * cos(cone) - vy * sin(cone)).toFloat() + radialSpeed * cos(angle)
            p.vy = (vx * sin(cone) + vy * cos(cone)).toFloat() + radialSpeed * sin(angle) * squashY
            p.age = 0f
            p.life = life * (0.8f + 0.4f * random.nextFloat())
            p.size = size * (0.8f + 0.4f * random.nextFloat())
            p.rotationDeg = rotationDeg
            p.flipX = random.nextBoolean()
            p.flipY = random.nextBoolean()
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

    /** Opacité de [p] selon le style : pleine, puis effacement linéaire jusqu'à la fin de sa vie. */
    fun alphaOf(p: Particle): Float {
        val t = (p.age / p.life).coerceIn(0f, 1f)
        if (t <= style.opaqueUntil) return 1f
        return 1f - (t - style.opaqueUntil) / (1f - style.opaqueUntil)
    }

    /** Taille de [p] selon le style : de sizeStart à sizeEnd (fois sa taille), linéaire ou en ralentissant. */
    fun sizeOf(p: Particle): Float {
        val t = (p.age / p.life).coerceIn(0f, 1f)
        val k = if (style.easeOut) 1f - (1f - t) * (1f - t) * (1f - t) else t
        return p.size * (style.sizeStart + (style.sizeEnd - style.sizeStart) * k)
    }

    /** Rotation de [p] : rotation de départ plus rotation continue. */
    fun rotationOf(p: Particle): Float = p.rotationDeg + style.spinDegPerS * p.age

    inline fun forEachAlive(action: (Particle) -> Unit) {
        for (i in 0 until capacity) { val p = get(i); if (p.alive) action(p) }
    }

    fun get(index: Int): Particle = particles[index]

    fun clear() {
        particles.forEach { it.alive = false }
        emitDebt = 0f
    }

}

/**
 * Évolution d'une particule au cours de sa vie (modules « Color / Size / Rotation over lifetime » de l'ancien jeu).
 */
data class FoamStyle(
    /** Part de la vie pendant laquelle la particule reste pleinement opaque avant de s'effacer. */
    val opaqueUntil: Float = 0.3f,
    /** Taille au début et à la fin de la vie, en fraction de la taille tirée à l'émission. */
    val sizeStart: Float = 1f,
    val sizeEnd: Float = 1f,
    /** Vrai : la taille évolue vite puis ralentit (courbe de l'ancien sillage) ; faux : linéaire. */
    val easeOut: Boolean = false,
    /** Rotation continue, en degrés par seconde. */
    val spinDegPerS: Float = 0f,
) {
    init {
        require(opaqueUntil in 0f..<1f && sizeStart >= 0f && sizeEnd >= 0f) { "Style d'écume invalide" }
    }
}
