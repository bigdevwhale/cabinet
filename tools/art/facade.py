"""Paints the museum (eight stages, day and night), the cover, the visitors and the app icon.

python tools/art/facade.py

Drawn with PIL and numpy in a three-quarter view: every block has a front, a top and a shaded right side,
projections cast soft shadows, the plaza recedes to the horizon with hedges, benches and lamps.
The day picture is painted first; night is made from it (darkened, cooled, moonlit) with everything that glows
added as light and bloom. Finished with a vignette and a little grain.
Writes app/src/main/res/drawable-nodpi/facade_<stage>_<day|night>.webp, visitor_<n>.png and the icon layers.
"""
from pathlib import Path
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "app/src/main/res/drawable-nodpi"
W, H = 1080, 780  # with room in the sky for the museum's name over it
SS = 3
HORIZON = 470
GROUND = 588          # the line where the building's front meets the plaza
DX, DY = 0.42, -0.30  # three-quarter depth: a point `d` deep moves right and up


def S(v):
    return int(round(v * SS))


def rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


STONE = rgb("#EADFC9")
STONE_LIT = rgb("#F7F0E1")
STONE_SIDE = rgb("#C7B796")
STONE_TOP = rgb("#F2E8D4")
ROOF = rgb("#8E6A4B")
ROOF_TOP = rgb("#A47E5B")
COPPER = rgb("#6FA088")
GLASS = rgb("#7E9898")
WOOD = rgb("#5A3820")
WARM = (255, 192, 104)
INK = rgb("#2A2420")


class Scene:
    def __init__(self):
        self.a = Image.new("RGB", (S(W), S(H)))
        self.e = Image.new("RGB", (S(W), S(H)))
        self.sh = Image.new("L", (S(W), S(H)), 0)  # shadows, multiplied in later and softened
        self.d = ImageDraw.Draw(self.a)
        self.de = ImageDraw.Draw(self.e)
        self.ds = ImageDraw.Draw(self.sh)
        self.lamps = []
        self.street = True

    def poly(self, pts, c, light=None):
        p = [(S(x), S(y)) for x, y in pts]
        self.d.polygon(p, fill=c)
        # what is drawn in front hides the light behind it
        self.de.polygon(p, fill=light or (0, 0, 0))

    def rect(self, x0, y0, x1, y1, c, light=None):
        self.poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], c, light)

    def ellipse(self, x0, y0, x1, y1, c, light=None):
        self.d.ellipse([S(x0), S(y0), S(x1), S(y1)], fill=c)
        self.de.ellipse([S(x0), S(y0), S(x1), S(y1)], fill=light or (0, 0, 0))

    def shadow(self, pts, k=110):
        self.ds.polygon([(S(x), S(y)) for x, y in pts], fill=k)

    def shadow_ellipse(self, x0, y0, x1, y1, k=110):
        self.ds.ellipse([S(x0), S(y0), S(x1), S(y1)], fill=k)

    def vgrad(self, x0, y0, x1, y1, c0, c1, light=False):
        h = max(1, S(y1) - S(y0))
        dr = self.de if light else self.d
        for i in range(h):
            dr.line([(S(x0), S(y0) + i), (S(x1) - 1, S(y0) + i)], fill=mix(c0, c1, i / h))

    def box(self, x0, top, x1, bottom, depth, front, side=None, roof=None, cast=True):
        """A block in three-quarter view: front face, top, and the right side in shade."""
        dx, dy = depth * DX, depth * DY
        self.poly([(x1, top), (x1 + dx, top + dy), (x1 + dx, bottom + dy), (x1, bottom)], side or shade(front, 0.78))
        self.poly([(x0, top), (x0 + dx, top + dy), (x1 + dx, top + dy), (x1, top)], roof or shade(front, 1.06))
        self.rect(x0, top, x1, bottom, front)
        if cast and depth > 20:
            self.shadow([(x1, bottom), (x1 + dx, bottom + dy), (x1 + dx + depth * 0.5, bottom + dy + 6), (x1 + 30, bottom + 8)], 70)

    def ledge(self, x0, y, x1, depth, h=6, overhang=6):
        """A cornice: a projecting stone strip with a soft shadow under it."""
        self.box(x0 - overhang, y, x1 + overhang, y + h, depth + overhang, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)
        self.shadow([(x0, y + h), (x1, y + h), (x1, y + h + 9), (x0, y + h + 9)], 90)

    def column(self, cx, top, bottom, w, depth=10):
        """A fluted column, lit from the left, casting a shadow on the wall behind it."""
        self.shadow([(cx + w * 0.3, top + 8), (cx + w * 1.4, top + 4), (cx + w * 1.4, bottom - 6), (cx + w * 0.3, bottom - 2)], 95)
        x0 = S(cx - w / 2); x1 = S(cx + w / 2)
        for i in range(x1 - x0):
            t = i / max(1, x1 - x0 - 1)
            k = 0.70 + 0.42 * math.cos((t - 0.3) * math.pi * 0.9)
            self.d.line([(x0 + i, S(top)), (x0 + i, S(bottom))], fill=shade(STONE_LIT, min(1.04, max(0.62, k))))
        for f in range(1, 7):
            x = x0 + (x1 - x0) * f / 7
            self.d.line([(x, S(top) + S(6)), (x, S(bottom) - S(6))], fill=shade(STONE, 0.84 if f > 2 else 0.92), width=SS)
        self.box(cx - w / 2 - 6, top - 7, cx + w / 2 + 6, top, depth, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)
        self.poly([(cx - w / 2 - 3, top), (cx + w / 2 + 3, top), (cx + w / 2, top + 6), (cx - w / 2, top + 6)], shade(STONE, 0.92))
        self.box(cx - w / 2 - 5, bottom, cx + w / 2 + 5, bottom + 8, depth, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)
        self.vgrad(cx - w / 2, top, cx + w / 2, bottom, (25, 18, 8), (150, 108, 58), light=True)

    def window(self, x, y, w, h, seed=0, lit=True):
        """A deep arched window: frame, inner shadow, glass with sky by day and a lit room by night."""
        r = w / 2
        self.ellipse(x - 6, y - 6, x + w + 6, y + w + 6, STONE_LIT)
        self.rect(x - 6, y + r, x + w + 6, y + h + 2, STONE_LIT)
        self.ellipse(x, y, x + w, y + w, shade(GLASS, 0.7), WARM if lit else None)
        self.rect(x, y + r, x + w, y + h, shade(GLASS, 0.7), WARM if lit else None)
        self.vgrad(x + 3, y + r, x + w, y + h, rgb("#A9C0C2"), rgb("#6E8888"))
        self.poly([(x + w * 0.55, y + r), (x + w * 0.8, y + r), (x + w * 0.35, y + h), (x + w * 0.1, y + h)], rgb("#B8CDCD"), WARM)
        if lit:
            self.vgrad(x, y + r * 0.5, x + w, y + h, (255, 210, 130), (215, 140, 62), light=True)
            r2 = np.random.default_rng(seed)
            self.de.rectangle([S(x), S(y + r * 0.6), S(x + w * 0.22), S(y + h)], fill=(170, 70, 50))
            px = x + w * r2.uniform(0.45, 0.7)
            self.de.rectangle([S(px - 5), S(y + h - 16), S(px + 5), S(y + h)], fill=(120, 80, 40))
            self.de.ellipse([S(px - 6), S(y + h - 28), S(px + 6), S(y + h - 16)], fill=(90, 120, 110))
        self.shadow([(x, y + r), (x + 5, y + r), (x + 5, y + h), (x, y + h)], 120)
        self.rect(x + w / 2 - 1.3, y + 3, x + w / 2 + 1.3, y + h, STONE_LIT)
        self.rect(x, y + r + (h - r) * 0.42, x + w, y + r + (h - r) * 0.42 + 2.6, STONE_LIT)
        self.box(x - 9, y + h + 2, x + w + 9, y + h + 8, 8, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)

    def lantern(self, x, base, h=96):
        self.shadow_ellipse(x - 16, base - 4, x + 26, base + 5, 90)
        self.rect(x - 2.5, base - h, x + 2.5, base, INK)
        self.rect(x - 7, base - 8, x + 7, base, INK)
        self.poly([(x - 9, base - h), (x + 9, base - h), (x + 7, base - h - 22), (x - 7, base - h - 22)], rgb("#E8E1CC"), WARM)
        self.poly([(x - 12, base - h - 22), (x + 12, base - h - 22), (x, base - h - 34)], INK)
        self.rect(x - 9, base - h - 1, x + 9, base - h + 2, INK)
        self.lamps.append((x, base - h - 11, base))

    def tree(self, x, base, s=1.0, seed=0, kind="round"):
        r = np.random.default_rng(seed)
        self.shadow_ellipse(x - 30 * s, base - 7 * s, x + 70 * s, base + 9 * s, 100)
        self.rect(x - 4 * s, base - 64 * s, x + 4 * s, base, rgb("#5C4430"))
        self.rect(x, base - 64 * s, x + 4 * s, base, rgb("#463222"))
        if kind == "cypress":
            pts = []
            for k in range(40):
                t = k / 39 * math.pi * 2
                rr = 1 + 0.06 * math.sin(t * 7)
                pts.append((x + math.sin(t) * 22 * s * rr, base - 130 * s - math.cos(t) * 80 * s * rr))
            self.poly(pts, rgb("#4E7A57"))
            for _ in range(180):
                a = r.uniform(0, 2 * math.pi); d = math.sqrt(r.uniform(0, 1))
                px = x + math.sin(a) * 20 * s * d; py = base - 130 * s - math.cos(a) * 76 * s * d
                c = rgb("#86B486") if px < x - 6 * s else (rgb("#3B6245") if px > x + 7 * s else rgb("#5E8E66"))
                q = r.uniform(3, 6) * s
                self.ellipse(px - q, py - q, px + q, py + q, c)
            return
        centres = [(0, -112, 44), (-30, -92, 34), (30, -94, 36), (-14, -140, 30), (18, -132, 30), (0, -80, 34)]
        for dx, dy, rr in centres:
            self.ellipse(x + (dx - rr) * s, base + (dy - rr) * s, x + (dx + rr) * s, base + (dy + rr) * s, rgb("#4F7A55"))
        for _ in range(420):
            dx, dy, rr = centres[r.integers(len(centres))]
            a = r.uniform(0, 2 * math.pi); d = math.sqrt(r.uniform(0, 1)) * rr
            px = x + (dx + math.cos(a) * d) * s; py = base + (dy + math.sin(a) * d) * s
            light = (-(px - x) / (60 * s) - (py - (base - 112 * s)) / (60 * s))
            c = mix(rgb("#3F6747"), rgb("#9CC496"), max(0, min(1, 0.45 + light * 0.6)))
            q = r.uniform(3, 7) * s
            self.ellipse(px - q, py - q, px + q, py + q, c)

    def hedge(self, x0, x1, base, h=26, depth=26):
        self.box(x0, base - h, x1, base, depth, rgb("#5E8C62"), rgb("#456E4C"), rgb("#79A777"))
        r = np.random.default_rng(int(x0) % 1000)
        for _ in range(int((x1 - x0) / 2)):
            px = r.uniform(x0, x1 + depth * DX); py = base - h + r.uniform(depth * DY - 2, 4)
            q = r.uniform(3, 6)
            self.ellipse(px - q, py - q, px + q, py + q, rgb("#86B482") if r.random() < 0.6 else rgb("#6A9A6A"))

    def bench(self, x, base):
        self.shadow([(x, base), (x + 60, base), (x + 74, base + 8), (x + 14, base + 8)], 80)
        for lx in (x + 4, x + 52):
            self.rect(lx, base - 15, lx + 4, base, INK)
        self.box(x, base - 20, x + 60, base - 15, 14, rgb("#8A5A36"), rgb("#6A4428"), rgb("#A06C42"), cast=False)
        self.box(x, base - 36, x + 60, base - 30, 3, rgb("#8A5A36"), rgb("#6A4428"), rgb("#A06C42"), cast=False)
        self.rect(x + 4, base - 30, x + 7, base - 20, INK); self.rect(x + 53, base - 30, x + 56, base - 20, INK)


