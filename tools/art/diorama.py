"""Renders the museum as a miniature diorama in Blender (Cycles), plus the kind showcases and the icon.

blender -b -P tools/art/diorama.py -- <job> [args]      (driven by tools/art/render.py)

Jobs:
  facade <stage 0..7> <day|night>  -> build/diorama/facade_<stage>_<day|night>.png (transparent, 1080×720)
  kind <key>                       -> build/diorama/kind_<key>.png (960×600)
  icon                             -> build/diorama/icon.png (transparent, 864×864)

Units are metres at model scale: the plinth is 62 cm wide, so a long lens and a shallow focus make it read
as a model on a table. Everything is built from primitives with bevels, procedural materials and real light.
"""
import bpy
import bmesh
import math
import os
import random
import sys
from mathutils import Vector

ARGS = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "build", "diorama")
os.makedirs(OUT, exist_ok=True)
NIGHT = False


# ---------------------------------------------------------------- basics

def srgb(h):
    h = h.lstrip("#")
    c = [int(h[i:i + 2], 16) / 255 for i in (0, 2, 4)]
    return tuple(v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in c)


def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    prefs = bpy.context.preferences.addons["cycles"].preferences
    for kind in ("OPTIX", "CUDA"):
        try:
            prefs.compute_device_type = kind
            prefs.get_devices()
            if any(d.type == kind for d in prefs.devices):
                for d in prefs.devices:
                    d.use = d.type == kind
                sc.cycles.device = "GPU"
                break
        except Exception:
            pass
    sc.cycles.samples = 160
    sc.cycles.use_denoising = True
    sc.view_settings.view_transform = "AgX"
    sc.view_settings.look = "AgX - Medium High Contrast"
    sc.render.film_transparent = True
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGBA"
    w = bpy.data.worlds.new("World")
    sc.world = w
    w.use_nodes = True
    return sc


def world(color, strength):
    bg = bpy.context.scene.world.node_tree.nodes["Background"]
    bg.inputs["Color"].default_value = (*srgb(color), 1)
    bg.inputs["Strength"].default_value = strength


_mats = {}


def mat(name, color, rough=0.6, metal=0.0, emit=None, emit_strength=0.0, bump=0.0, bump_scale=300.0, kind=None, coat=0.0):
    key = (name, NIGHT)
    if key in _mats:
        return _mats[key]
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    b = nt.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value = (*srgb(color), 1)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metal
    b.inputs["Coat Weight"].default_value = coat
    if emit and emit_strength > 0:
        b.inputs["Emission Color"].default_value = (*srgb(emit), 1)
        b.inputs["Emission Strength"].default_value = emit_strength
    tc = nt.nodes.new("ShaderNodeTexCoord")
    if kind == "wood":
        wave = nt.nodes.new("ShaderNodeTexWave")
        wave.inputs["Scale"].default_value = 30
        wave.inputs["Distortion"].default_value = 3
        wave.inputs["Detail"].default_value = 3
        ramp = nt.nodes.new("ShaderNodeValToRGB")
        c = srgb(color)
        ramp.color_ramp.elements[0].color = (c[0] * 0.78, c[1] * 0.74, c[2] * 0.7, 1)
        ramp.color_ramp.elements[1].color = (*c, 1)
        nt.links.new(tc.outputs["Object"], wave.inputs["Vector"])
        nt.links.new(wave.outputs["Fac"], ramp.inputs["Fac"])
        nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    if kind == "paving":
        br = nt.nodes.new("ShaderNodeTexBrick")
        br.inputs["Scale"].default_value = 60
        br.inputs["Mortar Size"].default_value = 0.012
        c = srgb(color)
        br.inputs["Color1"].default_value = (*c, 1)
        br.inputs["Color2"].default_value = (c[0] * 0.86, c[1] * 0.84, c[2] * 0.8, 1)
        br.inputs["Mortar"].default_value = (c[0] * 0.6, c[1] * 0.58, c[2] * 0.55, 1)
        nt.links.new(tc.outputs["Object"], br.inputs["Vector"])
        nt.links.new(br.outputs["Color"], b.inputs["Base Color"])
        bp = nt.nodes.new("ShaderNodeBump")
        bp.inputs["Strength"].default_value = 0.35
        nt.links.new(br.outputs["Fac"], bp.inputs["Height"])
        nt.links.new(bp.outputs["Normal"], b.inputs["Normal"])
    if kind == "stripes":
        wave = nt.nodes.new("ShaderNodeTexWave")
        wave.wave_profile = "SAW"
        wave.inputs["Scale"].default_value = 3
        ramp = nt.nodes.new("ShaderNodeValToRGB")
        ramp.color_ramp.interpolation = "CONSTANT"
        ramp.color_ramp.elements[0].color = (*srgb(color), 1)
        ramp.color_ramp.elements[1].position = 0.5
        ramp.color_ramp.elements[1].color = (*srgb("#F3EBDD"), 1)
        nt.links.new(tc.outputs["Generated"], wave.inputs["Vector"])
        nt.links.new(wave.outputs["Fac"], ramp.inputs["Fac"])
        nt.links.new(ramp.outputs["Color"], b.inputs["Base Color"])
    if bump > 0:
        nz = nt.nodes.new("ShaderNodeTexNoise")
        nz.inputs["Scale"].default_value = bump_scale
        nz.inputs["Detail"].default_value = 6
        bp = nt.nodes.new("ShaderNodeBump")
        bp.inputs["Strength"].default_value = bump
        nt.links.new(tc.outputs["Object"], nz.inputs["Vector"])
        nt.links.new(nz.outputs["Fac"], bp.inputs["Height"])
        nt.links.new(bp.outputs["Normal"], b.inputs["Normal"])
    _mats[key] = m
    return m


def finish(o, m, bevel=0.0, seg=2, smooth=False):
    if m:
        o.data.materials.append(m)
    if bevel > 0:
        md = o.modifiers.new("bevel", "BEVEL")
        md.width = bevel
        md.segments = seg
        md.limit_method = "ANGLE"
    if smooth:
        for p in o.data.polygons:
            p.use_smooth = True
    return o


def apply_scale(o):
    bpy.ops.object.select_all(action="DESELECT")
    o.select_set(True)
    bpy.context.view_layer.objects.active = o
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)


def box(x, y, z, sx, sy, sz, m, bevel=0.0008):
    """A box by its bottom-centre."""
    bpy.ops.mesh.primitive_cube_add(location=(x, y, z + sz / 2))
    o = bpy.context.object
    o.scale = (sx / 2, sy / 2, sz / 2)
    apply_scale(o)
    return finish(o, m, bevel)


