"""Paints a little lit showcase for each kind of hall (the cards of «What do you collect?»).

python tools/art/kinds.py

Same style as facade.py: a green velvet niche with a lamp, a walnut shelf, things on it lit from the left.
Writes app/src/main/res/drawable-nodpi/kind_<key>.webp, 480×300.
"""
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter
from facade import OUT, SS, rgb, shade, mix

W, H = 480, 300
SHELF = 236


def S(v):
    return int(round(v * SS))


class Niche:
    def __init__(self):
        self.im = Image.new("RGB", (S(W), S(H)))
        self.d = ImageDraw.Draw(self.im)
        self.sh = Image.new("L", (S(W), S(H)), 0)
        self.ds = ImageDraw.Draw(self.sh)
        for i in range(S(H)):
            self.d.line([(0, i), (S(W), i)], fill=mix(rgb("#34503F"), rgb("#1E3127"), i / S(H)))
        for x in range(0, W, 8):
            self.d.line([(S(x), 0), (S(x), S(SHELF))], fill=rgb("#2E4838"), width=SS)

    def poly(self, pts, c):
        self.d.polygon([(S(x), S(y)) for x, y in pts], fill=c)

    def rect(self, x0, y0, x1, y1, c):
        self.poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], c)

    def ell(self, x0, y0, x1, y1, c):
        self.d.ellipse([S(x0), S(y0), S(x1), S(y1)], fill=c)

    def line(self, pts, c, w):
        self.d.line([(S(x), S(y)) for x, y in pts], fill=c, width=S(w))

    def shadow(self, x0, x1, k=150):
        self.ds.ellipse([S(x0 + 6), S(SHELF - 5), S(x1 + 16), S(SHELF + 5)], fill=k)

    def cylinder(self, x0, y0, x1, y1, c):
        n = S(x1) - S(x0)
        for i in range(n):
            t = i / max(1, n - 1)
            k = 0.68 + 0.45 * math.cos((t - 0.3) * math.pi * 0.9)
            self.d.line([(S(x0) + i, S(y0)), (S(x0) + i, S(y1))], fill=shade(c, k))

    def finish(self, path):
        # shelf
        self.rect(0, SHELF, W, SHELF + 8, rgb("#B07A44"))
        self.rect(0, SHELF + 8, W, H, rgb("#6E4529"))
        for k in range(5):
            self.line([(0, SHELF + 18 + k * 12), (W, SHELF + 16 + k * 12)], rgb("#7A4F30"), 1)
        a = np.asarray(self.im.resize((W, H), Image.LANCZOS)).astype(np.float32) / 255
        sh = np.asarray(self.sh.resize((W, H), Image.LANCZOS).filter(ImageFilter.GaussianBlur(4))).astype(np.float32) / 255
        a = a * (1 - sh[..., None] * 0.6)
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        lamp = np.exp(-(((xx - W * 0.45) / 230) ** 2 + ((yy - 60) / 170) ** 2))
        a = a * (0.72 + 0.4 * lamp[..., None]) + lamp[..., None] * np.array([0.08, 0.06, 0.02])
        v = 1 - 0.25 * (((xx - W / 2) / W) ** 2 + ((yy - H / 2) / H) ** 2) * 2
        a = a * v[..., None] + np.random.default_rng(2).normal(0, 0.01, a.shape)
        Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).save(path, quality=90, method=6)


def can(n, x, col, band, h=96, w=52):
    n.shadow(x, x + w)
    n.cylinder(x, SHELF - h, x + w, SHELF, rgb(col))
    n.rect(x, SHELF - h * 0.62, x + w, SHELF - h * 0.46, rgb(band))
    n.ell(x, SHELF - h - 7, x + w, SHELF - h + 7, rgb("#C9CDD2"))
    n.ell(x + 6, SHELF - h - 4, x + w - 6, SHELF - h + 4, rgb("#E4E7EA"))


