"""Tests du script de rendu des bateaux. Lancent Blender en mode sans fenêtre.

Usage : python3 assets-src/blender/test_render_boat.py
Prérequis : blender dans le PATH, Pillow installé.
"""

import os
import shutil
import subprocess
import tempfile
import unittest

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
SCRIPT = os.path.join(HERE, "render_boat.py")
BOAT = os.path.join(HERE, "..", "boats", "player")
BASE_ARGS = [
    "--hull", os.path.join(BOAT, "Boat_player.fbx"),
    "--hull-tex", os.path.join(BOAT, "Boat_player.png"),
    "--sail", os.path.join(BOAT, "Boat_player_Voile.fbx"),
    "--sail-tex", os.path.join(BOAT, "Boat_player_voile.png"),
]


def render(args):
    cmd = ["blender", "-b", "--python-exit-code", "1", "--python", SCRIPT, "--"] + args
    return subprocess.run(cmd, capture_output=True, text=True, timeout=600)


@unittest.skipUnless(shutil.which("blender"), "Blender n'est pas installé")
class RenderBoatTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp()
        self.out = os.path.join(self.tmp, "boat")

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def test_vue_dessus_proue_vers_le_haut(self):
        res = render(BASE_ARGS + ["--view", "top", "--size", "128", "--out", self.out])
        self.assertEqual(res.returncode, 0, res.stderr)
        img = Image.open(self.out + "_top_00.png")
        self.assertEqual(img.size, (128, 128))
        self.assertEqual(img.mode, "RGBA")
        # Fond transparent : les coins sont vides.
        self.assertEqual(img.getpixel((0, 0))[3], 0)
        left, top, right, bottom = img.getchannel("A").getbbox()
        # Bateau centré horizontalement et qui remplit l'essentiel de la hauteur.
        self.assertAlmostEqual((left + right) / 2, 64, delta=6)
        self.assertGreater(bottom - top, 128 * 0.7)
        # La proue est pointue : la première ligne opaque est plus étroite que le milieu.
        alpha = img.getchannel("A")
        def width(y):
            xs = [x for x in range(128) if alpha.getpixel((x, y)) > 0]
            return (max(xs) - min(xs)) if xs else 0
        self.assertLess(width(top + 3), width((top + bottom) // 2))

    def test_plusieurs_angles_et_taille_minimale(self):
        res = render(BASE_ARGS + ["--view", "34", "--size", "16", "--angles", "4", "--out", self.out])
        self.assertEqual(res.returncode, 0, res.stderr)
        for i in range(4):
            img = Image.open(f"{self.out}_34_{i:02d}.png")
            self.assertEqual(img.size, (16, 16))
            self.assertIsNotNone(img.getchannel("A").getbbox(), f"angle {i} vide")

    def test_coque_seule_sans_voile(self):
        args = BASE_ARGS[:4] + ["--size", "32", "--out", self.out]
        res = render(args)
        self.assertEqual(res.returncode, 0, res.stderr)
        self.assertTrue(os.path.exists(self.out + "_top_00.png"))

    def test_erreur_voile_sans_texture(self):
        res = render(BASE_ARGS[:6] + ["--out", self.out])
        self.assertNotEqual(res.returncode, 0)
        self.assertFalse(os.path.exists(self.out + "_top_00.png"))

    def test_erreur_angles_invalides(self):
        res = render(BASE_ARGS + ["--angles", "0", "--out", self.out])
        self.assertNotEqual(res.returncode, 0)

    def _semi_transparent_ratio(self, path):
        """Part des pixels visibles du bateau rendus à travers l'eau (opacité entre 15 et 50 %)."""
        alpha = Image.open(path).getchannel("A")
        visible = [v for v in alpha.getdata() if v > 8]
        partial = [v for v in visible if 38 <= v <= 128]
        return len(partial) / len(visible)

    def test_tirant_d_eau_carene_vue_a_travers_l_eau(self):
        # Vue de profil (angle 1 sur 4 : 90°), où la carène sous la flottaison se voit bien.
        dry = render(BASE_ARGS + ["--view", "34", "--size", "96", "--angles", "4", "--out", self.out + "_sec"])
        wet = render(BASE_ARGS + ["--view", "34", "--size", "96", "--angles", "4", "--draft", "0.091", "--out", self.out])
        self.assertEqual(dry.returncode, 0, dry.stderr)
        self.assertEqual(wet.returncode, 0, wet.stderr)
        before = self._semi_transparent_ratio(f"{self.out}_sec_34_01.png")
        after = self._semi_transparent_ratio(f"{self.out}_34_01.png")
        self.assertGreater(after, before + 0.05, f"Carène immergée : {before:.2f} -> {after:.2f}")

    def test_erreur_tirant_d_eau_ou_opacite_invalides(self):
        self.assertNotEqual(render(BASE_ARGS + ["--draft", "-1", "--out", self.out]).returncode, 0)
        self.assertNotEqual(render(BASE_ARGS + ["--underwater-alpha", "2", "--out", self.out]).returncode, 0)

    def test_erreur_vue_inconnue(self):
        res = render(BASE_ARGS + ["--view", "side", "--out", self.out])
        self.assertNotEqual(res.returncode, 0)


if __name__ == "__main__":
    unittest.main()