def cyl(x, y, z, r, h, m, verts=32, r2=None, rot=None, bevel=0.0):
    if r2 is None:
        bpy.ops.mesh.primitive_cylinder_add(vertices=verts, radius=r, depth=h, location=(x, y, z + h / 2))
    else:
        bpy.ops.mesh.primitive_cone_add(vertices=verts, radius1=r, radius2=r2, depth=h, location=(x, y, z + h / 2))
    o = bpy.context.object
    if rot:
        o.rotation_euler = rot
    return finish(o, m, bevel, smooth=True)


def sphere(x, y, z, r, m, sx=1, sy=1, sz=1, seg=32):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=seg, ring_count=seg // 2, radius=r, location=(x, y, z))
    o = bpy.context.object
    o.scale = (sx, sy, sz)
    return finish(o, m, smooth=True)


def prism(pts, y0, y1, m, bevel=0.0006):
    """A shape drawn in the x–z plane, extruded from y0 back to y1."""
    me = bpy.data.meshes.new("prism")
    bm = bmesh.new()
    front = [bm.verts.new((x, y0, z)) for x, z in pts]
    back = [bm.verts.new((x, y1, z)) for x, z in pts]
    bm.faces.new(front[::-1])
    bm.faces.new(back)
    n = len(pts)
    for i in range(n):
        bm.faces.new([front[i], front[(i + 1) % n], back[(i + 1) % n], back[i]])
    bm.normal_update()
    bm.to_mesh(me)
    bm.free()
    o = bpy.data.objects.new("prism", me)
    bpy.context.collection.objects.link(o)
    bpy.context.view_layer.objects.active = o
    o.select_set(True)
    return finish(o, m, bevel)


def point_light(x, y, z, power, color="#FFC27A", radius=0.004):
    l = bpy.data.lights.new("lamp", "POINT")
    l.energy = power
    l.color = srgb(color)
    l.shadow_soft_size = radius
    o = bpy.data.objects.new("lamp", l)
    o.location = (x, y, z)
    bpy.context.collection.objects.link(o)
    return o


def sun(rot, strength, color, angle=3.0):
    l = bpy.data.lights.new("sun", "SUN")
    l.energy = strength
    l.color = srgb(color)
    l.angle = math.radians(angle)
    o = bpy.data.objects.new("sun", l)
    o.rotation_euler = [math.radians(a) for a in rot]
    bpy.context.collection.objects.link(o)
    return o


def area(loc, rot, size, power, color="#FFFFFF"):
    l = bpy.data.lights.new("area", "AREA")
    l.energy = power
    l.size = size
    l.color = srgb(color)
    o = bpy.data.objects.new("area", l)
    o.location = loc
    o.rotation_euler = [math.radians(a) for a in rot]
    bpy.context.collection.objects.link(o)
    return o


def camera(loc, target, lens=70, fstop=4.0, res=(1080, 720), shift=(0, 0)):
    sc = bpy.context.scene
    cd = bpy.data.cameras.new("cam")
    cd.lens = lens
    cd.shift_x, cd.shift_y = shift
    co = bpy.data.objects.new("cam", cd)
    bpy.context.collection.objects.link(co)
    co.location = loc
    d = Vector(target) - Vector(loc)
    co.rotation_euler = d.to_track_quat("-Z", "Y").to_euler()
    focus = bpy.data.objects.new("focus", None)
    focus.location = target
    bpy.context.collection.objects.link(focus)
    cd.dof.use_dof = True
    cd.dof.focus_object = focus
    cd.dof.aperture_fstop = fstop
    sc.camera = co
    sc.render.resolution_x, sc.render.resolution_y = res
    return co


def shadow_floor(z=-0.05):
    bpy.ops.mesh.primitive_plane_add(size=6, location=(0, 0, z))
    o = bpy.context.object
    o.is_shadow_catcher = True
    return o


# ---------------------------------------------------------------- palette

def M():
    """The materials of the current light (day or night)."""
    g = "#FFC982"
    glow = 9.0 if NIGHT else 0.0
    return {
        "stone": mat("stone", "#E9DDC5", 0.75, bump=0.08),
        "stone2": mat("stone2", "#D9CBB0", 0.8, bump=0.1),
        "roof": mat("roof", "#9A5E44", 0.65, bump=0.15, bump_scale=120),
        "copper": mat("copper", "#5F9C86", 0.45, metal=0.3, bump=0.05),
        "brass": mat("brass", "#C9A24E", 0.3, metal=1.0),
        "glass": mat("glass", "#2F3F45", 0.08, emit=g, emit_strength=glow, coat=0.5),
        "door": mat("door", "#5A3820", 0.5, emit=g, emit_strength=glow * 0.8, kind=None),
        "walnut": mat("walnut", "#7A4B2A", 0.45, kind="wood", coat=0.4),
        "walnut2": mat("walnut2", "#4A2D1A", 0.5, kind="wood"),
        "paving": mat("paving", "#CDBFA4", 0.85, kind="paving"),
        "grass": mat("grass", "#6E9A55", 0.9, bump=0.3, bump_scale=900),
        "leaf": mat("leaf", "#5E8F55", 0.7, bump=0.4, bump_scale=60),
        "leaf2": mat("leaf2", "#3F6B45", 0.7, bump=0.4, bump_scale=60),
        "trunk": mat("trunk", "#5C4430", 0.8),
        "iron": mat("iron", "#2A2724", 0.4, metal=0.6),
        "bulb": mat("bulb", "#FFE9C2", 0.2, emit="#FFC27A", emit_strength=30.0 if NIGHT else 0.5),
        "flowers": mat("flowers", "#C94A3A", 0.7),
        "flowers2": mat("flowers2", "#E9C46A", 0.7),
        "red": mat("red", "#A8362E", 0.6),
        "awning": mat("awning", "#B5473A", 0.7, kind="stripes"),
        "sign": mat("sign", "#2F4638", 0.5),
        "velvet": mat("velvet", "#24402F", 0.95, bump=0.2, bump_scale=800),
        "wallpaper": mat("wallpaper", "#2C4636", 0.95, bump=0.25, bump_scale=400),
        "floor": mat("floor", "#A8784A", 0.5, kind="wood", coat=0.3),
        "rug": mat("rug", "#8E3A30", 0.95, bump=0.3, bump_scale=600),
    }


PAINT = ["#3B3530", "#2F4A6B", "#9E3A30", "#5E5A3E", "#4F8C84", "#8E6E9E", "#C9714F", "#D9A441", "#2A2A2E", "#6B4A3A"]


# ---------------------------------------------------------------- pieces

def plinth(m, w=0.62, d=0.44):
    box(0, 0, -0.06, w + 0.03, d + 0.03, 0.045, m["walnut2"], 0.004)
    box(0, 0, -0.018, w, d, 0.018, m["walnut"], 0.003)
    box(0, 0, 0, w - 0.01, d - 0.01, 0.003, m["paving"], 0.0005)
    shadow_floor(-0.06)