def croc(n, x, y, s, col, belly):
    c = rgb(col)
    n.shadow(x - 60 * s, x + 80 * s)
    n.poly([(x - 64 * s, y - 5 * s), (x - 20 * s, y - 22 * s), (x + 30 * s, y - 25 * s), (x + 62 * s, y - 18 * s), (x + 80 * s, y - 12 * s), (x + 80 * s, y - 4 * s), (x + 40 * s, y), (x - 20 * s, y), (x - 64 * s, y - 2 * s)], c)
    n.poly([(x - 10 * s, y - 7 * s), (x + 40 * s, y - 9 * s), (x + 40 * s, y), (x - 10 * s, y)], rgb(belly))
    n.poly([(x - 20 * s, y - 22 * s), (x + 30 * s, y - 25 * s), (x + 30 * s, y - 19 * s), (x - 20 * s, y - 16 * s)], shade(c, 1.2))
    for k in range(8):
        bx = x - 34 * s + k * 9 * s
        n.poly([(bx, y - 21 * s), (bx + 4 * s, y - 29 * s), (bx + 8 * s, y - 21 * s)], shade(c, 0.72))
    n.ell(x + 48 * s, y - 31 * s, x + 59 * s, y - 20 * s, (250, 245, 225))
    n.ell(x + 53 * s, y - 28 * s, x + 58 * s, y - 23 * s, (25, 22, 18))


def cans():
    n = Niche()
    for k, (c, b) in enumerate([("#1F3A5F", "#F2C14E"), ("#C94A3A", "#F4EBDD"), ("#E6B422", "#7A4B12"), ("#2A9D8F", "#E9C46A"), ("#6A4C93", "#FFCA3A"), ("#111111", "#C9A646")]):
        can(n, 40 + k * 68, c, b, h=96 if k % 2 == 0 else 110, w=48)
    n.finish(OUT / "kind_cans.webp")


def figures():
    n = Niche()
    croc(n, 140, SHELF, 1.3, "#5DAA68", "#F3E9C6")
    # a ceramic cat sitting
    x = 330
    n.shadow(x - 30, x + 40)
    n.ell(x - 32, SHELF - 70, x + 32, SHELF, rgb("#E9DFC9"))
    n.ell(x - 22, SHELF - 110, x + 22, SHELF - 62, rgb("#E9DFC9"))
    n.poly([(x - 20, SHELF - 98), (x - 16, SHELF - 122), (x - 4, SHELF - 106)], rgb("#E9DFC9"))
    n.poly([(x + 20, SHELF - 98), (x + 16, SHELF - 122), (x + 4, SHELF - 106)], rgb("#E9DFC9"))
    n.ell(x - 12, SHELF - 92, x - 5, SHELF - 85, rgb("#2B4A6B")); n.ell(x + 5, SHELF - 92, x + 12, SHELF - 85, rgb("#2B4A6B"))
    for k in range(4):
        n.line([(x - 28 + k * 4, SHELF - 50 + k * 10), (x - 10 + k * 3, SHELF - 48 + k * 10)], rgb("#3E6B9E"), 2)
    n.ell(x + 16, SHELF - 50, x + 44, SHELF - 6, rgb("#D8CCB2"))
    # a small brass elephant
    x = 420
    n.shadow(x - 22, x + 26)
    n.ell(x - 24, SHELF - 42, x + 18, SHELF - 10, rgb("#C9A24E"))
    n.ell(x + 8, SHELF - 48, x + 30, SHELF - 24, rgb("#D9B45E"))
    n.line([(x + 28, SHELF - 30), (x + 34, SHELF - 6)], rgb("#B08A3A"), 5)
    for lx in (x - 18, x - 4, x + 8):
        n.rect(lx, SHELF - 16, lx + 8, SHELF, rgb("#A9853A"))
    n.finish(OUT / "kind_figures.webp")


def magnets():
    n = Niche()
    # a fridge door panel
    n.rect(70, 20, 410, SHELF, rgb("#ECE8E0"))
    n.rect(400, 20, 410, SHELF, rgb("#C9C4BA"))
    n.rect(380, 90, 390, 170, rgb("#B9B4AA"))
    r = np.random.default_rng(4)
    shapes = [(110, 50, "#C94A3A"), (190, 60, "#4F8C84"), (270, 48, "#D9A441"), (330, 70, "#6E6AA8"), (120, 140, "#2F4A6B"), (210, 150, "#E07A5F"), (300, 140, "#5DAA68")]
    for x, y, c in shapes:
        kind = r.integers(3)
        if kind == 0:
            n.rect(x - 28, y - 20, x + 28, y + 24, rgb("#F7F1E3")); n.rect(x - 24, y - 16, x + 24, y + 14, rgb(c))
            n.poly([(x - 24, y + 14), (x - 6, y - 4), (x + 8, y + 8), (x + 24, y - 8), (x + 24, y + 14)], shade(rgb(c), 0.7))
        elif kind == 1:
            n.ell(x - 26, y - 26, x + 26, y + 26, rgb(c)); n.ell(x - 16, y - 16, x + 16, y + 16, rgb("#F7F1E3"))
        else:
            n.poly([(x, y - 28), (x + 28, y + 20), (x - 28, y + 20)], rgb(c)); n.ell(x - 8, y - 2, x + 8, y + 14, rgb("#F7F1E3"))
    n.finish(OUT / "kind_magnets.webp")


