#!/usr/bin/env python3
"""Rend les pictos des modes d'interaction en PNG blancs sur fond transparent.

Source : police Material Icons (Google, licence Apache 2.0, voir MaterialIcons-LICENSE.txt).
Les PNG sont teintés au dessin par le jeu. Usage :
    python3 assets-src/ui/render_icons.py [--size 128] [--out assets/ui]
"""
import argparse
import os
import sys

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
FONT = os.path.join(HERE, "MaterialIcons-Regular.ttf")

# Nom du fichier -> point de code Material Icons. Doit correspondre à InputModeIcons.kt.
ICONS = {
    "input_keyboard": 0xE312,  # keyboard
    "input_gamepad": 0xEA28,   # sports_esports
    "input_motion": 0xE04B,    # videocam
    "camera_off": 0xE04C,      # videocam_off : caméra indisponible ou refusée
}

# Pictos barrés, dérivés d'un picto de ICONS pour garder exactement le même dessin.
CROSSED = {
    "input_gamepad_off": "input_gamepad",  # aucune manette détectée
}


# Caractères sans picto : hors de la police (glyphe de remplacement) ou espace. Leur dessin sert de référence
# pour refuser un point de code erroné, qui produirait l'un de ces glyphes au lieu d'un picto.
NOT_ICONS = (0x10FFFD, 0x0020)


def _draw(codepoint: int, big: int) -> Image.Image:
    font = ImageFont.truetype(FONT, big)
    canvas = Image.new("RGBA", (big * 2, big * 2), (255, 255, 255, 0))
    ImageDraw.Draw(canvas).text((big // 2, big // 2), chr(codepoint), font=font, fill=(255, 255, 255, 255))
    return canvas


def render(codepoint: int, size: int, fill: float = 0.9) -> Image.Image:
    """Dessine le glyphe, le recadre sur ses pixels réels et le centre dans un carré de [size] px."""
    if size < 8:
        raise ValueError(f"Taille trop petite : {size}")
    big = size * 2
    canvas = _draw(codepoint, big)
    box = canvas.getchannel("A").getbbox()
    if box is None or any(canvas.tobytes() == _draw(cp, big).tobytes() for cp in NOT_ICONS):
        raise ValueError(f"Glyphe absent de la police pour U+{codepoint:04X}")
    glyph = canvas.crop(box)
    scale = size * fill / max(glyph.width, glyph.height)
    glyph = glyph.resize((max(1, round(glyph.width * scale)), max(1, round(glyph.height * scale))), Image.LANCZOS)
    image = Image.new("RGBA", (size, size), (255, 255, 255, 0))
    image.alpha_composite(glyph, ((size - glyph.width) // 2, (size - glyph.height) // 2))
    return image


def crossed(image: Image.Image) -> Image.Image:
    """Barre un picto en diagonale, à la manière des icônes « off » de Material : un trait plein
    bordé d'un liseré transparent qui le détache du dessin."""
    size = image.width
    out = image.copy()
    draw = ImageDraw.Draw(out)
    start, end = (size * 0.12, size * 0.12), (size * 0.88, size * 0.88)
    # Sur une image RGBA, ImageDraw remplace les pixels : le liseré efface vraiment le dessin.
    draw.line([start, end], fill=(255, 255, 255, 0), width=max(2, round(size * 0.2)))
    draw.line([start, end], fill=(255, 255, 255, 255), width=max(1, round(size * 0.08)))
    return out


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--size", type=int, default=128)
    parser.add_argument("--out", default=os.path.join(HERE, "..", "..", "assets", "ui"))
    args = parser.parse_args(argv)
    os.makedirs(args.out, exist_ok=True)
    rendered = {}
    for name, codepoint in ICONS.items():
        rendered[name] = render(codepoint, args.size)
    for name, source in CROSSED.items():
        rendered[name] = crossed(rendered[source])
    for name, image in rendered.items():
        path = os.path.join(args.out, f"{name}.png")
        image.save(path)
        print(path)
    return 0


if __name__ == "__main__":
    sys.exit(main())