def lawn(m, x, y, sx, sy, flowers=True, seed=0):
    box(x, y, 0.003, sx, sy, 0.004, m["grass"], 0.001)
    if flowers:
        r = random.Random(seed)
        for _ in range(int(sx * sy * 2600)):
            px = x + (r.random() - 0.5) * sx * 0.9
            py = y + (r.random() - 0.5) * sy * 0.9
            sphere(px, py, 0.0075, 0.0013, m["flowers"] if r.random() < 0.6 else m["flowers2"], seg=8)


def tree(m, x, y, s=1.0, seed=0, cypress=False):
    r = random.Random(seed)
    cyl(x, y, 0.003, 0.0025 * s, 0.03 * s, m["trunk"], verts=10)
    if cypress:
        sphere(x, y, 0.003 + 0.055 * s, 0.014 * s, m["leaf2"], sz=3.2, seg=16)
        for k in range(6):
            sphere(x + r.uniform(-0.006, 0.006) * s, y + r.uniform(-0.006, 0.006) * s, 0.003 + (0.03 + k * 0.011) * s, 0.009 * s, m["leaf"], sz=1.3, seg=12)
        return
    for k in range(9):
        a = r.uniform(0, math.tau)
        rr = r.uniform(0.004, 0.018) * s
        sphere(x + math.cos(a) * rr, y + math.sin(a) * rr, 0.003 + (0.045 + r.uniform(-0.01, 0.016)) * s, r.uniform(0.012, 0.018) * s,
               m["leaf"] if r.random() < 0.6 else m["leaf2"], seg=12)


def lantern(m, x, y, h=0.07):
    cyl(x, y, 0.003, 0.0035, 0.003, m["iron"], verts=12)
    cyl(x, y, 0.003, 0.0011, h, m["iron"], verts=8)
    box(x, y, 0.003 + h, 0.006, 0.006, 0.009, m["bulb"], 0.0004)
    cyl(x, y, 0.003 + h + 0.009, 0.0045, 0.004, m["iron"], verts=4, r2=0.0005)
    if NIGHT:
        point_light(x, y, 0.003 + h + 0.0045, 0.35)


def bench(m, x, y):
    box(x, y, 0.009, 0.03, 0.008, 0.0018, m["walnut"])
    box(x, y + 0.004, 0.011, 0.03, 0.0015, 0.008, m["walnut"])
    for dx in (-0.012, 0.012):
        box(x + dx, y, 0.003, 0.0015, 0.007, 0.006, m["iron"])


def person(m, x, y, r, child=False, hat=False, umbrella=False):
    s = 0.95 if child else 1.35
    paint = mat("paint" + r.choice(PAINT), r.choice(PAINT), 0.55)
    skin = mat("skin", "#D9B08C", 0.6)
    cyl(x - 0.0012 * s, y, 0.003, 0.0009 * s, 0.006 * s, mat("legs", "#2A2724", 0.6), verts=8)
    cyl(x + 0.0012 * s, y, 0.003, 0.0009 * s, 0.006 * s, mat("legs", "#2A2724", 0.6), verts=8)
    cyl(x, y, 0.003 + 0.006 * s, 0.0034 * s, 0.011 * s, paint, verts=16, r2=0.0022 * s)
    sphere(x, y, 0.003 + 0.0195 * s, 0.0024 * s, skin, seg=16)
    if hat:
        cyl(x, y, 0.003 + 0.0205 * s, 0.0036, 0.0006, mat("hat", "#2A2724", 0.6), verts=16)
        cyl(x, y, 0.003 + 0.021 * s, 0.0022, 0.0028, mat("hat", "#2A2724", 0.6), verts=16)
    if umbrella:
        cyl(x + 0.003, y, 0.003 + 0.012, 0.0003, 0.02, m["iron"], verts=6)
        sphere(x + 0.003, y, 0.003 + 0.031, 0.011, mat("umbrella", "#B5473A", 0.6), sz=0.45, seg=16)


def crowd(m, n, seed, keep_out):
    """Little painted figures on the plaza, never on the steps or in the doorway."""
    r = random.Random(seed)
    placed = []
    tries = 0
    while len(placed) < n and tries < 500:
        tries += 1
        x, y = r.uniform(-0.27, 0.27), r.uniform(-0.19, -0.03)
        if any(abs(x - a) < b and y > c for a, b, c in keep_out):
            continue
        if any((x - px) ** 2 + (y - py) ** 2 < 0.012 ** 2 for px, py in placed):
            continue
        placed.append((x, y))
        k = r.random()
        person(m, x, y, r, child=k < 0.15, hat=0.15 <= k < 0.35, umbrella=0.35 <= k < 0.45)


def arch_window(m, x, y, z, w, h, frame=True):
    """An arched window on a front wall at depth y (the wall faces -y)."""
    if frame:
        box(x, y - 0.0006, z - 0.002, w + 0.004, 0.0018, h + 0.004, m["stone"], 0.0004)
    box(x, y - 0.0016, z, w, 0.0016, h - w / 2, m["glass"], 0.0002)
    cyl(x, y - 0.0016, z + h - w / 2 - 0.0008, w / 2, 0.0016, m["glass"], verts=24, rot=(math.radians(90), 0, 0))
    box(x, y - 0.0024, z + (h - w / 2) * 0.55, w, 0.001, 0.0008, m["stone"], 0)
    box(x, y - 0.0024, z, 0.0008, 0.001, h - 0.002, m["stone"], 0)
    box(x, y - 0.004, z - 0.003, w + 0.007, 0.006, 0.0018, m["stone"], 0.0004)


def door(m, x, y, w, h):
    box(x, y - 0.001, 0.003, w + 0.008, 0.002, h + 0.004, m["stone"], 0.0006)
    box(x, y - 0.0022, 0.003, w, 0.0022, h - w / 2, m["door"], 0.0003)
    cyl(x, y - 0.0022, 0.003 + h - w / 2 - 0.0006, w / 2, 0.0022, m["door"], verts=24, rot=(math.radians(90), 0, 0))
    if NIGHT:
        point_light(x, y - 0.02, 0.006, 0.06, radius=0.01)


def column(m, x, y, z0, h, r):
    box(x, y, z0, r * 2.8, r * 2.8, 0.0035, m["stone"], 0.0004)
    cyl(x, y, z0 + 0.0035, r * 1.15, 0.002, m["stone"], verts=24)
    cyl(x, y, z0 + 0.0055, r, h - 0.012, m["stone"], verts=24, r2=r * 0.88)
    cyl(x, y, z0 + h - 0.0065, r * 0.95, 0.0025, m["stone"], verts=24, r2=r * 1.3)
    box(x, y, z0 + h - 0.004, r * 2.9, r * 2.9, 0.004, m["stone"], 0.0004)
    if NIGHT:
        point_light(x, y - 0.012, z0 + 0.004, 0.06, radius=0.003)