# ---------- the sky, the town, the plaza ----------

def sky(sc, seed):
    r = np.random.default_rng(seed)
    sc.vgrad(0, 0, W, HORIZON + 60, rgb("#9EC0D2"), rgb("#F4DDB8"))
    for _ in range(6):
        cx, cy, s = r.uniform(-50, W), r.uniform(150, 330), r.uniform(0.7, 1.3)
        parts = [(r.uniform(-60, 60) * s, r.uniform(-12, 8) * s, r.uniform(22, 40) * s) for _ in range(7)]
        for dx, dy, rr in parts:
            sc.ellipse(cx + dx - rr * 1.3, cy + dy - rr * 0.7 + 5, cx + dx + rr * 1.3, cy + dy + rr * 0.7 + 5, rgb("#D8C6BA"))
        for dx, dy, rr in parts:
            sc.ellipse(cx + dx - rr * 1.25, cy + dy - rr * 0.75, cx + dx + rr * 1.25, cy + dy + rr * 0.55, rgb("#FBF4EA"))
    for _ in range(6):
        bx, by = r.uniform(620, 1000), r.uniform(200, 300)
        sc.d.line([(S(bx - 7), S(by - 3)), (S(bx), S(by)), (S(bx + 7), S(by - 3))], fill=rgb("#5A5048"), width=SS * 2)


def town(sc, seed):
    r = np.random.default_rng(seed)
    for col, top, hmin, hmax in [(rgb("#CEC7BC"), HORIZON + 34, 50, 120), (rgb("#BDB5A8"), HORIZON + 56, 30, 90)]:
        x = -20
        while x < W:
            w = r.uniform(40, 100); h = r.uniform(hmin, hmax)
            sc.rect(x, top - h, x + w, top + 40, col)
            if r.random() < 0.4:
                sc.poly([(x - 4, top - h), (x + w / 2, top - h - r.uniform(14, 30)), (x + w + 4, top - h)], shade(col, 0.95))
            if r.random() < 0.25:
                sc.rect(x + w * 0.4, top - h - 30, x + w * 0.5, top - h, col)
            for wy in range(int(top - h + 10), int(top + 20), 16):
                for wx in range(int(x + 8), int(x + w - 8), 14):
                    if r.random() < 0.2:
                        sc.rect(wx, wy, wx + 5, wy + 7, shade(col, 0.92), (210, 160, 90) if r.random() < 0.6 else None)
            x += w + r.uniform(-6, 4)


