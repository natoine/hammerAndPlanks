"""Tests du rendu des pictos : python3 assets-src/ui/test_render_icons.py -v"""
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import render_icons  # noqa: E402
from PIL import Image  # noqa: E402


class RenderIconsTest(unittest.TestCase):
    def test_chaque_picto_est_visible_centre_et_transparent_autour(self):
        for name, codepoint in render_icons.ICONS.items():
            image = render_icons.render(codepoint, 128)
            self.assertEqual((128, 128), image.size, name)
            alpha = image.getchannel("A")
            box = alpha.getbbox()
            self.assertIsNotNone(box, f"{name} est vide")
            self.assertEqual(0, alpha.getpixel((0, 0)), f"{name} : coin non transparent")
            cx, cy = (box[0] + box[2]) / 2, (box[1] + box[3]) / 2
            self.assertLess(abs(cx - 64), 4, f"{name} mal centré horizontalement")
            self.assertLess(abs(cy - 64), 4, f"{name} mal centré verticalement")

    def test_pictos_differents(self):
        images = [render_icons.render(cp, 64).tobytes() for cp in render_icons.ICONS.values()]
        self.assertEqual(len(images), len(set(images)))

    def test_ecrit_les_fichiers(self):
        with tempfile.TemporaryDirectory() as out:
            self.assertEqual(0, render_icons.main(["--size", "32", "--out", out]))
            for name in render_icons.ICONS:
                self.assertEqual((32, 32), Image.open(os.path.join(out, f"{name}.png")).size)

    def test_taille_trop_petite_refusee(self):
        with self.assertRaises(ValueError):
            render_icons.render(0xE312, 4)

    def test_glyphe_absent_refuse(self):
        with self.assertRaises(ValueError):
            render_icons.render(0x0041, 64)  # « A » : absent de la police d'icônes


if __name__ == "__main__":
    unittest.main()