def temple(m, cx, y, w, h, cols, stage):
    d = 0.16
    z0 = 0.003
    # podium and steps
    for k in range(4):
        box(cx, y + 0.012 - k * 0.004, z0 + k * 0.0035, w + 0.03 - k * 0.006, d + 0.05 - k * 0.008, 0.0035, m["stone"] if k % 2 else m["stone2"], 0.0005)
    floor = z0 + 0.014
    # the hall behind the colonnade
    box(cx, y + d / 2 + 0.02, floor, w - 0.03, d - 0.02, h, m["stone"], 0.0008)
    face = y + 0.02 + 0.001
    gap = (w - 0.03) / (cols - 1)
    for k in range(cols - 1):
        mx = cx - (w - 0.03) / 2 + gap * (k + 0.5)
        if abs(mx - cx) < gap * 0.6:
            continue
        arch_window(m, mx, face, floor + 0.022, 0.016, 0.042)
    door(m, cx, face, 0.022, 0.052)
    for k in range(cols):
        column(m, cx - (w - 0.03) / 2 + gap * k, y, floor, h - 0.006, 0.0055)
    # entablature and cornice
    top = floor + h - 0.006
    box(cx, y + d / 2 + 0.005, top, w + 0.004, d + 0.02, 0.014, m["stone"], 0.0006)
    box(cx, y + d / 2 + 0.005, top + 0.014, w + 0.012, d + 0.028, 0.004, m["stone2"], 0.0006)
    for k in range(int(w / 0.006)):
        box(cx - w / 2 + 0.003 + k * 0.006, y - 0.005, top + 0.009, 0.0028, 0.002, 0.003, m["stone2"], 0)
    # pediment and roof
    ph = w * 0.2
    base = top + 0.018
    prism([(cx - w / 2 - 0.006, base), (cx + w / 2 + 0.006, base), (cx, base + ph)], y - 0.008, y + d + 0.012, m["stone"])
    prism([(cx - w / 2 + 0.012, base + 0.003), (cx + w / 2 - 0.012, base + 0.003), (cx, base + ph - 0.009)], y - 0.0095, y - 0.0075, m["stone2"], 0.0003)
    rl = math.hypot(w / 2 + 0.012, ph + 0.004)
    ang = math.atan2(ph + 0.004, w / 2 + 0.012)
    for side in (-1, 1):
        bpy.ops.mesh.primitive_cube_add(location=(cx + side * (w / 4 + 0.003), y + d / 2 + 0.002, base + ph / 2 + 0.003))
        o = bpy.context.object
        o.scale = (rl / 2, (d + 0.03) / 2, 0.002)
        o.rotation_euler = (0, side * ang, 0)
        finish(o, m["roof"], 0.0006)
    if stage >= 7:
        cyl(cx, y - 0.012, base + ph * 0.38, 0.011, 0.003, m["brass"], verts=32, rot=(math.radians(90), 0, 0))
        for fx in (cx - w / 2 - 0.006, cx + w / 2 + 0.006):
            cyl(fx, y - 0.004, base, 0.0008, 0.06, m["iron"], verts=8)
            box(fx + 0.009, y - 0.004, base + 0.046, 0.018, 0.0006, 0.011, m["red"], 0.0002)
        for bx in (cx - w * 0.3, cx + w * 0.3):
            box(bx, y + 0.016, floor + 0.035, 0.012, 0.0008, 0.045, m["red"], 0.0002)
    else:
        cyl(cx, y - 0.0095, base + ph * 0.36, 0.006, 0.002, m["stone"], verts=24, rot=(math.radians(90), 0, 0))
    return top, base + ph


def wing(m, x0, x1, y, h, seed=0):
    d = 0.13
    z0 = 0.003
    cx = (x0 + x1) / 2
    box(cx, y + d / 2, z0, x1 - x0, d, 0.008, m["stone2"], 0.0006)
    box(cx, y + d / 2, z0 + 0.008, x1 - x0, d, h, m["stone"], 0.0008)
    box(cx, y + d / 2, z0 + 0.008 + h, x1 - x0 + 0.006, d + 0.006, 0.004, m["stone2"], 0.0005)
    # balustrade
    for k in range(int((x1 - x0) / 0.006)):
        cyl(x0 + 0.003 + k * 0.006, y + 0.002, z0 + 0.012 + h, 0.0012, 0.008, m["stone"], verts=10, r2=0.0016)
    box(cx, y + 0.002, z0 + 0.02 + h, x1 - x0, 0.004, 0.002, m["stone"], 0.0003)
    n = max(2, int((x1 - x0) / 0.032))
    for k in range(n):
        wx = x0 + (x1 - x0) * (k + 0.5) / n
        arch_window(m, wx, y, z0 + 0.022, 0.014, 0.04)
        arch_window(m, wx, y, z0 + 0.022 + 0.048, 0.012, 0.022) if h > 0.09 else None


def dome(m, cx, y, base, r):
    cyl(cx, y, base, r * 0.92, r * 0.55, m["stone"], verts=48)
    for k in range(16):
        a = k / 16 * math.tau
        if math.sin(a) > 0.2:
            continue
        box(cx + math.cos(a) * r * 0.93, y + math.sin(a) * r * 0.93, base + r * 0.12, 0.004, 0.004, r * 0.3, m["glass"], 0.0003)
    cyl(cx, y, base + r * 0.55, r * 1.0, 0.004, m["stone2"], verts=48)
    bpy.ops.mesh.primitive_uv_sphere_add(segments=48, ring_count=24, radius=r * 0.9, location=(cx, y, base + r * 0.55 + 0.004))
    o = bpy.context.object
    bm = bmesh.new()
    bm.from_mesh(o.data)
    bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.z < -1e-5], context="VERTS")
    bm.to_mesh(o.data)
    bm.free()
    finish(o, m["copper"], smooth=True)
    for k in range(12):
        a = k / 12 * math.tau
        bpy.ops.mesh.primitive_torus_add(major_radius=r * 0.9, minor_radius=0.0009, location=(cx, y, base + r * 0.55 + 0.004), rotation=(math.radians(90), 0, a))
        o = bpy.context.object
        bm = bmesh.new()
        bm.from_mesh(o.data)
        bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.z < 0], context="VERTS")
        bm.to_mesh(o.data)
        bm.free()
        finish(o, mat("rib", "#4A7A66", 0.4, metal=0.3), smooth=True)
    top = base + r * 0.55 + 0.004 + r * 0.9
    cyl(cx, y, top - 0.002, 0.008, 0.016, m["stone"], verts=16)
    cyl(cx, y, top + 0.002, 0.0052, 0.009, m["glass"], verts=16)
    sphere(cx, y, top + 0.017, 0.009, m["copper"], sz=0.7)
    cyl(cx, y, top + 0.022, 0.0009, 0.014, m["brass"], verts=8)
    sphere(cx, y, top + 0.037, 0.0025, m["brass"])