def plaza(sc):
    top = HORIZON + 66
    sc.vgrad(0, top, W, H, rgb("#D6C8AE"), rgb("#B4A386"))
    vx, vy = W / 2, HORIZON - 40
    for i in range(-30, 31):
        x1 = W / 2 + i * 70
        t0 = (top - vy) / (H - vy)
        x0 = vx + (x1 - vx) * t0
        sc.d.line([(S(x0), S(top)), (S(x1), S(H))], fill=rgb("#BBAB8F"), width=SS)
    y = top; step = 6
    while y < H:
        sc.d.line([(0, S(y)), (S(W), S(y))], fill=rgb("#BEAE92"), width=SS)
        step *= 1.22; y += step


# ---------- buildings ----------

def temple(sc, cx, w, h, cols, stage):
    x0, x1 = cx - w / 2, cx + w / 2
    top = GROUND - h
    depth = 120
    ph = w * 0.19
    dx, dy = depth * DX, depth * DY
    sc.poly([(cx, top - ph), (cx + dx, top - ph + dy), (x1 + 16 + dx, top + 8 + dy), (x1 + 16, top + 8)], ROOF_TOP)
    sc.box(x0 + 14, top + 34, x1 - 14, GROUND, depth, STONE, STONE_SIDE, STONE_TOP)
    for y in range(int(top + 52), GROUND, 18):
        sc.rect(x0 + 14, y, x1 - 14, y + 1, shade(STONE, 0.9))
    gap = (w - 44) / (cols - 1)
    for k in range(cols - 1):
        mx = x0 + 22 + gap * (k + 0.5)
        if abs(mx - cx) < gap * 0.6:
            continue
        sc.window(mx - 17, top + 84, 34, 74, seed=k + stage)
    dw, dh = 54, 118
    sc.rect(cx - dw / 2 - 10, GROUND - dh - 10, cx + dw / 2 + 10, GROUND, STONE_LIT)
    sc.ellipse(cx - dw / 2 - 10, GROUND - dh - 10 - dw / 2 - 10, cx + dw / 2 + 10, GROUND - dh + dw / 2 + 10, STONE_LIT)
    sc.ellipse(cx - dw / 2, GROUND - dh - dw / 2, cx + dw / 2, GROUND - dh + dw / 2, WOOD, WARM)
    sc.rect(cx - dw / 2, GROUND - dh, cx + dw / 2, GROUND, WOOD, WARM)
    sc.vgrad(cx - dw / 2, GROUND - dh, cx + dw / 2, GROUND, (255, 216, 150), (255, 165, 72), light=True)
    sc.rect(cx - 1.5, GROUND - dh, cx + 1.5, GROUND, rgb("#3A2412"))
    sc.lamps.append((cx, GROUND - 40, GROUND + 30))
    for k in range(5):
        y = GROUND - 26 + k * 6
        ext = k * 9
        sc.box(x0 - 6 - ext, y, x1 + 6 + ext, y + 6, 24, STONE_LIT if k % 2 == 0 else STONE, STONE_SIDE, STONE_TOP, cast=False)
    cw = min(30, gap * 0.36)
    for k in range(cols):
        sc.column(x0 + 22 + gap * k, top + 46, GROUND - 26, cw)
    sc.box(x0 - 4, top + 14, x1 + 4, top + 40, depth, STONE, STONE_SIDE, STONE_TOP, cast=False)
    sc.shadow([(x0, top + 40), (x1, top + 40), (x1, top + 52), (x0, top + 52)], 100)
    x = x0 - 2
    while x < x1:
        sc.rect(x, top + 16, x + 6, top + 23, shade(STONE, 0.86))
        x += 11
    sc.ledge(x0 - 6, top + 6, x1 + 6, depth, 8, 8)
    sc.poly([(x0 - 18, top + 7), (cx, top - ph), (x1 + 18, top + 7)], STONE_LIT)
    sc.poly([(x0 + 16, top + 2), (cx, top - ph + 18), (x1 - 16, top + 2)], shade(STONE, 0.86))
    sc.shadow([(x0 + 16, top + 2), (cx, top - ph + 18), (cx + 30, top - ph + 30), (x0 + 40, top + 2)], 70)
    sc.poly([(x0 - 22, top + 8), (cx, top - ph - 6), (x1 + 22, top + 8), (x1 + 22, top + 12), (cx, top - ph - 2), (x0 - 22, top + 12)], shade(STONE_LIT, 0.95))
    if stage >= 7:
        r = ph * 0.3; cy = top - ph * 0.38
        sc.ellipse(cx - r - 3, cy - r - 3, cx + r + 3, cy + r + 3, shade(rgb("#C9A24E"), 0.7))
        sc.ellipse(cx - r, cy - r, cx + r, cy + r, rgb("#C9A24E"))
        sc.ellipse(cx - r * 0.72, cy - r * 0.72, cx + r * 0.72, cy + r * 0.72, rgb("#9F7A2C"))
        sc.poly([(cx - r * 0.4, cy + r * 0.25), (cx, cy - r * 0.5), (cx + r * 0.4, cy + r * 0.25)], rgb("#EBD08A"))
        for side in (-1, 1):
            for k in range(5):
                a = math.pi / 2 + side * (0.4 + k * 0.32)
                lx, ly = cx + math.cos(a) * r * 1.5, cy + math.sin(a) * r * 1.2
                sc.ellipse(lx - 5, ly - 3, lx + 5, ly + 3, rgb("#B89446"))
    else:
        r = ph * 0.16; cy = top - ph * 0.4
        sc.ellipse(cx - r, cy - r, cx + r, cy + r, STONE_LIT)
        sc.ellipse(cx - r * 0.6, cy - r * 0.6, cx + r * 0.6, cy + r * 0.6, shade(STONE, 0.9))


def wing(sc, x0, x1, h, depth=110, seed=0):
    top = GROUND - h
    sc.box(x0, top, x1, GROUND, depth, STONE, STONE_SIDE, STONE_TOP)
    for y in range(int(top + 26), GROUND, 16):
        sc.rect(x0, y, x1, y + 1, shade(STONE, 0.9))
    sc.box(x0, GROUND - 16, x1, GROUND, 4, shade(STONE, 0.9), STONE_SIDE, STONE_TOP, cast=False)
    sc.ledge(x0, top - 4, x1, depth, 8, 6)
    sc.box(x0 - 2, top - 30, x1 + 2, top - 25, depth, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)
    x = x0 + 4
    while x < x1 - 4:
        sc.ellipse(x, top - 25, x + 8, top - 6, shade(STONE, 0.95))
        sc.ellipse(x + 4, top - 22, x + 8, top - 9, STONE_SIDE)
        x += 12
    n = max(2, int((x1 - x0) / 64))
    for k in range(n):
        sc.window(x0 + (x1 - x0) * (k + 0.5) / n - 16, top + 32, 32, 70, seed=seed + k)


