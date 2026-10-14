"""Rendu d'un bateau (FBX coque + voile) en sprites 2D sur fond transparent.

Usage (Blender en ligne de commande) :
    blender -b --python assets-src/blender/render_boat.py -- \
        --hull assets-src/boats/player/Boat_player.fbx \
        --hull-tex assets-src/boats/player/Boat_player.png \
        --sail assets-src/boats/player/Boat_player_Voile.fbx \
        --sail-tex assets-src/boats/player/Boat_player_voile.png \
        --view top --size 512 --angles 1 \
        --out assets-src/boats/player/render/boat_player

Options :
    --view    top (vue de dessus) ou 34 (vue en 3/4, caméra inclinée)
    --angles  nombre de rotations autour de l'axe vertical (1 = bateau seul, cap vers le haut)
    --size    taille en pixels d'une image carrée
    --heading décalage de cap en degrés (180 par défaut : la proue des FBX de l'ancien jeu pointe vers -Y)
Sortie : <out>_<view>_<angle>.png, une image par angle.
"""

import argparse
import math
import os
import sys

import bpy
from mathutils import Vector

# Rampe cartoon reprise du shader Unity « Toony-Lighted » : 3 paliers d'éclairage.
TOON_STOPS = [(0.0, 0.55), (0.35, 0.8), (0.7, 1.0)]
VIEW_TILT_DEG = {"top": 0.0, "34": 35.0}


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument("--hull", required=True)
    p.add_argument("--hull-tex", required=True)
    p.add_argument("--sail")
    p.add_argument("--sail-tex")
    p.add_argument("--view", choices=sorted(VIEW_TILT_DEG), default="top")
    p.add_argument("--size", type=int, default=512)
    p.add_argument("--angles", type=int, default=1)
    p.add_argument("--heading", type=float, default=180.0)
    p.add_argument("--out", required=True)
    args = p.parse_args(argv)
    if args.angles < 1 or args.size < 16:
        p.error("--angles doit être >= 1 et --size >= 16")
    if bool(args.sail) != bool(args.sail_tex):
        p.error("--sail et --sail-tex vont ensemble")
    return args


def toon_material(name, texture_path, double_sided):
    """Texture × rampe d'éclairage à paliers, comme l'ancien rendu Unity."""
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    mat.use_backface_culling = not double_sided
    nt = mat.node_tree
    nt.nodes.clear()
    tex = nt.nodes.new("ShaderNodeTexImage")
    tex.image = bpy.data.images.load(os.path.abspath(texture_path))
    diffuse = nt.nodes.new("ShaderNodeBsdfDiffuse")
    to_rgb = nt.nodes.new("ShaderNodeShaderToRGB")
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.interpolation = "CONSTANT"
    els = ramp.color_ramp.elements
    while len(els) < len(TOON_STOPS):
        els.new(0.5)
    for el, (pos, v) in zip(els, TOON_STOPS):
        el.position = pos
        el.color = (v, v, v, 1.0)
    mix = nt.nodes.new("ShaderNodeMix")
    mix.data_type = "RGBA"
    mix.blend_type = "MULTIPLY"
    mix.inputs["Factor"].default_value = 1.0
    emit = nt.nodes.new("ShaderNodeEmission")
    out = nt.nodes.new("ShaderNodeOutputMaterial")
    nt.links.new(diffuse.outputs["BSDF"], to_rgb.inputs["Shader"])
    nt.links.new(to_rgb.outputs["Color"], ramp.inputs["Fac"])
    nt.links.new(tex.outputs["Color"], mix.inputs["A"])
    nt.links.new(ramp.outputs["Color"], mix.inputs["B"])
    nt.links.new(mix.outputs["Result"], emit.inputs["Color"])
    nt.links.new(emit.outputs["Emission"], out.inputs["Surface"])
    return mat


def import_part(path, material):
    before = set(bpy.data.objects)
    bpy.ops.import_scene.fbx(filepath=os.path.abspath(path))
    new = set(bpy.data.objects) - before
    for o in new:
        if o.type == "MESH":
            o.data.materials.clear()
            o.data.materials.append(material)
    return [o for o in new if o.parent is None]


def world_bounds(objs):
    pts = [o.matrix_world @ Vector(c) for o in objs if o.type == "MESH" for c in o.bound_box]
    lo = Vector((min(p.x for p in pts), min(p.y for p in pts), min(p.z for p in pts)))
    hi = Vector((max(p.x for p in pts), max(p.y for p in pts), max(p.z for p in pts)))
    return lo, hi


def setup_scene(size):
    scene = bpy.context.scene
    engines = {e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items}
    scene.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else "BLENDER_EEVEE"
    scene.render.resolution_x = scene.render.resolution_y = size
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.view_settings.view_transform = "Standard"
    world = bpy.data.worlds.new("World")
    world.color = (0.0, 0.0, 0.0)
    scene.world = world
    sun = bpy.data.objects.new("Sun", bpy.data.lights.new("Sun", "SUN"))
    sun.data.energy = 3.0
    sun.rotation_euler = (math.radians(40), math.radians(15), math.radians(-30))
    scene.collection.objects.link(sun)
    return scene


def main():
    args = parse_args()
    bpy.ops.wm.read_factory_settings(use_empty=True)
    scene = setup_scene(args.size)

    pivot = bpy.data.objects.new("Pivot", None)
    scene.collection.objects.link(pivot)
    roots = import_part(args.hull, toon_material("Hull", args.hull_tex, double_sided=False))
    if args.sail:
        roots += import_part(args.sail, toon_material("Sail", args.sail_tex, double_sided=True))
    bpy.context.view_layer.update()

    # Centre le bateau sur l'origine, ligne de flottaison posée sur z = 0.
    lo, hi = world_bounds(bpy.data.objects)
    center = Vector(((lo.x + hi.x) / 2, (lo.y + hi.y) / 2, lo.z))
    for r in roots:
        r.location -= center
        r.parent = pivot
    bpy.context.view_layer.update()
    lo, hi = world_bounds(bpy.data.objects)
    # Rayon horizontal : le bateau tient dans le cadre quel que soit l'angle de rotation.
    radius_xy = max(Vector((x, y)).length for x in (lo.x, hi.x) for y in (lo.y, hi.y))
    tilt = math.radians(VIEW_TILT_DEG[args.view])
    half_height = (hi.z - lo.z) / 2
    # Demi-hauteur projetée : longueur raccourcie par l'inclinaison + hauteur du mât vue de biais.
    half_extent = max(radius_xy, radius_xy * math.cos(tilt) + half_height * math.sin(tilt))

    cam_data = bpy.data.cameras.new("Cam")
    cam_data.type = "ORTHO"
    cam_data.ortho_scale = half_extent * 2.1
    cam = bpy.data.objects.new("Cam", cam_data)
    scene.collection.objects.link(cam)
    scene.camera = cam
    dist = (hi - lo).length * 5
    target = Vector((0, 0, half_height))
    cam.location = target + Vector((0, -math.sin(tilt) * dist, math.cos(tilt) * dist))
    cam.rotation_euler = (tilt, 0, 0)
    cam_data.clip_end = dist * 3

    os.makedirs(os.path.dirname(os.path.abspath(args.out)) or ".", exist_ok=True)
    for i in range(args.angles):
        pivot.rotation_euler = (0, 0, math.radians(args.heading + 360.0 * i / args.angles))
        scene.render.filepath = os.path.abspath(f"{args.out}_{args.view}_{i:02d}.png")
        bpy.ops.render.render(write_still=True)
        print("RENDERED", scene.render.filepath)


if __name__ == "__main__":
    main()