def coins():
    n = Niche()
    for k in range(9):  # a stack
        y = SHELF - 8 - k * 9
        n.ell(110, y - 8, 200, y + 8, shade(rgb("#C9A24E"), 0.85 + 0.02 * k))
        n.ell(110, y - 12, 200, y + 4, rgb("#D9B45E"))
    n.shadow(110, 200)
    # a big coin standing
    x, y, r = 300, SHELF - 66, 64
    n.shadow(x - 50, x + 60)
    n.ell(x - r, y - r, x + r, y + r, rgb("#A9A9A9"))
    n.ell(x - r + 6, y - r + 6, x + r - 6, y + r - 6, rgb("#CFCFCF"))
    n.ell(x - 26, y - 34, x + 18, y + 18, rgb("#B5B5B5"))
    n.line([(x - 40, y + 30), (x + 40, y + 30)], rgb("#9A9A9A"), 3)
    # a banknote
    n.poly([(370, SHELF - 4), (470, SHELF - 30), (476, SHELF - 2), (380, SHELF + 2)], rgb("#8DB596"))
    n.ell(410, SHELF - 22, 430, SHELF - 10, rgb("#6E9C7A"))
    n.finish(OUT / "kind_coins.webp")


def paper():
    n = Niche()
    # a postcard leaning
    n.shadow(60, 250)
    n.poly([(70, SHELF), (240, SHELF), (262, SHELF - 120), (92, SHELF - 120)], rgb("#F3EBDD"))
    n.poly([(96, SHELF - 112), (180, SHELF - 112), (166, SHELF - 30), (82, SHELF - 30)], rgb("#7FB0C4"))
    n.poly([(82, SHELF - 54), (120, SHELF - 80), (150, SHELF - 60), (166, SHELF - 30), (82, SHELF - 30)], rgb("#5E8C62"))
    for k in range(4):
        n.line([(196, SHELF - 90 + k * 16), (240, SHELF - 90 + k * 16)], rgb("#A89A84"), 1.5)
    # stamps with perforation
    for k, (c, x, y) in enumerate([("#C94A3A", 300, 110), ("#2F4A6B", 380, 130), ("#D9A441", 330, 180)]):
        n.rect(x - 34, y - 42, x + 34, y + 42, rgb("#FBF7EE"))
        for t in range(9):
            for (px, py) in ((x - 34 + t * 8.5, y - 42), (x - 34 + t * 8.5, y + 42), (x - 34, y - 42 + t * 10.5), (x + 34, y - 42 + t * 10.5)):
                n.ell(px - 3, py - 3, px + 3, py + 3, rgb("#2A3F33"))
        n.rect(x - 26, y - 34, x + 26, y + 34, rgb(c))
        n.ell(x - 12, y - 16, x + 12, y + 8, shade(rgb(c), 1.35))
    n.finish(OUT / "kind_paper.webp")


def records():
    n = Niche()
    x, y, r = 170, SHELF - 100, 98
    n.shadow(x - 90, x + 90)
    n.rect(x - 104, SHELF - 206, x + 104, SHELF, rgb("#C94A3A"))
    n.rect(x - 104, SHELF - 206, x - 60, SHELF, rgb("#D9A441"))
    n.ell(x - r + 60, y - r, x + r + 60, y + r, rgb("#1A1A1A"))
    for k in range(6):
        rr = r - 10 - k * 10
        n.d.arc([S(x + 60 - rr), S(y - rr), S(x + 60 + rr), S(y + rr)], 200, 300, fill=rgb("#3A3A3A"), width=SS)
    n.ell(x + 60 - 26, y - 26, x + 60 + 26, y + 26, rgb("#E9C46A"))
    n.ell(x + 56, y - 4, x + 64, y + 4, rgb("#1A1A1A"))
    # a cassette
    cx = 380
    n.shadow(cx - 60, cx + 60)
    n.rect(cx - 64, SHELF - 84, cx + 64, SHELF, rgb("#2F2F33"))
    n.rect(cx - 54, SHELF - 76, cx + 54, SHELF - 40, rgb("#F3EBDD"))
    n.rect(cx - 54, SHELF - 66, cx + 54, SHELF - 58, rgb("#E07A5F"))
    for dx in (-28, 28):
        n.ell(cx + dx - 12, SHELF - 34, cx + dx + 12, SHELF - 10, rgb("#DDD6C8"))
        n.ell(cx + dx - 5, SHELF - 27, cx + dx + 5, SHELF - 17, rgb("#2F2F33"))
    n.finish(OUT / "kind_records.webp")


