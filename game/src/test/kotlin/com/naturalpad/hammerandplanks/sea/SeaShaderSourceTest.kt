package com.naturalpad.hammerandplanks.sea

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Cohérence entre le shader GLSL et le code Kotlin qui le pilote (le GLSL n'est compilé qu'à l'exécution). */
class SeaShaderSourceTest {
    private val assets = File(System.getProperty("assetsDir") ?: "../assets")
    private val fragment = File(assets, SeaRenderer.FRAGMENT_PATH).readText()
    private val vertex = File(assets, SeaRenderer.VERTEX_PATH).readText()

    private fun declaredUniforms(source: String): Set<String> =
        Regex("""^\s*uniform\s+\w+\s+(\w+)\s*;""", RegexOption.MULTILINE).findAll(source).map { it.groupValues[1] }.toSet()

    @Test fun `chaque uniform pilote par Kotlin est declare dans le shader`() {
        val declared = declaredUniforms(fragment)
        for (name in SeaUniforms.ALL) assertTrue(name in declared, "Uniform absent du shader : $name")
    }

    @Test fun `le shader ne declare pas d uniform oublie par Kotlin`() {
        val expected = SeaUniforms.ALL.toSet() + "u_texture" // u_texture est fourni par SpriteBatch
        assertEquals(expected, declaredUniforms(fragment))
    }

    @Test fun `la boucle d octaves couvre le maximum autorise`() {
        assertTrue("i < ${SeaWaves.MAX_OCTAVES}" in fragment, "Borne de boucle différente de MAX_OCTAVES")
    }

    @Test fun `le vertex shader fournit ce que SpriteBatch attend`() {
        for (name in listOf("a_position", "a_color", "a_texCoord0", "u_projTrans")) {
            assertTrue(name in vertex, "$name absent du vertex shader")
        }
    }

    @Test fun `le fragment shader reste en GLSL ES 1 point 0`() {
        // Ni #version, ni texture() de GLSL 3 : WebGL 1 et OpenGL ES 2 doivent le compiler.
        val code = fragment.lines().joinToString("\n") { it.substringBefore("//") }
        assertTrue("#version" !in code)
        assertTrue(Regex("""\btexture\s*\(""").find(code) == null, "texture() de GLSL 3 utilisé")
        assertTrue("texture2D" in code)
        assertTrue("precision" in code)
    }
}
