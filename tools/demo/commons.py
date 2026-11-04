"""Finds free photos of real things on Wikimedia Commons for the demo museum.

python tools/demo/commons.py search      -> build/commons/candidates.json + sheet_<group>.png (numbered thumbnails)
python tools/demo/commons.py fetch       -> downloads the picks listed in tools/demo/picks.json into build/commons/full/

Only free licences are kept (public domain, CC0, CC BY, CC BY-SA); author and licence travel with every file
into tools/demo/credits.md.
"""
import io
import json
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "build" / "commons"
UA = {"User-Agent": "CabinetDemoBuilder/1.0 (https://github.com/bigdevwhale/cabinet; demo images for an open-source app)"}
FREE = re.compile(r"^(public domain|pd|cc0|cc by(-sa)? ?[1-4]\.\d|cc by(-sa)?)", re.I)

QUERIES = {
    "coins": ["soviet coin kopeks obverse", "russian empire coin kopek", "coin ruble reverse ussr", "silver coin rouble 1924", "5 kopecks 1961",
              "commemorative ruble ussr", "russian imperial silver coin"],
    "books": ["old book cover binding", "antique book leather binding", "book cover cloth 19th century", "first edition book cover", "Bodleian Libraries binding",
              "russian book 19th century cover"],
    "records": ["gramophone record 78 rpm label", "melodiya record label", "vinyl record with sleeve", "shellac record", "phonograph record label 1930",
                "soviet vinyl record"],
}


def api(params):
    url = "https://commons.wikimedia.org/w/api.php?" + urllib.parse.urlencode({**params, "format": "json"})
    with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=60) as r:
        return json.load(r)


def get(url):
    for k in range(6):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=UA), timeout=90) as r:
                return r.read()
        except Exception:
            time.sleep(3 + k * 5)
    raise RuntimeError(url)


def clean(html):
    return re.sub(r"\s+", " ", re.sub(r"<[^>]+>", "", html or "")).strip()


def search():
    OUT.mkdir(parents=True, exist_ok=True)
    found = {}
    for group, qs in QUERIES.items():
        seen, items = set(), []
        for q in qs:
            d = api({"action": "query", "generator": "search", "gsrnamespace": 6, "gsrsearch": q + " filetype:bitmap", "gsrlimit": 30,
                     "prop": "imageinfo", "iiprop": "url|size|extmetadata", "iiurlwidth": 320,
                     "iiextmetadatafilter": "LicenseShortName|Artist|LicenseUrl"})
            for p in sorted(d.get("query", {}).get("pages", {}).values(), key=lambda p: p["index"]):
                ii = p["imageinfo"][0]
                m = ii.get("extmetadata", {})
                lic = clean(m.get("LicenseShortName", {}).get("value"))
                if p["title"] in seen or not FREE.match(lic) or min(ii["width"], ii["height"]) < 500:
                    continue
                seen.add(p["title"])
                items.append({"title": p["title"], "url": ii["url"], "thumb": ii["thumburl"], "w": ii["width"], "h": ii["height"],
                              "license": lic, "license_url": clean(m.get("LicenseUrl", {}).get("value")), "artist": clean(m.get("Artist", {}).get("value"))[:120],
                              "page": "https://commons.wikimedia.org/wiki/" + urllib.parse.quote(p["title"].replace(" ", "_"))})
        found[group] = items
        # numbered contact sheet
        cols, cell = 8, 200
        sheet = Image.new("RGB", (cols * cell, ((len(items) + cols - 1) // cols) * cell), "white")
        d = ImageDraw.Draw(sheet)
        for i, it in enumerate(items):
            try:
                im = Image.open(io.BytesIO(get(it["thumb"]))).convert("RGB")
            except Exception:
                continue
            im.thumbnail((cell - 8, cell - 24))
            x, y = (i % cols) * cell, (i // cols) * cell
            sheet.paste(im, (x + 4, y + 20))
            d.text((x + 4, y + 3), f"{i}", fill="red")
        sheet.save(OUT / f"sheet_{group}.png")
        print(group, len(items))
    json.dump(found, open(OUT / "candidates.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)


def fetch():
    found = json.load(open(OUT / "candidates.json", encoding="utf-8"))
    picks = json.load(open(ROOT / "tools/demo/picks.json", encoding="utf-8"))
    full = OUT / "full"
    full.mkdir(parents=True, exist_ok=True)
    credits = ["# Demo photos", "", "The demo museum shows real things photographed by these people, from Wikimedia Commons.", ""]
    for key, (group, idx) in picks.items():
        it = found[group][idx]
        dst = full / f"{key}.jpg"
        if not dst.exists():
            # a 1600 px copy is plenty for a phone, and Commons serves thumbnails more gently than originals
            # a standard 960 px thumbnail: Commons serves those from cache without throttling, and it is plenty for a phone
            url = re.sub(r"/(\d+)px-", "/960px-", it["thumb"]) if it["w"] > 960 else it["url"]
            dst.write_bytes(get(url))
            time.sleep(1)
        credits.append(f"- `{key}`: [{it['title'][5:]}]({it['page']}), {it['artist'] or 'unknown author'}, {it['license']}")
        print(key, it["title"])
    (ROOT / "tools/demo/credits.md").write_text("\n".join(credits) + "\n", encoding="utf-8")


if __name__ == "__main__":
    {"search": search, "fetch": fetch}[sys.argv[1]]()
