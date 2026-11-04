"""Turns the Blender renders in build/diorama into app resources.

python tools/art/pack.py

facade_*.png -> drawable-nodpi/facade_*.webp (with alpha: the diorama stands on the page itself)
kind_*.png   -> drawable-nodpi/kind_*.webp
icon.png     -> drawable-nodpi/ic_fg.png, ic_bg.png, ic_mono.png (adaptive icon, 108 dp at 432 px)
"""
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "build/diorama"
OUT = ROOT / "app/src/main/res/drawable-nodpi"


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for f in sorted(SRC.glob("facade_*.png")) + sorted(SRC.glob("kind_*.png")):
        im = Image.open(f)
        if f.stem.startswith("facade"):
            # trim the empty margin around the diorama, keep a little air
            bbox = im.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
            if bbox:
                pad = 24
                x0, y0, x1, y1 = bbox
                im = im.crop((max(0, x0 - pad), max(0, y0 - pad), min(im.width, x1 + pad), min(im.height, y1 + pad)))
                # one aspect for every stage, so the screen doesn't jump as the museum grows
                target = 1080 / 700
                w, h = im.size
                if w / h > target:
                    nh = round(w / target); canvas = Image.new("RGBA", (w, nh)); canvas.paste(im, (0, nh - h)); im = canvas
                else:
                    nw = round(h * target); canvas = Image.new("RGBA", (nw, h)); canvas.paste(im, ((nw - w) // 2, 0)); im = canvas
                im = im.resize((1080, 700), Image.LANCZOS)
        im.save(OUT / f"{f.stem}.webp", quality=90, method=6)
        print(f.stem)
    icon = SRC / "icon.png"
    if icon.exists():
        n = 432
        src = Image.open(icon).convert("RGBA")
        bbox = src.getchannel("A").point(lambda a: 255 if a > 230 else 0).getbbox()
        src = src.crop(bbox)
        k = (n * 0.68) / max(src.size)
        src = src.resize((round(src.width * k), round(src.height * k)), Image.LANCZOS)
        fg = Image.new("RGBA", (n, n))
        solid = src.copy(); solid.putalpha(src.getchannel("A").point(lambda a: 255 if a > 230 else 0))
        fg.alpha_composite(solid, ((n - src.width) // 2, (n - src.height) // 2))
        fg.save(OUT / "ic_fg.png")
        yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
        top = np.array([0x35, 0x54, 0x3F], np.float32); bot = np.array([0x18, 0x29, 0x1F], np.float32)
        bg = top + (bot - top) * (yy / n)[..., None]
        glow = np.exp(-(((xx - n / 2) / 150) ** 2 + ((yy - n * 0.45) / 150) ** 2))
        bg = bg + glow[..., None] * np.array([70, 54, 24])
        Image.fromarray(np.clip(bg, 0, 255).astype(np.uint8)).save(OUT / "ic_bg.png")
        a = np.asarray(fg.getchannel("A"))
        mono = np.zeros((n, n, 4), np.uint8); mono[..., :3] = 255; mono[..., 3] = (a > 100) * 255
        Image.fromarray(mono).save(OUT / "ic_mono.png")
        prev = Image.open(OUT / "ic_bg.png").convert("RGBA"); prev.alpha_composite(fg)
        prev.save(ROOT / "tools/art/icon_preview.png")
        print("icon")


if __name__ == "__main__":
    main()