def dome(sc, cx, base, r):
    sc.box(cx - r * 0.85, base - r * 0.62, cx + r * 0.85, base, 30, STONE, STONE_SIDE, STONE_TOP, cast=False)
    for k in range(7):
        x = cx - r * 0.74 + k * r * 1.48 / 6
        sc.rect(x - 3, base - r * 0.56, x + 3, base - 4, STONE_LIT)
    for k in range(6):
        x = cx - r * 0.62 + k * r * 1.24 / 5 + r * 0.03
        sc.ellipse(x - 6, base - r * 0.48, x + 6, base - r * 0.36, shade(GLASS, 0.6), WARM)
        sc.rect(x - 6, base - r * 0.42, x + 6, base - r * 0.16, shade(GLASS, 0.6), WARM)
    sc.ledge(cx - r * 0.88, base - r * 0.66, cx + r * 0.88, 30, 6, 6)
    top = base - r * 0.66 - 6
    R = S(r)
    yy, xx = np.mgrid[-R:1, -R:R + 1]
    inside = xx ** 2 + yy ** 2 <= R ** 2
    nz = np.sqrt(np.clip(1 - (xx ** 2 + yy ** 2) / R ** 2, 0, 1))
    nx, ny = xx / R, yy / R
    lam = np.clip(nx * -0.55 + ny * -0.45 + nz * 0.7, 0, 1)
    spec = lam ** 18
    col = np.array(COPPER, dtype=np.float32) * (0.42 + 0.75 * lam[..., None]) + 255 * 0.35 * spec[..., None]
    ribs = (np.abs(np.sin(np.arctan2(xx, -yy + 1e-6) * 6)) < 0.05) & (yy < -2)
    col[ribs] *= 0.76
    img = Image.fromarray(np.clip(col, 0, 255).astype(np.uint8))
    sc.a.paste(img, (S(cx) - R, S(top) - R), Image.fromarray((inside * 255).astype(np.uint8)))
    sc.box(cx - 11, top - r - 30, cx + 11, top - r + 2, 10, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)
    sc.rect(cx - 5, top - r - 24, cx + 5, top - r - 4, shade(GLASS, 0.6), WARM)
    sc.ellipse(cx - 13, top - r - 44, cx + 13, top - r - 22, COPPER)
    sc.rect(cx - 1.5, top - r - 64, cx + 1.5, top - r - 40, rgb("#C9A24E"))
    sc.ellipse(cx - 5, top - r - 72, cx + 5, top - r - 62, rgb("#EBD08A"))


def room(sc, cx):
    w, h = 300, 200
    x0, top = cx - w / 2, GROUND - h
    sc.box(x0, top, x0 + w, GROUND, 90, STONE, STONE_SIDE, STONE_TOP)
    for y in range(int(top + 40), GROUND, 16):
        sc.rect(x0, y, x0 + w, y + 1, shade(STONE, 0.9))
    sc.box(x0 - 10, top - 16, x0 + w + 10, top + 6, 100, ROOF, shade(ROOF, 0.8), ROOF_TOP, cast=False)
    sc.box(x0 + 30, top + 14, x0 + w - 30, top + 40, 4, rgb("#2F4638"), cast=False)
    sc.rect(x0 + 36, top + 20, x0 + w - 36, top + 34, rgb("#C9A24E"))
    wx0, wy0, wx1, wy1 = x0 + 24, top + 70, x0 + 182, GROUND - 24
    sc.rect(wx0 - 7, wy0 - 7, wx1 + 7, wy1 + 7, WOOD)
    sc.rect(wx0, wy0, wx1, wy1, rgb("#3E5A49"), (225, 165, 90))
    r = np.random.default_rng(3)
    for k in range(3):
        y = wy0 + (wy1 - wy0) * (k + 1) / 3
        sc.rect(wx0, y - 3, wx1, y + 2, rgb("#8A5A36"), (130, 85, 40))
        x = wx0 + 8
        while x < wx1 - 16:
            hh = r.uniform(14, 28); ww = r.uniform(8, 16)
            col = [rgb("#C94A3A"), rgb("#D9A441"), rgb("#4F8C84"), rgb("#6FA87A"), rgb("#E9E1C8")][r.integers(5)]
            sc.rect(x, y - 3 - hh, x + ww, y - 3, col, shade(col, 0.75))
            x += ww + r.uniform(5, 12)
    sc.poly([(wx0 + 20, wy0), (wx0 + 46, wy0), (wx0 + 14, wy1), (wx0, wy1)], rgb("#5B7868"))
    sc.rect(x0 + 204, top + 80, x0 + 268, GROUND, WOOD, WARM)
    sc.rect(x0 + 212, top + 90, x0 + 260, top + 150, shade(GLASS, 0.6), WARM)
    sc.ellipse(x0 + 250, top + 164, x0 + 256, top + 170, rgb("#E3C77A"))
    for k in range(8):
        c = rgb("#B5473A") if k % 2 == 0 else rgb("#F3EBDD")
        ax = x0 + 18 + k * 21
        sc.poly([(ax, top + 50), (ax + 21, top + 50), (ax + 25, top + 66), (ax + 4, top + 66)], c)
    sc.shadow([(x0 + 22, top + 66), (x0 + 192, top + 66), (x0 + 192, top + 82), (x0 + 22, top + 82)], 90)
    sc.box(x0 - 4, GROUND - 3, x0 + w + 4, GROUND + 3, 90, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)


def townhouse(sc, cx):
    w, h = 360, 262
    x0, top = cx - w / 2, GROUND - h
    dpt = 110
    sc.poly([(cx, top - 66), (cx + dpt * DX, top - 66 + dpt * DY), (x0 + w + 14 + dpt * DX, top - 6 + dpt * DY), (x0 + w + 14, top - 6)], ROOF_TOP)
    sc.box(x0, top, x0 + w, GROUND, dpt, STONE, STONE_SIDE, STONE_TOP)
    for y in range(int(top + 20), GROUND, 18):
        sc.rect(x0, y, x0 + w, y + 1, shade(STONE, 0.9))
    sc.ledge(x0, top + 122, x0 + w, dpt, 6, 6)
    for k in range(3):
        sc.window(x0 + 50 + k * 110, top + 36, 40, 66, seed=k)
    sc.window(x0 + 50, top + 156, 40, 78, seed=5)
    sc.window(x0 + 270, top + 156, 40, 78, seed=6)
    dw = 56
    sc.ellipse(cx - dw / 2, GROUND - 100 - dw / 2, cx + dw / 2, GROUND - 100 + dw / 2, WOOD, WARM)
    sc.rect(cx - dw / 2, GROUND - 100, cx + dw / 2, GROUND, WOOD, WARM)
    sc.vgrad(cx - dw / 2, GROUND - 100, cx + dw / 2, GROUND, (255, 216, 150), (255, 165, 72), light=True)
    sc.ledge(x0 - 6, top - 8, x0 + w + 6, dpt, 8, 8)
    sc.poly([(x0 - 14, top - 8), (cx, top - 72), (x0 + w + 14, top - 8)], STONE_LIT)
    sc.poly([(x0 + 14, top - 12), (cx, top - 60), (x0 + w - 14, top - 12)], shade(STONE, 0.86))
    sc.ellipse(cx - 13, top - 46, cx + 13, top - 20, STONE_LIT)
    for k in range(3):
        sc.box(cx - 50 - k * 9, GROUND - 14 + k * 5, cx + 50 + k * 9, GROUND - 9 + k * 5, 10, STONE_LIT, STONE_SIDE, STONE_TOP, cast=False)


