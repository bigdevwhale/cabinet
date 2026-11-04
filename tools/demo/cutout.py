"""Cuts the demo photos out of their backgrounds, as the app does with a new photo.

python tools/demo/cutout.py       (needs: pip install "rembg[cpu]")
build/commons/full/<key>.jpg -> app/src/demo/assets/things/<key>.webp (transparent, at most 900 px)
"""
from pathlib import Path

from PIL import Image
from rembg import new_session, remove

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "build/commons/full"
OUT = ROOT / "app/src/demo/assets/things"


# Flat covers and sleeves stay whole: a cut-out would eat the cloth and leave only the gilt.
WHOLE = ("rec_imperial", "rec_beatles", "rec_vox")
# Museum scans with a colour chart along the bottom edge: the share of the height to drop.
CROP_BOTTOM = {"book_flower": 0.11}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    session = new_session("isnet-general-use")
    for f in sorted(SRC.glob("*.jpg")):
        dst = OUT / f"{f.stem}.webp"
        if dst.exists():
            continue
        im = Image.open(f).convert("RGB")
        if f.stem in CROP_BOTTOM:
            im = im.crop((0, 0, im.width, int(im.height * (1 - CROP_BOTTOM[f.stem]))))
        if f.stem.startswith("book_") or f.stem in WHOLE:
            im.thumbnail((900, 900))
            im.save(dst, "WEBP", quality=86, method=6)
            print(f.stem, "whole", im.size)
            continue
        cut = remove(im, session=session, post_process_mask=True)
        box = cut.getchannel("A").point(lambda a: 255 if a > 40 else 0).getbbox()
        if box:
            pad = 6
            cut = cut.crop((max(0, box[0] - pad), max(0, box[1] - pad), min(cut.width, box[2] + pad), min(cut.height, box[3] + pad)))
        cut.thumbnail((900, 900))
        cut.save(dst, "WEBP", quality=88, method=6)
        print(f.stem, cut.size)


if __name__ == "__main__":
    main()