def cars():
    n = Niche()
    def car(x, s, col):
        c = rgb(col)
        n.shadow(x - 70 * s, x + 80 * s)
        n.poly([(x - 80 * s, SHELF - 20 * s), (x - 70 * s, SHELF - 46 * s), (x - 30 * s, SHELF - 52 * s), (x - 6 * s, SHELF - 80 * s), (x + 46 * s, SHELF - 80 * s), (x + 70 * s, SHELF - 52 * s),
                (x + 86 * s, SHELF - 44 * s), (x + 88 * s, SHELF - 20 * s)], c)
        n.poly([(x + 2 * s, SHELF - 74 * s), (x + 20 * s, SHELF - 74 * s), (x + 20 * s, SHELF - 54 * s), (x - 16 * s, SHELF - 54 * s)], rgb("#BFD6D8"))
        n.poly([(x + 26 * s, SHELF - 74 * s), (x + 44 * s, SHELF - 74 * s), (x + 60 * s, SHELF - 54 * s), (x + 26 * s, SHELF - 54 * s)], rgb("#BFD6D8"))
        n.rect(x - 80 * s, SHELF - 46 * s, x + 88 * s, SHELF - 40 * s, shade(c, 1.25))
        for wx in (x - 46 * s, x + 54 * s):
            n.ell(wx - 18 * s, SHELF - 36 * s, wx + 18 * s, SHELF, rgb("#1E1E1E"))
            n.ell(wx - 8 * s, SHELF - 26 * s, wx + 8 * s, SHELF - 10 * s, rgb("#C9CDD2"))
    car(150, 1.25, "#C94A3A")
    car(360, 0.9, "#2F6B9E")
    n.finish(OUT / "kind_cars.webp")


def own():
    n = Niche()
    # a curiosity chest, a key, a question in brass
    x = 200
    n.shadow(x - 90, x + 100)
    n.rect(x - 96, SHELF - 90, x + 96, SHELF, rgb("#7A4B2A"))
    n.d.pieslice([S(x - 96), S(SHELF - 150), S(x + 96), S(SHELF - 30)], 180, 360, fill=rgb("#8A5A36"))
    for dx in (-60, 0, 60):
        n.rect(x + dx - 6, SHELF - 150 + (0 if dx == 0 else 10), x + dx + 6, SHELF, rgb("#C9A24E"))
    n.rect(x - 16, SHELF - 76, x + 16, SHELF - 46, rgb("#E3C77A"))
    n.ell(x - 5, SHELF - 68, x + 5, SHELF - 58, rgb("#3A2412"))
    kx = 370
    n.shadow(kx - 40, kx + 40)
    n.ell(kx - 30, SHELF - 30, kx - 4, SHELF - 4, rgb("#C9A24E"))
    n.ell(kx - 22, SHELF - 22, kx - 12, SHELF - 12, rgb("#2A3F33"))
    n.rect(kx - 4, SHELF - 20, kx + 50, SHELF - 14, rgb("#C9A24E"))
    n.rect(kx + 36, SHELF - 14, kx + 42, SHELF - 4, rgb("#C9A24E")); n.rect(kx + 46, SHELF - 14, kx + 50, SHELF - 6, rgb("#C9A24E"))
    for k in range(12):
        a = k / 12 * 2 * math.pi
        n.ell(330 + math.cos(a) * 60 - 2, 90 + math.sin(a) * 30 - 2, 330 + math.cos(a) * 60 + 2, 90 + math.sin(a) * 30 + 2, rgb("#E3C77A"))
    n.finish(OUT / "kind_own.webp")


if __name__ == "__main__":
    for f in (cans, figures, magnets, coins, paper, records, cars, own):
        f()
    print("kinds done")