def cabinet_cover(sc, cx):
    sc.street = False
    floor = GROUND + 22
    sc.vgrad(0, 0, W, floor, rgb("#2F4A3A"), rgb("#1F3328"))
    for x in range(0, W, 30):
        sc.rect(x, 0, x + 12, floor, rgb("#2C4637"))
        for y in range(10, floor, 40):
            sc.ellipse(x + 18, y, x + 26, y + 10, rgb("#35523F"))
    sc.rect(0, floor - 46, W, floor, rgb("#5A3820"))
    sc.rect(0, floor - 50, W, floor - 44, rgb("#7A5030"))
    sc.vgrad(0, floor, W, H, rgb("#6E4A2E"), rgb("#3E2716"))
    for k in range(-24, 25):
        sc.d.line([(S(W / 2 + k * 50), S(floor)), (S(W / 2 + k * 150), S(H))], fill=rgb("#5E3E25"), width=SS)
    sc.poly([(cx - 300, H - 20), (cx + 300, H - 20), (cx + 230, floor + 26), (cx - 230, floor + 26)], rgb("#7E2F2A"))
    sc.poly([(cx - 270, H - 34), (cx + 270, H - 34), (cx + 210, floor + 36), (cx - 210, floor + 36)], rgb("#9A3E33"))
    for fx, kind in ((165, 0), (915, 1)):
        sc.shadow([(fx - 58, 254), (fx + 78, 254), (fx + 78, 402), (fx - 58, 402)], 110)
        sc.box(fx - 66, 242, fx + 66, 392, 6, rgb("#C9A24E"), rgb("#9F7A2C"), rgb("#E3C77A"), cast=False)
        sc.rect(fx - 54, 254, fx + 54, 380, rgb("#EFE6D2"))
        for k in range(6):
            x, y = fx - 34 + (k % 3) * 34, 282 + (k // 3) * 54
            col = [rgb("#4F8C84"), rgb("#C94A3A"), rgb("#D9A441"), rgb("#6E6AA8")][(k + kind) % 4]
            if kind == 0:
                sc.poly([(x, y), (x - 13, y - 10), (x - 11, y + 9)], col); sc.poly([(x, y), (x + 13, y - 10), (x + 11, y + 9)], col)
                sc.rect(x - 1, y - 9, x + 1, y + 9, INK)
            else:
                sc.ellipse(x - 12, y - 9, x + 12, y + 9, col); sc.ellipse(x - 6, y - 5, x + 2, y + 1, shade(col, 1.3))
    w, h = 340, 420
    x0, top = cx - w / 2, floor + 20 - h
    rr = w / 2
    sc.shadow([(x0 + w, floor + 20), (x0 + w + 70, floor - 10), (x0 + w + 70, top + 200), (x0 + w, top + 160)], 120)
    sc.ellipse(x0, top, x0 + w, top + w, rgb("#7A4B2A"))
    sc.rect(x0, top + rr, x0 + w, floor + 20, rgb("#7A4B2A"))
    sc.poly([(x0 + w, top + rr), (x0 + w + 26, top + rr - 18), (x0 + w + 26, floor + 2), (x0 + w, floor + 20)], rgb("#55331C"))
    m = 22
    sc.ellipse(x0 + m, top + m, x0 + w - m, top + w - m, rgb("#1C2A22"))
    sc.rect(x0 + m, top + rr, x0 + w - m, floor - 6, rgb("#1C2A22"))
    for y in range(int(top + rr), floor - 6, 5):
        sc.rect(x0 + m, y, x0 + w - m, y + 1, rgb("#20302A"))
    sc.de.ellipse([S(x0 + m), S(top + m), S(x0 + w - m), S(top + w - m)], fill=(70, 52, 26))
    sc.vgrad(x0 + m, top + rr, x0 + w - m, floor - 6, (70, 52, 26), (14, 10, 5), light=True)
    shelves = [top + 180, top + 272, top + 360]
    for y in shelves:
        sc.box(x0 + m, y, x0 + w - m, y + 8, 4, rgb("#B07A44"), rgb("#7A4B2A"), rgb("#C48A50"), cast=False)
        sc.shadow([(x0 + m, y + 8), (x0 + w - m, y + 8), (x0 + w - m, y + 22), (x0 + m, y + 22)], 120)
    gx, gy = cx, top + 112
    sc.rect(gx - 3, gy + 34, gx + 3, shelves[0], rgb("#C9A24E"))
    sc.box(gx - 28, shelves[0] - 7, gx + 28, shelves[0], 6, rgb("#C9A24E"), rgb("#9F7A2C"), rgb("#E3C77A"), cast=False)
    sc.ellipse(gx - 44, gy - 44, gx + 44, gy + 44, rgb("#4F8C84"), (40, 50, 40))
    sc.ellipse(gx - 32, gy - 30, gx + 6, gy + 10, rgb("#D9C78E"))
    sc.ellipse(gx + 4, gy - 4, gx + 34, gy + 28, rgb("#D9C78E"))
    sc.ellipse(gx - 30, gy - 34, gx - 10, gy - 20, rgb("#DDEBE6"))
    sc.d.arc([S(gx - 50), S(gy - 50), S(gx + 50), S(gy + 50)], 200, 340, fill=rgb("#C9A24E"), width=SS * 3)
    for k, (col, band) in enumerate([("#C94A3A", "#E9CF8C"), ("#1F3A5F", "#F2C14E"), ("#E6B422", "#7A4B12"), ("#2A9D8F", "#E9C46A")]):
        x = x0 + 50 + k * 58; y = shelves[1]
        for i in range(S(36)):
            t = i / (S(36) - 1); kk = 0.7 + 0.45 * math.cos((t - 0.3) * math.pi * 0.9)
            sc.d.line([(S(x) + i, S(y - 64)), (S(x) + i, S(y))], fill=shade(rgb(col), kk))
        sc.de.rectangle([S(x), S(y - 64), S(x + 36), S(y)], fill=(0, 0, 0))
        sc.rect(x, y - 44, x + 36, y - 32, rgb(band))
        sc.ellipse(x, y - 70, x + 36, y - 60, rgb("#C9CDD2"))
    sx, sy = x0 + w - 72, shelves[1]
    sc.ellipse(sx - 28, sy - 44, sx + 28, sy, rgb("#F1D9C4"))
    for k in range(6):
        sc.d.line([(S(sx), S(sy - 2)), (S(sx - 24 + k * 9.6), S(sy - 40))], fill=rgb("#D9B79A"), width=SS * 2)

    def croc(x, y, s, col, belly):
        c = rgb(col)
        sc.poly([(x - 64 * s, y - 5 * s), (x - 20 * s, y - 22 * s), (x + 30 * s, y - 25 * s), (x + 62 * s, y - 18 * s), (x + 80 * s, y - 12 * s), (x + 80 * s, y - 4 * s), (x + 40 * s, y), (x - 20 * s, y), (x - 64 * s, y - 2 * s)], c)
        sc.poly([(x - 10 * s, y - 7 * s), (x + 40 * s, y - 9 * s), (x + 40 * s, y), (x - 10 * s, y)], rgb(belly))
        sc.poly([(x - 20 * s, y - 22 * s), (x + 30 * s, y - 25 * s), (x + 30 * s, y - 19 * s), (x - 20 * s, y - 16 * s)], shade(c, 1.2))
        for k in range(8):
            bx = x - 34 * s + k * 9 * s
            sc.poly([(bx, y - 21 * s), (bx + 4 * s, y - 29 * s), (bx + 8 * s, y - 21 * s)], shade(c, 0.72))
        sc.ellipse(x + 48 * s, y - 31 * s, x + 59 * s, y - 20 * s, (250, 245, 225))
        sc.ellipse(x + 53 * s, y - 28 * s, x + 58 * s, y - 23 * s, (25, 22, 18))
        for k in range(4):
            sc.rect(x - 20 * s + k * 18 * s, y - 2 * s, x - 14 * s + k * 18 * s, y + 1, shade(c, 0.8))
    croc(x0 + 112, shelves[2] - 1, 1.0, "#5DAA68", "#F3E9C6")
    croc(x0 + 244, shelves[2] - 1, 0.7, "#B5892E", "#E2C46B")
    by = floor - 6
    for k, col in enumerate(["#8C3B2E", "#2F4A6B", "#6E6A3A"]):
        sc.box(x0 + 52 + k * 4, by - 15 - k * 15, x0 + 152 - k * 6, by - k * 15, 6, rgb(col), cast=False)
    sc.box(x0 + 190, by - 42, x0 + 272, by, 8, rgb("#A8743F"), cast=False)
    sc.rect(x0 + 196, by - 36, x0 + 266, by - 6, rgb("#C9A24E"))
    sc.d.line([(S(x0 + 84), S(top + 130)), (S(x0 + 44), S(floor - 20))], fill=rgb("#3F5A4B"), width=SS * 3)
    sc.ellipse(cx - 15, top - 28, cx + 15, top + 2, rgb("#C9A24E"))
    sc.box(x0 - 16, floor + 14, x0 + w + 16, floor + 26, 30, rgb("#4A2D1A"), cast=False)
    sc.rect(cx - 1, 0, cx + 1, 156, INK)
    sc.poly([(cx - 40, 186), (cx + 40, 186), (cx + 20, 156), (cx - 20, 156)], rgb("#C9A24E"), WARM)
    sc.lamps.append((cx, 190, floor + 60))
    # a stool with a magnifying glass
    sx0 = x0 + w + 90
    for lx in (sx0 + 6, sx0 + 60):
        sc.rect(lx, floor - 32, lx + 5, floor + 24, rgb("#6A4428"))
    sc.box(sx0, floor - 40, sx0 + 70, floor - 32, 30, rgb("#8A5A36"), cast=False)
    sc.d.line([(S(sx0 + 46), S(floor - 44)), (S(sx0 + 62), S(floor - 36))], fill=WOOD, width=SS * 4)
    sc.ellipse(sx0 + 22, sx0 * 0 + floor - 72, sx0 + 52, floor - 42, rgb("#C9A24E"))
    sc.ellipse(sx0 + 26, floor - 68, sx0 + 48, floor - 46, rgb("#BFD6D8"))


def stage_scene(stage):
    sc = Scene()
    cx = W / 2
    if stage == 0:
        cabinet_cover(sc, cx)
        return sc
    sky(sc, 7)
    town(sc, 3)
    plaza(sc)
    if stage == 1:
        sc.tree(cx - 260, GROUND + 4, 0.95, 1)
        sc.tree(cx + 290, GROUND - 2, 0.8, 2, "cypress")
        sc.hedge(cx - 330, cx - 200, GROUND + 6, 22, 20)
        room(sc, cx)
        sc.lantern(cx + 210, GROUND + 22, 92)
        sc.bench(cx - 230, GROUND + 64)
        return sc
    if stage == 2:
        sc.tree(cx - 300, GROUND, 1.0, 1); sc.tree(cx + 310, GROUND - 4, 0.9, 2, "cypress")
        townhouse(sc, cx)
        sc.hedge(cx - 262, cx - 196, GROUND + 6, 22, 20); sc.hedge(cx + 196, cx + 262, GROUND + 6, 22, 20)
        sc.lantern(cx - 220, GROUND + 26, 96); sc.lantern(cx + 220, GROUND + 26, 96)
        sc.bench(cx + 290, GROUND + 78)
        return sc
    w = {3: 380, 4: 390, 5: 420, 6: 420, 7: 440}[stage]
    h = 236
    if stage >= 6:
        sc.tree(cx - w / 2 - 260, GROUND - 6, 0.9, 4, "cypress")
    else:
        sc.tree(cx - w / 2 - 90, GROUND, 1.0, 5)
    if stage >= 4:
        sc.tree(cx + w / 2 + 262, GROUND - 6, 0.9, 3, "cypress")
    else:
        sc.tree(cx + w / 2 + 100, GROUND - 2, 0.9, 6, "cypress")
    top = GROUND - h
    if stage >= 5:
        dome(sc, cx + 22, top - 26, 92)
    if stage >= 6:
        wing(sc, cx - w / 2 - 210, cx - w / 2 + 6, 172, seed=20)
    if stage >= 4:
        wing(sc, cx + w / 2 - 6, cx + w / 2 + 210, 172, seed=10)
    temple(sc, cx, w, h, 6 if stage >= 5 else 4, stage)
    sc.hedge(cx - w / 2 - 64, cx - w / 2 + 6, GROUND + 18, 20, 16)
    sc.hedge(cx + w / 2 - 6, cx + w / 2 + 64, GROUND + 18, 20, 16)
    for lx in (cx - w / 2 - 44, cx + w / 2 + 44):
        sc.lantern(lx, GROUND + 44, 104)
    sc.bench(cx - w / 2 - 180, GROUND + 100)
    sc.bench(cx + w / 2 + 116, GROUND + 100)
    if stage >= 7:
        for fx in (cx - w / 2 - 18, cx + w / 2 + 18):
            sc.rect(fx - 1.6, top - 130, fx + 1.6, top + 12, INK)
            pts = [(fx + 1.6, top - 130)] + [(fx + 1.6 + t * 48, top - 128 + math.sin(t * 5) * 3) for t in np.linspace(0, 1, 8)] + \
                  [(fx + 1.6 + t * 48, top - 104 + math.sin(t * 5 + 1) * 3) for t in np.linspace(1, 0, 8)]
            sc.poly(pts, rgb("#B5473A"))
        for bx in (cx - w * 0.29, cx + w * 0.29):
            sc.poly([(bx - 16, top + 54), (bx + 16, top + 54), (bx + 16, top + 158), (bx, top + 148), (bx - 16, top + 158)], rgb("#9E3A30"))
            sc.rect(bx - 16, top + 54, bx - 10, top + 158, shade(rgb("#9E3A30"), 1.15))
            sc.ellipse(bx - 9, top + 74, bx + 9, top + 92, rgb("#E3C77A"))
    return sc


def render(sc, night):
    a = np.asarray(sc.a.resize((W, H), Image.LANCZOS)).astype(np.float32) / 255
    e = np.asarray(sc.e.resize((W, H), Image.LANCZOS)).astype(np.float32) / 255
    sh = np.asarray(sc.sh.resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(5))).astype(np.float32) / 255
    out = a * (1 - sh[..., None] * 0.55)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    if not night:
        out = out * np.array([1.03, 1.0, 0.95])
        if sc.street:
            glow = np.exp(-(((xx - 60) / 520) ** 2 + ((yy - 370) / 300) ** 2))
            out = out + glow[..., None] * np.array([0.10, 0.07, 0.02])
        else:
            lamp = np.exp(-(((xx - W / 2) / 260) ** 2 + ((yy - 340) / 260) ** 2))
            out = out + lamp[..., None] * np.array([0.10, 0.07, 0.02])
        out = np.clip(out + e * 0.04, 0, 1)
    else:
        out = out * np.array([0.30, 0.34, 0.50])
        if sc.street:
            out[:HORIZON + 60] = out[:HORIZON + 60] * np.linspace(0.55, 1.0, HORIZON + 60)[:, None, None]
            moon = np.exp(-(((xx - 880) / 240) ** 2 + ((yy - 240) / 200) ** 2))
            out = out + moon[..., None] * np.array([0.05, 0.07, 0.11])
        light = Image.fromarray((np.clip(e, 0, 1) * 255).astype(np.uint8))
        dl = ImageDraw.Draw(light)
        for x, y, base in sc.lamps:
            dl.ellipse([x - 9, y - 9, x + 9, y + 9], fill=(255, 214, 150))
        el = np.asarray(light).astype(np.float32) / 255 * 0.85
        b1 = np.asarray(light.filter(ImageFilter.GaussianBlur(10))).astype(np.float32) / 255
        b2 = np.asarray(light.filter(ImageFilter.GaussianBlur(44))).astype(np.float32) / 255
        out = out * (1 - el * 0.7) + el + b1 * 0.32 + b2 * 0.42
        for x, y, base in sc.lamps:
            pool = np.exp(-(((xx - x) / 110) ** 2 + ((yy - base) / 28) ** 2))
            out = out + pool[..., None] * np.array([0.30, 0.20, 0.08])
        out = np.clip(out, 0, 1)
    v = 1 - 0.22 * (((xx - W / 2) / (W * 0.7)) ** 2 + ((yy - H * 0.55) / (H * 0.8)) ** 2)
    out = out * v[..., None]
    out = out + np.random.default_rng(1).normal(0, 0.012, out.shape)
    img = Image.fromarray((np.clip(out, 0, 1) * 255).astype(np.uint8))
    if night and sc.street:
        d = ImageDraw.Draw(img)
        r = np.random.default_rng(11)
        for _ in range(110):
            x, y = r.uniform(0, W), r.uniform(0, HORIZON * 0.8)
            p = img.getpixel((int(x), int(y)))
            if p[2] < 110 and p[0] < 90:
                s = r.uniform(0.5, 1.5)
                d.ellipse([x - s, y - s, x + s, y + s], fill=(235, 235, 220))
        sky_c = img.getpixel((940, 214))
        d.ellipse([862, 200, 902, 240], fill=(240, 236, 214))
        d.ellipse([874, 194, 914, 234], fill=sky_c)
    return img


