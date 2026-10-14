package com.naturalpad.hammerandplanks.sea

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.naturalpad.hammerandplanks.launch.LaunchAnimation

/**
 * Dessine la mer animée (vagues et écume) avec le shader assets/shaders/sea.frag.
 * Si le shader ne compile pas sur l'appareil, retombe sur la texture qui défile sans vagues.
 */
class SeaRenderer(private val waves: SeaWaves = SeaWaves()) {
    // Pas de mipmaps : la texture n'a pas des côtés en puissance de deux, ce que WebGL 1 refuse.
    private val texture = Texture(Gdx.files.internal("sea/sea.jpg")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    private val shader: ShaderProgram? = loadShader()

    /** Vrai si les vagues sont rendues par le shader, faux en mode de repli. */
    val animated: Boolean get() = shader != null

    private fun loadShader(): ShaderProgram? {
        ShaderProgram.pedantic = false
        val program = ShaderProgram(
            Gdx.files.internal(VERTEX_PATH).readString(),
            Gdx.files.internal(FRAGMENT_PATH).readString(),
        )
        return if (program.isCompiled) {
            Gdx.app.log(TAG, "Shader de mer compilé")
            program
        } else {
            Gdx.app.error(TAG, "Shader de mer non compilé, repli sans vagues : ${program.log}")
            program.dispose()
            null
        }
    }

    /**
     * Dessine la mer sur [width] x [height] pixels à l'instant [timeS]. [batch] doit être entre begin() et end().
     * La texture couvre toute la largeur et se raccorde verticalement.
     */
    fun draw(batch: SpriteBatch, width: Float, height: Float, timeS: Float) {
        val tileHeight = texture.height * (width / texture.width)
        val offset = LaunchAnimation.seaOffset(timeS, tileHeight)
        val program = shader
        if (program == null) {
            drawTiles(batch, width, height, tileHeight, offset)
            return
        }
        batch.shader = program
        // Le shader travaille en pixels du framebuffer (gl_FragCoord), qui peuvent différer des unités logiques.
        val fbWidth = Gdx.graphics.backBufferWidth.toFloat()
        val scale = fbWidth / width
        val scrolledPx = timeS * LaunchAnimation.SEA_SPEED * tileHeight * scale
        program.setUniformf(SeaUniforms.SCREEN_SIZE, fbWidth, Gdx.graphics.backBufferHeight.toFloat())
        program.setUniformf(SeaUniforms.TILE_HEIGHT, tileHeight * scale)
        program.setUniformf(SeaUniforms.SEA_OFFSET, offset * scale)
        program.setUniformf(SeaUniforms.NOISE_SCALE, waves.noiseScaleX, waves.noiseScaleY)
        val noiseUnit = minOf(fbWidth, Gdx.graphics.backBufferHeight.toFloat())
        program.setUniformf(SeaUniforms.NOISE_UNIT, noiseUnit)
        program.setUniformf(SeaUniforms.NOISE_OFFSET, waves.noiseOffset(scrolledPx, noiseUnit))
        program.setUniformf(SeaUniforms.PHASE, waves.phase(timeS))
        program.setUniformf(SeaUniforms.OCTAVES, waves.octaves.toFloat())
        program.setUniformf(SeaUniforms.PERLIN_PARAMS, waves.lacunarity, waves.gain)
        program.setUniformf(SeaUniforms.PARALLAX_PX, waves.parallaxPx(noiseUnit))
        program.setUniformf(SeaUniforms.FOAM_BAND, waves.foamBandPos, waves.foamBandWidth, waves.foamSmoothBand)
        program.setUniformf(SeaUniforms.FOAM_COLOR, waves.foamR, waves.foamG, waves.foamB, waves.foamIntensity)
        // Un seul quad plein écran : le shader calcule lui-même les coordonnées de texture.
        batch.draw(texture, 0f, 0f, width, height)
        batch.flush()
        batch.shader = null
    }

    private fun drawTiles(batch: SpriteBatch, width: Float, height: Float, tileHeight: Float, offset: Float) {
        var y = -offset
        while (y < height) {
            batch.draw(texture, 0f, y, width, tileHeight)
            y += tileHeight
        }
    }

    fun dispose() {
        shader?.dispose()
        texture.dispose()
    }

    companion object {
        const val VERTEX_PATH = "shaders/sea.vert"
        const val FRAGMENT_PATH = "shaders/sea.frag"
        private const val TAG = "Sea"
    }
}

/** Noms des uniforms du shader de mer, partagés avec les tests de cohérence. */
object SeaUniforms {
    const val SCREEN_SIZE = "u_screenSize"
    const val TILE_HEIGHT = "u_tileHeight"
    const val SEA_OFFSET = "u_seaOffset"
    const val NOISE_UNIT = "u_noiseUnit"
    const val NOISE_SCALE = "u_noiseScale"
    const val NOISE_OFFSET = "u_noiseOffset"
    const val PHASE = "u_phase"
    const val OCTAVES = "u_octaves"
    const val PERLIN_PARAMS = "u_perlinParams"
    const val PARALLAX_PX = "u_parallaxPx"
    const val FOAM_BAND = "u_foamBand"
    const val FOAM_COLOR = "u_foamColor"

    val ALL = listOf(
        SCREEN_SIZE, TILE_HEIGHT, SEA_OFFSET, NOISE_UNIT, NOISE_SCALE, NOISE_OFFSET, PHASE,
        OCTAVES, PERLIN_PARAMS, PARALLAX_PX, FOAM_BAND, FOAM_COLOR,
    )
}