def shop(m, cx, y):
    w, h, d = 0.17, 0.085, 0.11
    z0 = 0.003
    box(cx, y + d / 2, z0, w, d, h, m["stone"], 0.0008)
    box(cx, y + d / 2, z0 + h, w + 0.01, d + 0.012, 0.008, m["roof"], 0.001)
    box(cx, y - 0.001, z0 + h - 0.019, w * 0.8, 0.003, 0.013, m["sign"], 0.0005)
    box(cx, y - 0.0026, z0 + h - 0.016, w * 0.72, 0.001, 0.007, m["brass"], 0.0002)
    # the big window full of shelves
    wx = cx - 0.03
    box(wx, y - 0.0005, z0 + 0.012, 0.094, 0.002, 0.052, m["walnut2"], 0.0004)
    box(wx, y - 0.0012, z0 + 0.015, 0.086, 0.0015, 0.046, m["glass"], 0.0002)
    r = random.Random(3)
    for k in range(3):
        sz = z0 + 0.017 + k * 0.015
        box(wx, y + 0.004, sz, 0.084, 0.008, 0.0012, m["walnut"], 0)
        x = wx - 0.038
        while x < wx + 0.036:
            hh = r.uniform(0.005, 0.011)
            col = r.choice(["#C94A3A", "#D9A441", "#4F8C84", "#6FA87A", "#E9E1C8", "#2F4A6B"])
            cyl(x, y + 0.004, sz + 0.0012, 0.0022, hh, mat("thing" + col, col, 0.4, emit=col, emit_strength=1.5 if NIGHT else 0), verts=12)
            x += r.uniform(0.006, 0.01)
    door(m, cx + 0.058, y, 0.026, 0.06)
    # awning
    bpy.ops.mesh.primitive_cube_add(location=(wx, y - 0.012, z0 + 0.07))
    o = bpy.context.object
    o.scale = (0.05, 0.013, 0.0012)
    o.rotation_euler = (math.radians(-28), 0, 0)
    finish(o, m["awning"], 0.0004)


def townhouse(m, cx, y):
    w, h, d = 0.22, 0.13, 0.13
    z0 = 0.003
    box(cx, y + d / 2, z0, w, d, 0.008, m["stone2"], 0.0006)
    box(cx, y + d / 2, z0 + 0.008, w, d, h, m["stone"], 0.0008)
    box(cx, y + d / 2, z0 + 0.008 + h * 0.5, w + 0.004, d + 0.004, 0.003, m["stone2"], 0.0004)
    for k in range(3):
        arch_window(m, cx - 0.07 + k * 0.07, y, z0 + 0.008 + h * 0.58, 0.018, 0.04)
    for dx in (-0.07, 0.07):
        arch_window(m, cx + dx, y, z0 + 0.016, 0.018, 0.044)
    door(m, cx, y, 0.024, 0.055)
    top = z0 + 0.008 + h
    box(cx, y + d / 2, top, w + 0.01, d + 0.01, 0.005, m["stone2"], 0.0006)
    ph = 0.045
    prism([(cx - w / 2 - 0.004, top + 0.005), (cx + w / 2 + 0.004, top + 0.005), (cx, top + 0.005 + ph)], y - 0.006, y + d + 0.006, m["stone"])
    rl = math.hypot(w / 2 + 0.01, ph + 0.003)
    ang = math.atan2(ph + 0.003, w / 2 + 0.01)
    for side in (-1, 1):
        bpy.ops.mesh.primitive_cube_add(location=(cx + side * (w / 4 + 0.003), y + d / 2, top + 0.005 + ph / 2 + 0.002))
        o = bpy.context.object
        o.scale = (rl / 2, (d + 0.02) / 2, 0.002)
        o.rotation_euler = (0, side * ang, 0)
        finish(o, m["roof"], 0.0006)
    cyl(cx, y - 0.0065, top + 0.005 + ph * 0.36, 0.006, 0.002, m["stone2"], verts=24, rot=(math.radians(90), 0, 0))


# ---------------------------------------------------------------- the cabinet of curiosities

def can(x, y, z, r, h, col, band):
    cyl(x, y, z, r, h, mat("can" + col, col, 0.25, metal=0.3, coat=0.6), verts=32)
    cyl(x, y, z + h * 0.42, r * 1.004, h * 0.16, mat("band" + band, band, 0.3), verts=32)
    cyl(x, y, z + h, r * 0.92, r * 0.18, mat("lid", "#C9CDD2", 0.25, metal=1.0), verts=32, r2=r * 0.8)


def croc(x, y, z, s, col, belly):
    c = mat("croc" + col, col, 0.35, coat=0.7)
    b = mat("belly" + belly, belly, 0.5)
    sphere(x, y, z + 0.006 * s, 0.006 * s, c, sx=3.2, sy=1.0, sz=0.85)
    sphere(x + 0.022 * s, y, z + 0.0055 * s, 0.004 * s, c, sx=2.6, sy=0.9, sz=0.7)
    bpy.ops.mesh.primitive_cone_add(vertices=16, radius1=0.0045 * s, radius2=0.0004 * s, depth=0.03 * s,
                                    location=(x - 0.033 * s, y, z + 0.0048 * s), rotation=(0, math.radians(-94), 0))
    finish(bpy.context.object, c, smooth=True)
    sphere(x, y, z + 0.0035 * s, 0.0052 * s, b, sx=2.8, sy=0.95, sz=0.6)
    for k in range(8):
        sphere(x - 0.014 * s + k * 0.004 * s, y, z + 0.011 * s, 0.0012 * s, mat("scute" + col, col, 0.4), sz=1.4, seg=10)
    for dy in (-0.0028, 0.0028):
        sphere(x + 0.019 * s, y + dy * s, z + 0.0095 * s, 0.0016 * s, mat("eye", "#F7F1E3", 0.2), seg=12)
        sphere(x + 0.0198 * s, y + dy * s - 0.0001, z + 0.0099 * s, 0.0008 * s, mat("pupil", "#1A1A1A", 0.2), seg=10)
    for dx, dy in ((-0.01, -0.006), (-0.01, 0.006), (0.01, -0.006), (0.01, 0.006)):
        cyl(x + dx * s, y + dy * s, z, 0.0016 * s, 0.004 * s, c, verts=10)