# ---------- visitors ----------

def visitor(kind):
    """A person on the plaza: two-tone clothes lit from the left, a walking stride, a shadow at the feet."""
    w, h = 90, 150
    im = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    def P(pts, c): d.polygon([(S(x), S(y)) for x, y in pts], fill=c)
    def E(x0, y0, x1, y1, c): d.ellipse([S(x0), S(y0), S(x1), S(y1)], fill=c)
    def L(x0, y0, x1, y1, c, wd): d.line([(S(x0), S(y0)), (S(x1), S(y1))], fill=c, width=max(1, S(wd)))
    palette = [((62, 52, 46), (92, 78, 66)), ((40, 56, 70), (66, 88, 108)), ((120, 52, 44), (166, 80, 62)), ((70, 66, 48), (102, 96, 68)),
               ((52, 46, 70), (80, 72, 104)), ((60, 110, 102), (92, 150, 140)), ((92, 58, 70), (130, 88, 102))]
    dark, lit = palette[kind % 7]
    dark = dark + (255,); lit = lit + (255,)
    skin = (204, 166, 134, 255); skin2 = (176, 138, 108, 255)
    E(16, 141, 84, 150, (0, 0, 0, 70))
    cx = 42
    stride = kind % 2 == 0
    legc = skin if kind in (1, 6) else (48, 44, 42, 255)
    if kind == 2:
        L(cx - 3, 120, cx - 7, 143, (48, 44, 42, 255), 4); L(cx + 3, 120, cx + 6, 143, (48, 44, 42, 255), 4)
        E(cx - 11, 140, cx - 2, 146, (30, 28, 26, 255)); E(cx + 2, 140, cx + 11, 146, (30, 28, 26, 255))
        P([(cx - 9, 92), (cx + 9, 92), (cx + 11, 122), (cx - 11, 122)], lit); P([(cx + 2, 92), (cx + 9, 92), (cx + 11, 122), (cx + 3, 122)], dark)
        E(cx - 8, 74, cx + 8, 92, skin); E(cx - 9, 70, cx + 8, 82, (90, 60, 40, 255))
        L(cx + 9, 98, cx + 24, 30, (90, 90, 90, 255), 0.8)
        E(cx + 12, 6, cx + 36, 34, (220, 168, 66, 255)); E(cx + 16, 10, cx + 24, 18, (245, 214, 140, 255))
        return im.resize((w, h), Image.LANCZOS)
    L(cx - 4, 112, cx - (10 if stride else 4), 143, legc, 5)
    L(cx + 4, 112, cx + (9 if stride else 4), 143, legc, 5)
    E(cx - (15 if stride else 9), 140, cx - (3 if stride else 1), 146, (30, 28, 26, 255))
    E(cx + (4 if stride else 0), 140, cx + (16 if stride else 10), 146, (30, 28, 26, 255))
    body = [(cx - 11, 48), (cx + 11, 48), (cx + 21, 118), (cx - 21, 118)] if kind in (1, 6) else [(cx - 14, 48), (cx + 14, 48), (cx + 16, 118), (cx - 16, 118)]
    P(body, dark)
    P([body[0], (cx + 1, 48), (cx - 1, 118), body[3]], lit)
    L(cx - 13, 52, cx - 17, 96, lit, 6); L(cx + 13, 52, cx + 16, 94, dark, 6)
    E(cx - 20, 92, cx - 13, 100, skin); E(cx + 13, 90, cx + 20, 98, skin2)
    L(cx, 40, cx, 50, skin2, 6)
    E(cx - 9, 22, cx + 9, 42, skin); E(cx + 2, 24, cx + 9, 40, skin2)
    if kind == 0:
        P([(cx - 15, 25), (cx + 15, 25), (cx + 15, 28), (cx - 15, 28)], (30, 28, 26, 255)); P([(cx - 9, 10), (cx + 9, 10), (cx + 9, 26), (cx - 9, 26)], (30, 28, 26, 255))
    elif kind == 1:
        E(cx - 10, 18, cx + 10, 34, (70, 44, 30, 255)); E(cx - 4, 12, cx + 6, 22, (70, 44, 30, 255))
        P([(cx + 15, 82), (cx + 29, 82), (cx + 30, 98), (cx + 14, 98)], (160, 76, 52, 255)); d.arc([S(cx + 15), S(70), S(cx + 29), S(90)], 180, 360, fill=(160, 76, 52, 255), width=SS * 2)
    elif kind == 3:
        E(cx - 10, 20, cx + 10, 32, (210, 210, 206, 255))
        L(cx + 17, 96, cx + 24, 145, (70, 46, 24, 255), 2)
    elif kind == 4:
        E(cx - 10, 20, cx + 10, 32, (50, 36, 28, 255))
        L(cx + 15, 94, cx + 15, 10, (40, 40, 40, 255), 1.2)
        d.pieslice([S(cx - 26), S(-8), S(cx + 56), S(44)], 180, 360, fill=(170, 64, 54, 255))
        d.pieslice([S(cx - 26), S(-8), S(cx + 56), S(44)], 180, 250, fill=(198, 86, 72, 255))
    elif kind == 5:
        P([(cx - 10, 22), (cx + 10, 22), (cx + 16, 28), (cx - 10, 28)], (40, 40, 44, 255))
        P([(cx - 26, 54), (cx - 13, 54), (cx - 13, 92), (cx - 26, 92)], (78, 62, 46, 255))
        P([(cx + 6, 60), (cx + 22, 60), (cx + 22, 71), (cx + 6, 71)], (28, 28, 30, 255)); E(cx + 10, 61, cx + 18, 70, (90, 110, 130, 255))
    else:
        E(cx - 13, 16, cx + 9, 28, (170, 54, 50, 255))
        P([(cx - 9, 44), (cx + 9, 44), (cx + 7, 52), (cx - 7, 52)], (214, 168, 66, 255))
    return im.resize((w, h), Image.LANCZOS)


