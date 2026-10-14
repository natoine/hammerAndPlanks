// Mer animée de Hammer and Planks.
// Portage du shader Unity de l'ancien jeu (Assets/Shaders/PerlinNormalHeight.shader, « Parallax Specular Proc ») :
// une turbulence de bruit 3D donne une hauteur de vague h, qui décale la texture (parallaxe)
// et dessine l'écume là où h approche 1. La 3e coordonnée du bruit (phase) fait vivre les vagues sur place.
// GLSL ES 1.0 : compatible OpenGL ES 2 (Android, iOS), WebGL 1 et OpenGL desktop.

#ifdef GL_ES
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
#endif

varying vec4 v_color;
varying vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_screenSize;       // taille du framebuffer en pixels
uniform float u_tileHeight;      // hauteur à l'écran d'une tuile de texture (pixels)
uniform float u_seaOffset;       // défilement de la texture, dans [0, u_tileHeight[
uniform vec2 u_noiseScale;       // fréquences du bruit, par largeur d'écran (x, y)
uniform float u_noiseOffset;     // défilement du bruit, en unités de bruit, modulo 289
uniform float u_phase;           // évolution des vagues sur place, modulo 289
uniform float u_octaves;         // nombre d'octaves de turbulence (1 à 5)
uniform vec2 u_perlinParams;     // lacunarité, gain
uniform float u_parallaxPx;      // amplitude du décalage de texture, en pixels
uniform vec3 u_foamBand;         // position, largeur, lissage de la bande d'écume
uniform vec4 u_foamColor;        // couleur de l'écume (alpha = intensité)

// ---------------------------------------------------------------------------------------------
// Bruit simplex 3D. Description : Ian McEwan, Ashima Arts, et Stefan Gustavson.
// Copyright (C) 2011 Ashima Arts. Licence MIT (https://github.com/ashima/webgl-noise).
// Périodique de période 289 : les décalages peuvent être ramenés modulo 289 sans couture.
// ---------------------------------------------------------------------------------------------
vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x * 34.0) + 1.0) * x); }
vec4 taylorInvSqrt(vec4 r) { return 1.79284291400159 - 0.85373472095314 * r; }

float snoise(vec3 v) {
    const vec2 C = vec2(1.0 / 6.0, 1.0 / 3.0);
    const vec4 D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);

    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = mod289(i);
    vec4 p = permute(permute(permute(
                i.z + vec4(0.0, i1.z, i2.z, 1.0))
              + i.y + vec4(0.0, i1.y, i2.y, 1.0))
              + i.x + vec4(0.0, i1.x, i2.x, 1.0));

    float n_ = 0.142857142857;
    vec3 ns = n_ * D.wyz - D.xzx;

    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);
    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);

    vec4 x = x_ * ns.x + ns.yyyy;
    vec4 y = y_ * ns.x + ns.yyyy;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);
    vec4 s0 = floor(b0) * 2.0 + 1.0;
    vec4 s1 = floor(b1) * 2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw * sh.xxyy;
    vec4 a1 = b1.xzyw + s1.xzyw * sh.zzww;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    vec4 norm = taylorInvSqrt(vec4(dot(p0, p0), dot(p1, p1), dot(p2, p2), dot(p3, p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0, x0), dot(x1, x1), dot(x2, x2), dot(x3, x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m * m, vec4(dot(p0, x0), dot(p1, x1), dot(p2, x2), dot(p3, x3)));
}
// ---------------------------------------------------------------------------------------------

// Turbulence de l'ancien shader : somme d'octaves, valeur absolue.
float turbulence(vec3 p) {
    float sum = 0.0;
    float scale = 1.0;
    float amplitude = 1.0;
    for (int i = 0; i < 5; i++) {          // borne constante exigée par GLSL ES 1.0
        if (float(i) >= u_octaves) break;
        sum += amplitude * snoise(p * scale);
        scale *= u_perlinParams.x;
        amplitude *= u_perlinParams.y;
    }
    return abs(sum);
}

void main() {
    vec2 frag = gl_FragCoord.xy;
    float width = u_screenSize.x;

    vec3 p = vec3(
        frag.x / width * u_noiseScale.x,
        frag.y / width * u_noiseScale.y + u_noiseOffset,
        u_phase);
    float n = turbulence(p);
    float h = 1.0 - n;

    // Parallaxe : la hauteur de vague décale la texture verticalement (caméra inclinée de l'ancien jeu).
    float shift = (h - 0.5) * u_parallaxPx;
    vec2 uv = vec2(frag.x / width, fract((frag.y + u_seaOffset + shift) / u_tileHeight));
    uv.y = 1.0 - uv.y; // les textures libGDX ont l'origine en haut
    vec4 tex = texture2D(u_texture, uv);

    // Écume : même bande que l'ancien shader (smoothstep sur h autour de u_foamBand.x).
    float pos = u_foamBand.x;
    float bandWidth = u_foamBand.y;
    float smoothing = u_foamBand.z;
    float w = smoothstep(pos - bandWidth - smoothing, pos - bandWidth, h)
            * smoothstep(-pos - bandWidth - smoothing, -pos - bandWidth, -h);

    vec3 color = tex.rgb + w * u_foamColor.rgb * u_foamColor.a;
    gl_FragColor = vec4(color, 1.0) * v_color;
}
