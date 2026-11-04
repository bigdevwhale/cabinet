"""Turns phone screenshots into the README screenshots.

python tools/shots.py ru path/to/pngs

The screenshots are taken on a phone from the demo build (./gradlew assembleRelease -Pdemo [-Pshots -Plang=en]);
this scales them down and writes docs/screenshots/<lang>/*.jpg.
"""
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
WIDTH = 720


def main(lang: str, src: str):
    out = ROOT / f"docs/screenshots/{lang}"
    out.mkdir(parents=True, exist_ok=True)
    for f in sorted(Path(src).glob("*.png")):
        im = Image.open(f).convert("RGB")
        im = im.resize((WIDTH, round(im.height * WIDTH / im.width)), Image.LANCZOS)
        im.save(out / f"{f.stem}.jpg", quality=86, optimize=True)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