def globe(x, y, z, r):
    cyl(x, y, z, r * 0.5, r * 0.12, mat("brass", "#C9A24E", 0.3, metal=1.0), verts=24)
    cyl(x, y, z + r * 0.12, r * 0.06, r * 0.5, mat("brass", "#C9A24E", 0.3, metal=1.0), verts=12)
    o = sphere(x, y, z + r * 1.5, r, mat("globe", "#4F8C84", 0.35, coat=0.6), seg=32)
    o.rotation_euler = (0, math.radians(23), 0)
    bpy.ops.mesh.primitive_torus_add(major_radius=r * 1.12, minor_radius=r * 0.03, location=(x, y, z + r * 1.5), rotation=(math.radians(90), math.radians(23), 0))
    finish(bpy.context.object, mat("brass", "#C9A24E", 0.3, metal=1.0), smooth=True)


def shell(x, y, z, s):
    for k in range(7):
        a = math.radians(-60 + k * 20)
        bpy.ops.mesh.primitive_cone_add(vertices=12, radius1=0.0028 * s, radius2=0.0009 * s, depth=0.02 * s,
                                        location=(x + math.sin(a) * 0.009 * s, y, z + math.cos(a) * 0.009 * s), rotation=(0, a, 0))
        finish(bpy.context.object, mat("shell", "#F1D9C4", 0.4, coat=0.5), smooth=True)


def curiosity_cabinet(m, cx, cy, z0, w=0.2, h=0.3, d=0.07):
    """An arched walnut cabinet, open at the front: a velvet back, side boards, an arched top, shelves of things."""
    r = w / 2
    t = 0.01                       # board thickness
    body = h - r                   # height of the straight part
    zb = z0 + 0.012
    box(cx, cy, z0, w + 0.014, d + 0.012, 0.012, m["walnut2"], 0.002)
    # the velvet back, arched
    pts = [(cx - r + t, zb), (cx + r - t, zb)] + [(cx + (r - t) * math.cos(a), zb + body + (r - t) * math.sin(a)) for a in [i / 24 * math.pi for i in range(25)]]
    prism(pts, cy + d / 2 - 0.006, cy + d / 2, m["velvet"], 0)
    # sides
    for sx in (-1, 1):
        box(cx + sx * (r - t / 2), cy, zb, t, d, body, m["walnut"], 0.0015)
    # the arch: short boards around a half circle
    n = 20
    for i in range(n):
        a0 = i / n * math.pi; a1 = (i + 1) / n * math.pi; am = (a0 + a1) / 2
        rr = r - t / 2
        bpy.ops.mesh.primitive_cube_add(location=(cx + rr * math.cos(am), cy, zb + body + rr * math.sin(am)))
        o = bpy.context.object
        o.scale = (t / 2, d / 2, rr * (a1 - a0) / 2 + 0.0004)
        o.rotation_euler = (0, -am, 0)
        finish(o, m["walnut"], 0.0008)
    # a moulding round the front edge of the arch
    bpy.ops.mesh.primitive_torus_add(major_radius=r + 0.002, minor_radius=0.004, location=(cx, cy - d / 2, zb + body), rotation=(math.radians(90), 0, 0))
    o = bpy.context.object
    bm = bmesh.new(); bm.from_mesh(o.data)
    bmesh.ops.delete(bm, geom=[v for v in bm.verts if v.co.y < -0.0001], context="VERTS")
    bm.to_mesh(o.data); bm.free()
    finish(o, m["walnut2"], smooth=True)
    # shelves
    shelves = [zb + 0.004, zb + body * 0.45, zb + body * 0.9]
    for zz in shelves:
        box(cx, cy, zz - 0.004, w - 2 * t, d - 0.008, 0.004, m["walnut"], 0.0006)
    sy = cy - 0.004
    globe(cx, sy, shelves[2], 0.024)
    for k, (col, band) in enumerate([("#C94A3A", "#E9CF8C"), ("#1F3A5F", "#F2C14E"), ("#E6B422", "#7A4B12"), ("#2A9D8F", "#E9C46A")]):
        can(cx - 0.066 + k * 0.025, sy, shelves[1], 0.0085, 0.036, col, band)
    shell(cx + 0.06, sy, shelves[1], 1.1)
    croc(cx - 0.032, sy, shelves[0], 1.35, "#5DAA68", "#F3E9C6")
    croc(cx + 0.05, sy - 0.01, shelves[0], 0.9, "#B5892E", "#E2C46B")
    for k, col in enumerate(["#8C3B2E", "#2F4A6B", "#6E6A3A"]):
        box(cx + 0.062, sy + 0.012, shelves[0] + k * 0.006, 0.03, 0.02, 0.006, mat("book" + col, col, 0.6), 0.0005)
    # a little lamp inside, under the arch
    if NIGHT:
        point_light(cx, cy - 0.01, zb + body + r * 0.6, 0.5, radius=0.01)
    sphere(cx, cy, zb + body + r + 0.01, 0.008, m["brass"])
    return shelves