# ---------- icon ----------

def icon():
    """Launcher icon layers (432 px = 108 dp adaptive): a lit arched cabinet with a can, a globe and a crocodile."""
    n = 432
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    top_c = np.array(rgb("#35543F"), dtype=np.float32); bot_c = np.array(rgb("#18291F"), dtype=np.float32)
    bg = top_c + (bot_c - top_c) * (yy / n)[..., None]
    glow = np.exp(-(((xx - n / 2) / 140) ** 2 + ((yy - n * 0.44) / 150) ** 2))
    bg = bg + glow[..., None] * np.array([70, 54, 24])
    Image.fromarray(np.clip(bg, 0, 255).astype(np.uint8)).save(OUT / "ic_bg.png")
    fg = Image.new("RGBA", (n * SS, n * SS), (0, 0, 0, 0))
    f = ImageDraw.Draw(fg)
    def X(v): return int(v * SS)
    cx = n / 2; w = 156; x0 = cx - w / 2; top = 112; bottom = 316; r = w / 2
    f.ellipse([X(x0 + 6), X(bottom - 2), X(x0 + w + 36), X(bottom + 18)], fill=(0, 0, 0, 90))
    f.ellipse([X(x0), X(top), X(x0 + w), X(top + w)], fill=(146, 94, 54, 255))
    f.rectangle([X(x0), X(top + r), X(x0 + w), X(bottom)], fill=(146, 94, 54, 255))
    for i in range(X(12)):
        f.line([(X(x0) + i, X(top + r)), (X(x0) + i, X(bottom))], fill=(int(176 - i / X(12) * 30), int(116 - i / X(12) * 20), 66, 255))
    f.rectangle([X(x0 + w - 12), X(top + r), X(x0 + w), X(bottom)], fill=(106, 64, 36, 255))
    m = 13
    f.ellipse([X(x0 + m), X(top + m), X(x0 + w - m), X(top + w - m)], fill=(28, 44, 35, 255))
    f.rectangle([X(x0 + m), X(top + r), X(x0 + w - m), X(bottom - 10)], fill=(28, 44, 35, 255))
    f.ellipse([X(cx - 56), X(top + 26), X(cx + 56), X(top + 136)], fill=(64, 92, 72, 255))
    for y in (top + 112, bottom - 14):
        f.rectangle([X(x0 + m), X(y), X(x0 + w - m), X(y + 7)], fill=(214, 164, 100, 255))
    for i in range(X(30)):
        t = i / (X(30) - 1); k = 0.72 + 0.42 * math.cos((t - 0.3) * math.pi * 0.9)
        f.line([(X(cx - 40) + i, X(top + 62)), (X(cx - 40) + i, X(top + 112))], fill=shade(rgb("#D2523F"), k) + (255,))
    f.rectangle([X(cx - 40), X(top + 78), X(cx - 10), X(top + 88)], fill=(236, 210, 142, 255))
    f.ellipse([X(cx - 40), X(top + 57), X(cx - 10), X(top + 66)], fill=(206, 210, 214, 255))
    f.ellipse([X(cx + 4), X(top + 70), X(cx + 44), X(top + 110)], fill=(86, 150, 140, 255))
    f.ellipse([X(cx + 10), X(top + 76), X(cx + 30), X(top + 94)], fill=(222, 204, 146, 255))
    f.ellipse([X(cx + 26), X(top + 90), X(cx + 40), X(top + 104)], fill=(222, 204, 146, 255))
    y = bottom - 14; s = 0.92; x = cx - 8
    f.polygon([(X(x - 56 * s), X(y - 4)), (X(x - 18 * s), X(y - 20 * s)), (X(x + 28 * s), X(y - 23 * s)), (X(x + 58 * s), X(y - 16 * s)), (X(x + 74 * s), X(y - 10 * s)),
               (X(x + 74 * s), X(y - 3 * s)), (X(x + 38 * s), X(y)), (X(x - 18 * s), X(y))], fill=(112, 184, 124, 255))
    for k in range(7):
        bx = x - 30 * s + k * 9 * s
        f.polygon([(X(bx), X(y - 19 * s)), (X(bx + 4 * s), X(y - 27 * s)), (X(bx + 8 * s), X(y - 19 * s))], fill=(78, 140, 92, 255))
    f.ellipse([X(x + 45 * s), X(y - 29 * s), X(x + 55 * s), X(y - 19 * s)], fill=(250, 245, 225, 255))
    f.ellipse([X(x + 49 * s), X(y - 26 * s), X(x + 54 * s), X(y - 21 * s)], fill=(25, 22, 18, 255))
    f.ellipse([X(cx - 11), X(top - 18), X(cx + 11), X(top + 4)], fill=(232, 198, 114, 255))
    f.rectangle([X(x0 - 10), X(bottom), X(x0 + w + 10), X(bottom + 11)], fill=(90, 56, 32, 255))
    fg.resize((n, n), Image.LANCZOS).save(OUT / "ic_fg.png")
    # monochrome: the cabinet's outline with its shelves
    mono = Image.new("RGBA", (n * SS, n * SS), (0, 0, 0, 0))
    mo = ImageDraw.Draw(mono)
    mo.ellipse([X(x0), X(top), X(x0 + w), X(top + w)], fill=(255, 255, 255, 255))
    mo.rectangle([X(x0), X(top + r), X(x0 + w), X(bottom)], fill=(255, 255, 255, 255))
    mo.ellipse([X(x0 + m), X(top + m), X(x0 + w - m), X(top + w - m)], fill=(0, 0, 0, 0))
    mo.rectangle([X(x0 + m), X(top + r), X(x0 + w - m), X(bottom - 10)], fill=(0, 0, 0, 0))
    for yv in (top + 112, bottom - 14):
        mo.rectangle([X(x0 + m), X(yv), X(x0 + w - m), X(yv + 7)], fill=(255, 255, 255, 255))
    mo.rectangle([X(cx - 40), X(top + 62), X(cx - 10), X(top + 112)], fill=(255, 255, 255, 255))
    mo.ellipse([X(cx + 4), X(top + 70), X(cx + 44), X(top + 110)], fill=(255, 255, 255, 255))
    mono.resize((n, n), Image.LANCZOS).save(OUT / "ic_mono.png")
    # preview for the README and the site
    prev = Image.open(OUT / "ic_bg.png").convert("RGBA")
    prev.alpha_composite(Image.open(OUT / "ic_fg.png"))
    prev.save(ROOT / "tools/art/icon_preview.png")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for st in range(8):
        sc = stage_scene(st)
        for night in (False, True):
            render(sc, night).save(OUT / f"facade_{st}_{'night' if night else 'day'}.webp", quality=90, method=6)
        print("stage", st)
    for k in range(7):
        visitor(k).save(OUT / f"visitor_{k}.png")
    icon()


if __name__ == "__main__":
    main()
