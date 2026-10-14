package com.naturalpad.hammerandplanks.launch

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LaunchLayoutTest {
    private val screens = listOf(
        360 to 640,   // petit smartphone portrait
        1080 to 2400, // smartphone récent portrait
        2400 to 1080, // smartphone paysage
        1920 to 1080, // desktop
        768 to 1024,  // tablette
    )

    @Test fun `le bateau reste entierement visible`() {
        for ((w, h) in screens) {
            val l = LaunchLayout.compute(w, h)
            val half = l.boatSize / 2f
            assertTrue(l.boatCenterX - half >= 0f && l.boatCenterX + half <= w, "Bateau hors largeur en ${w}x$h")
            assertTrue(l.boatCenterY - half >= 0f && l.boatCenterY + half <= h, "Bateau hors hauteur en ${w}x$h")
        }
    }

    @Test fun `le titre est au dessus de l accroche et sous le haut de l ecran`() {
        for ((w, h) in screens) {
            val l = LaunchLayout.compute(w, h)
            assertTrue(l.titleBaselineY < h, "Titre hors écran en ${w}x$h")
            assertTrue(l.taglineBaselineY < l.titleBaselineY, "Accroche au-dessus du titre en ${w}x$h")
        }
    }

    @Test fun `l accroche ne chevauche pas le haut du bateau`() {
        for ((w, h) in screens) {
            val l = LaunchLayout.compute(w, h)
            // Le sprite a des marges transparentes : le haut du bateau visible est vers 80 % de la demi-taille.
            val boatTop = l.boatCenterY + l.boatSize / 2f * 0.8f
            val taglineBottom = l.taglineBaselineY - l.taglineFontPx * 2.4f // deux lignes possibles
            assertTrue(taglineBottom >= boatTop, "Accroche sur le bateau en ${w}x$h")
        }
    }

    @Test fun `le texte est plus grand sur un grand ecran`() {
        val small = LaunchLayout.compute(360, 640)
        val big = LaunchLayout.compute(1080, 2400)
        assertTrue(big.titleFontPx > small.titleFontPx)
        assertTrue(big.boatSize > small.boatSize)
    }

    @Test fun `polices jamais sous le minimum lisible sur un ecran minuscule`() {
        val l = LaunchLayout.compute(50, 50)
        assertTrue(l.titleFontPx >= LaunchLayout.MIN_FONT_PX)
        assertTrue(l.taglineFontPx >= LaunchLayout.MIN_FONT_PX)
        assertTrue(l.versionFontPx >= LaunchLayout.MIN_FONT_PX)
    }

    @Test fun `la zone de texte respecte les marges`() {
        val l = LaunchLayout.compute(360, 640)
        assertTrue(l.margin > 0f && l.textWidth + 2 * l.margin <= 360f + 0.001f)
    }

    @Test fun `l accroche descend quand le titre passe sur deux lignes`() {
        val oneLine = LaunchLayout.taglineBaselineBelow(900f, 50f, 20)
        val twoLines = LaunchLayout.taglineBaselineBelow(900f, 110f, 20)
        assertTrue(twoLines < oneLine)
        assertTrue(oneLine < 900f - 50f, "L'accroche doit être sous le bloc de titre")
    }

    @Test fun `hauteur de titre negative refusee`() {
        assertFailsWith<IllegalArgumentException> { LaunchLayout.taglineBaselineBelow(900f, -1f, 20) }
    }

    @Test fun `dimensions nulles ou negatives refusees`() {
        assertFailsWith<IllegalArgumentException> { LaunchLayout.compute(0, 640) }
        assertFailsWith<IllegalArgumentException> { LaunchLayout.compute(360, -1) }
    }
}