def hallway(m):
    """Stage 0 and the cover: a corner of a hallway on the plinth, a cabinet of curiosities against the wall."""
    plinth(m, 0.5, 0.36)
    box(0, 0, 0.003, 0.49, 0.35, 0.002, m["floor"], 0.0005)
    box(0, 0.16, 0.003, 0.49, 0.012, 0.34, m["wallpaper"], 0.001)
    box(-0.24, 0, 0.003, 0.012, 0.33, 0.34, m["wallpaper"], 0.001)
    box(0, 0.152, 0.003, 0.49, 0.006, 0.03, m["walnut2"], 0.0005)
    box(-0.232, 0, 0.003, 0.006, 0.33, 0.03, m["walnut2"], 0.0005)
    box(0.02, -0.03, 0.005, 0.3, 0.2, 0.0015, m["rug"], 0.0005)
    curiosity_cabinet(m, 0.0, 0.12, 0.003)
    # a framed specimen on the wall and a stool with a magnifying glass
    for fx in (-0.17, 0.17):
        box(fx, 0.152, 0.17, 0.07, 0.006, 0.09, m["brass"], 0.001)
        box(fx, 0.149, 0.177, 0.058, 0.002, 0.076, mat("paper", "#EFE6D2", 0.8), 0)
        for k in range(4):
            sphere(fx - 0.016 + (k % 2) * 0.032, 0.147, 0.2 - (k // 2) * 0.034, 0.006, mat("bug" + str(k), ["#4F8C84", "#C94A3A", "#D9A441", "#6E6AA8"][k], 0.3, coat=0.8), sz=0.5, seg=12)
    box(0.17, -0.02, 0.003, 0.004, 0.004, 0.045, m["walnut2"])
    cyl(0.17, -0.02, 0.048, 0.022, 0.005, m["walnut"], verts=32)
    cyl(0.17, -0.02, 0.053, 0.012, 0.0015, m["brass"], verts=32)
    if NIGHT:
        point_light(0.0, 0.0, 0.33, 2.0, radius=0.03)
        point_light(0.0, 0.08, 0.2, 0.8, radius=0.02)
    else:
        point_light(0.0, 0.02, 0.33, 0.8, radius=0.04)


# ---------------------------------------------------------------- scenes

def grow(k, build):
    """Builds something and scales it about the plinth's centre, so the building can be sized to the plinth."""
    before = set(bpy.data.objects)
    build()
    pivot = bpy.data.objects.new("pivot", None)
    bpy.context.collection.objects.link(pivot)
    for o in set(bpy.data.objects) - before - {pivot}:
        if o.parent is None:
            o.parent = pivot
    pivot.scale = (k, k, k)


def fountain(m, x, y, s=1.0):
    water = mat("water", "#6FA3AE", 0.05, coat=1.0)
    cyl(x, y, 0.003, 0.04 * s, 0.008 * s, m["stone2"], verts=48, bevel=0.001)
    cyl(x, y, 0.0035, 0.035 * s, 0.0082 * s, water, verts=48)
    cyl(x, y, 0.011 * s, 0.006 * s, 0.025 * s, m["stone"], verts=24)
    cyl(x, y, 0.034 * s, 0.017 * s, 0.004 * s, m["stone2"], verts=32, r2=0.012 * s)
    cyl(x, y, 0.0365 * s, 0.013 * s, 0.0016 * s, water, verts=32)
    cyl(x, y, 0.038 * s, 0.0025 * s, 0.012 * s, m["stone"], verts=16)
    sphere(x, y, 0.052 * s, 0.003 * s, water)
    if NIGHT:
        point_light(x, y - 0.02, 0.01, 0.08, color="#BFE3FF", radius=0.01)


def light_rig(sc):
    if NIGHT:
        world("#1A2233", 0.55)
        sun((52, 0, 38), 0.9, "#9DB4FF", 6)
    else:
        world("#F4EBDC", 0.75)
        sun((48, 0, -35), 3.2, "#FFF0DA", 3)
        area((0.6, -0.8, 0.6), (60, 0, 35), 0.8, 18, "#DCE7FF")


def facade(stage):
    sc = reset()
    m = M()
    light_rig(sc)
    if stage == 0:
        hallway(m)
        camera((0.55, -1.4, 0.7), (0.0, 0.02, 0.11), lens=70, fstop=5.6)
        return
    plinth(m)
    # lawns along the back and the sides
    lawn(m, 0, 0.19, 0.6, 0.05, seed=1)
    lawn(m, -0.27, -0.06, 0.06, 0.14, seed=2)
    lawn(m, 0.27, -0.06, 0.06, 0.14, seed=3)
    keep = []
    y = -0.005
    if stage == 1:
        grow(1.3, lambda: shop(m, 0.0, y))
        tree(m, -0.2, 0.06, 1.2, 1)
        tree(m, 0.21, 0.08, 1.1, 2, cypress=True)
        lantern(m, 0.12, -0.03)
        bench(m, -0.13, -0.02)
        keep = [(0.058, 0.02, -0.05)]
        people = 4
    elif stage == 2:
        grow(1.3, lambda: townhouse(m, 0.0, y))
        tree(m, -0.21, 0.06, 1.25, 1)
        tree(m, 0.22, 0.08, 1.15, 2, cypress=True)
        for lx in (-0.14, 0.14):
            lantern(m, lx, -0.03)
        bench(m, -0.2, -0.1)
        keep = [(0.0, 0.025, -0.05)]
        people = 6
    else:
        w = {3: 0.2, 4: 0.2, 5: 0.22, 6: 0.22, 7: 0.23}[stage]
        h = 0.105
        cols = 6 if stage >= 5 else 4
        k = 1.32 if stage < 6 else 1.2

        def build():
            if stage >= 5:
                dome(m, 0.0, 0.1, 0.016 + h + 0.018, 0.05)
            if stage >= 4:
                wing(m, w / 2 + 0.002, w / 2 + 0.085, 0.03, 0.085)
            if stage >= 6:
                wing(m, -w / 2 - 0.085, -w / 2 - 0.002, 0.03, 0.085)
            temple(m, 0.0, -0.03, w, h, cols, stage)
            for lx in (-w / 2 - 0.02, w / 2 + 0.02):
                lantern(m, lx, -0.07)
        grow(k, build)
        fountain(m, -0.17 if stage < 6 else 0.0, -0.16 if stage < 6 else -0.175, 1.1)
        if stage < 6:
            tree(m, -0.24, 0.07, 1.25, 4)
        if stage < 4:
            tree(m, 0.24, 0.09, 1.15, 5, cypress=True)
        tree(m, -0.27, -0.12, 0.9, 6, cypress=True)
        tree(m, 0.27, -0.12, 0.9, 7, cypress=True)
        bench(m, 0.13, -0.17)
        bench(m, 0.2, -0.12)
        keep = [(0.0, (w / 2 + 0.03) * k, -0.1), (-0.17 if stage < 6 else 0.0, 0.05, -0.2)]
        people = 4 + stage * 2
    crowd(m, people, stage * 13, keep)
    camera((0.5, -1.55, 0.78), (0.0, 0.0, 0.03), lens=72, fstop=5.0)


def kind(key):
    """A walnut shadow box lined with velvet, things on its shelf under a small lamp."""
    sc = reset()
    sc.render.film_transparent = False
    sc.view_settings.look = "AgX - Punchy"
    m = M()
    world("#1E3127", 0.4)
    box(0, 0.09, -0.02, 0.36, 0.1, 0.24, m["velvet"], 0)
    box(0, 0.0, -0.02, 0.36, 0.12, 0.02, m["walnut"], 0.002)
    area((0.0, -0.05, 0.22), (20, 0, 0), 0.15, 6, "#FFE2B8")
    area((-0.3, -0.4, 0.1), (75, 0, -35), 0.4, 5, "#FFFFFF")
    z = 0.0
    if key == "cans":
        for k, (c, b) in enumerate([("#1F3A5F", "#F2C14E"), ("#C94A3A", "#F4EBDD"), ("#E6B422", "#7A4B12"), ("#2A9D8F", "#E9C46A"), ("#6A4C93", "#FFCA3A")]):
            can(-0.1 + k * 0.05, -0.01, z, 0.016, 0.062 if k % 2 == 0 else 0.07, c, b)
    elif key == "figures":
        croc(-0.04, -0.01, z, 2.2, "#5DAA68", "#F3E9C6")
        sphere(0.085, -0.005, z + 0.022, 0.02, mat("cat", "#E9DFC9", 0.3, coat=0.8), sz=1.1)
        sphere(0.085, -0.005, z + 0.05, 0.014, mat("cat", "#E9DFC9", 0.3, coat=0.8))
        for dx in (-0.007, 0.007):
            cyl(0.085 + dx, -0.005, z + 0.058, 0.004, 0.01, mat("cat", "#E9DFC9", 0.3, coat=0.8), verts=8, r2=0.0005)
    elif key == "magnets":
        box(0, 0.025, z, 0.22, 0.02, 0.13, mat("fridge", "#EDEAE4", 0.25, coat=0.6), 0.008)
        r = random.Random(4)
        for k in range(9):
            col = r.choice(["#C94A3A", "#4F8C84", "#D9A441", "#6E6AA8", "#2F4A6B", "#E07A5F", "#5DAA68"])
            x, zz = -0.07 + (k % 3) * 0.07 + r.uniform(-0.008, 0.008), z + 0.025 + (k // 3) * 0.038
            if k % 2:
                cyl(x, 0.009, zz, 0.014, 0.004, mat("mag" + col, col, 0.4, coat=0.5), verts=24, rot=(math.radians(90), 0, 0))
            else:
                box(x, 0.006, zz - 0.012, 0.03, 0.006, 0.024, mat("mag" + col, col, 0.4, coat=0.5), 0.002)
    elif key == "coins":
        for k in range(10):
            cyl(-0.06, -0.01, z + k * 0.004, 0.022, 0.0037, m["brass"], verts=48, bevel=0.0005)
        o = cyl(0.05, -0.01, z + 0.045, 0.045, 0.006, mat("silver", "#D0D0D0", 0.25, metal=1.0), verts=64, rot=(math.radians(80), 0, 0), bevel=0.001)
    elif key == "paper":
        bpy.ops.mesh.primitive_cube_add(location=(-0.05, 0.0, z + 0.05))
        o = bpy.context.object
        o.scale = (0.07, 0.001, 0.05)
        o.rotation_euler = (math.radians(-12), 0, 0)
        finish(o, mat("postcard", "#F3EBDD", 0.7), 0.0005)
        for col, zz, hh in (("#8FC1D6", 0.07, 0.05), ("#5E8C62", 0.035, 0.03)):
            bpy.ops.mesh.primitive_cube_add(location=(-0.065, -0.0035, z + zz))
            o = bpy.context.object
            o.scale = (0.045, 0.0005, hh / 2)
            o.rotation_euler = (math.radians(-12), 0, 0)
            finish(o, mat("pc" + col, col, 0.7), 0)
        for k, col in enumerate(["#C94A3A", "#2F4A6B", "#D9A441"]):
            bpy.ops.mesh.primitive_cube_add(location=(0.06 + (k % 2) * 0.035, -0.012, z + 0.03 + k * 0.035))
            o = bpy.context.object
            o.scale = (0.016, 0.0006, 0.02)
            o.rotation_euler = (math.radians(-10), math.radians(-8 + k * 9), 0)
            finish(o, mat("stamp" + col, col, 0.6), 0.0003)
    elif key == "records":
        box(-0.05, 0.005, z, 0.13, 0.006, 0.13, mat("sleeve", "#C94A3A", 0.6), 0.001)
        cyl(0.02, -0.004, z + 0.065, 0.062, 0.002, mat("vinyl", "#1A1A1A", 0.38, coat=0.25), verts=64, rot=(math.radians(90), 0, 0))
        cyl(0.02, -0.006, z + 0.065, 0.02, 0.0005, mat("label", "#E9C46A", 0.5), verts=48, rot=(math.radians(90), 0, 0))
        box(0.11, -0.02, z, 0.06, 0.012, 0.04, mat("tape", "#2F2F33", 0.4), 0.002)
    elif key == "cars":
        for x, col, s in ((-0.06, "#C94A3A", 1.2), (0.08, "#2F6B9E", 0.9)):
            box(x, -0.01, z + 0.008 * s, 0.09 * s, 0.04 * s, 0.018 * s, mat("car" + col, col, 0.2, coat=1.0), 0.006 * s)
            box(x - 0.005 * s, -0.01, z + 0.024 * s, 0.05 * s, 0.034 * s, 0.016 * s, mat("cabin", "#9FB8BD", 0.05, coat=1.0), 0.005 * s)
            for dx in (-0.03, 0.03):
                for dy in (-0.02, 0.02):
                    cyl(x + dx * s, -0.01 + dy * s, z + 0.009 * s, 0.009 * s, 0.006 * s, mat("tyre", "#1E1E1E", 0.7), verts=24, rot=(math.radians(90), 0, 0))
    else:
        box(-0.02, 0.0, z, 0.12, 0.07, 0.06, m["walnut2"], 0.003)
        bpy.ops.mesh.primitive_cylinder_add(vertices=48, radius=0.035, depth=0.12, location=(-0.02, 0.0, z + 0.06), rotation=(0, math.radians(90), 0))
        finish(bpy.context.object, m["walnut2"], 0.002)
        for dx in (-0.05, -0.02, 0.01):
            box(dx, 0.0, z, 0.006, 0.072, 0.062, m["brass"], 0.0005)
        cyl(0.1, -0.02, z + 0.004, 0.009, 0.003, m["brass"], verts=24, rot=(math.radians(90), 0, 0))
        box(0.12, -0.02, z + 0.003, 0.03, 0.003, 0.003, m["brass"], 0.0004)
    camera((0.0, -0.52, 0.17), (0.0, 0.0, 0.035), lens=62, fstop=4.0, res=(960, 600))


def icon():
    sc = reset()
    m = M()
    world("#F4EBDC", 0.6)
    sun((50, 0, -30), 3.0, "#FFF0DA", 3)
    point_light(0.0, -0.1, 0.33, 1.2, radius=0.04)
    curiosity_cabinet(m, 0.0, 0.0, 0.0)
    shadow_floor(0.0)
    camera((0.0, -1.3, 0.2), (0.0, 0.0, 0.15), lens=110, fstop=8, res=(864, 864))


def run():
    global NIGHT
    job = ARGS[0]
    sc = bpy.context.scene
    if job == "facade":
        stage = int(ARGS[1])
        NIGHT = ARGS[2] == "night"
        facade(stage)
        name = f"facade_{stage}_{ARGS[2]}.png"
    elif job == "kind":
        kind(ARGS[1])
        name = f"kind_{ARGS[1]}.png"
    else:
        icon()
        name = "icon.png"
    sc = bpy.context.scene
    if len(ARGS) > 3:
        sc.cycles.samples = int(ARGS[3])
    sc.render.filepath = os.path.join(OUT, name)
    bpy.ops.render.render(write_still=True)


run()
