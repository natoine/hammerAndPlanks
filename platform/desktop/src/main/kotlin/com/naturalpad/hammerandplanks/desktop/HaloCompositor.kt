package com.naturalpad.hammerandplanks.desktop

import com.naturalpad.hammerandplanks.pose.HaloShape
import com.naturalpad.hammerandplanks.pose.HaloStroke
import com.naturalpad.hammerandplanks.pose.PlayerHalo
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

/**
 * Halo autour du joueur dans l'image de la webcam (desktop) : capsules élargies et floutées (plusieurs passes),
 * intérieur effacé, puis mélange de la couleur dorée sur une copie de l'image.
 */
object HaloCompositor {
    /** Passes du flou : plus il y en a, plus le bord extérieur est doux. */
    const val BLUR_PASSES = 6

    /** Renvoie une copie de [rgb] (RGB 24 bits, [width]×[height]) avec le halo de [shape]. [rgb] n'est pas modifié. */
    fun compose(rgb: ByteArray, width: Int, height: Int, shape: HaloShape): ByteArray {
        require(rgb.size >= width * height * 3) { "Image trop petite" }
        val layer = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = layer.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            // Bord flou : passes de la plus large (transparente) à la plus étroite, qui s'accumulent.
            for (pass in 0 until BLUR_PASSES) {
                val extra = shape.haloWidth * (1f - pass.toFloat() / BLUR_PASSES)
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f / BLUR_PASSES + 0.1f)
                g.color = Color.WHITE
                shape.strokes.forEach { draw(g, it, it.radius + extra) }
            }
            // Intérieur effacé : il ne reste qu'un liseré autour de la personne.
            g.composite = AlphaComposite.Clear
            shape.strokes.forEach { draw(g, it, it.radius) }
        } finally {
            g.dispose()
        }
        val out = rgb.copyOf()
        val gold = floatArrayOf(PlayerHalo.COLOR_R * 255f, PlayerHalo.COLOR_G * 255f, PlayerHalo.COLOR_B * 255f)
        val pixels = IntArray(width)
        for (y in 0 until height) {
            layer.getRGB(0, y, width, 1, pixels, 0, width)
            for (x in 0 until width) {
                val a = (pixels[x] ushr 24) / 255f * PlayerHalo.OPACITY
                if (a <= 0f) continue
                val i = (y * width + x) * 3
                for (c in 0 until 3) {
                    val v = (out[i + c].toInt() and 0xFF).toFloat()
                    out[i + c] = (v + (gold[c] - v) * a).toInt().coerceIn(0, 255).toByte()
                }
            }
        }
        return out
    }

    private fun draw(g: java.awt.Graphics2D, s: HaloStroke, radius: Float) {
        if (s.x1 == s.x2 && s.y1 == s.y2) {
            g.fill(Ellipse2D.Float(s.x1 - radius, s.y1 - radius, 2 * radius, 2 * radius))
        } else {
            g.stroke = BasicStroke(2 * radius, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
            g.draw(Line2D.Float(s.x1, s.y1, s.x2, s.y2))
        }
    }
}
