#!/usr/bin/env python3
"""Export the eight-page companion and package the existing, unmodified development JAR.

Requires Python Markdown, PySide6, and pypdf. Runs locally without fetching fonts or images.
The output directory is ignored by Git; the original handbook and JAR are never edited.
"""

import argparse
import base64
from datetime import datetime, timezone
import hashlib
from html import escape
from html.parser import HTMLParser
import json
import mimetypes
from pathlib import Path
import re
import shutil
import subprocess
import tomllib
from urllib.parse import unquote
import zipfile

import markdown

from render_klei_pdf import render_pdf


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "docs/klei-field-guide.md"
OUTPUT = ROOT / "output/klei-field-guide"
STEM = "Retold-Field-Guide"
MEDIA_EXTENSIONS = {".png", ".jpg", ".jpeg", ".webp", ".mp4", ".webm"}


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def properties():
    return dict(re.findall(r"^([a-z_]+)=(.+)$", (ROOT / "gradle.properties").read_text(), re.M))


class References(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = []
        self.links = []
        self.images = []

    def handle_starttag(self, tag, attributes):
        attributes = dict(attributes)
        if "id" in attributes:
            self.ids.append(attributes["id"])
        if tag == "a":
            self.links.append(attributes.get("href", ""))
        if tag == "img":
            self.images.append(attributes.get("src", ""))


def package_identity(settings):
    jar = ROOT / f"build/libs/retold-{settings['mod_version']}.jar"
    if not jar.is_file():
        raise ValueError(f"Missing development build: {jar.relative_to(ROOT)}")
    inputs = [ROOT / name for name in ("build.gradle", "settings.gradle", "gradle.properties")]
    inputs += [p for directory in ("src/main", "src/generated", "gradle")
               for p in (ROOT / directory).rglob("*") if p.is_file() and not p.name.endswith("~")]
    newer = [str(p.relative_to(ROOT)) for p in inputs if p.stat().st_mtime > jar.stat().st_mtime]
    if newer:
        raise ValueError("Build inputs newer than the JAR; check/rebuild before sharing: " + ", ".join(newer[:8]))
    compiled = sorted((ROOT / "build/classes/java/main").rglob("*.class"))
    if not compiled:
        raise ValueError("No compiled classes available for the static JAR comparison")
    resources = [p for p in (ROOT / "src/main/resources").rglob("*")
                 if p.is_file() and not p.name.endswith("~") and p.suffix != ".bbmodel"]
    with zipfile.ZipFile(jar) as archive:
        metadata = tomllib.loads(archive.read("META-INF/neoforge.mods.toml").decode())
        mod = next(mod for mod in metadata["mods"] if mod["modId"] == "retold")
        if mod["version"] != settings["mod_version"]:
            raise ValueError("JAR mod version differs from gradle.properties")
        minecraft = next(dep for dep in metadata["dependencies"]["retold"] if dep["modId"] == "minecraft")
        if minecraft["versionRange"] != settings["minecraft_version_range"]:
            raise ValueError("JAR Minecraft target differs from gradle.properties")
        for files, root in ((compiled, ROOT / "build/classes/java/main"),
                            (resources, ROOT / "src/main/resources")):
            for path in files:
                entry = path.relative_to(root).as_posix()
                if entry not in archive.namelist() or archive.read(entry) != path.read_bytes():
                    raise ValueError(f"JAR differs from current compiled/resource input: {entry}")
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    dirty = bool(subprocess.check_output(["git", "status", "--porcelain"], cwd=ROOT, text=True).strip())
    return jar, {
        "retold": settings["mod_version"], "minecraft": settings["minecraft_version"],
        "neoforge": settings["neo_version"], "java": "25", "artifact": jar.name,
        "artifact_sha256": digest(jar),
        "artifact_file_modified_utc": datetime.fromtimestamp(jar.stat().st_mtime, timezone.utc).isoformat(),
        "source_revision": revision, "source_worktree_dirty": dirty,
        "compiled_classes_compared": len(compiled), "source_resources_compared": len(resources),
        "guide_source_sha256": digest(SOURCE),
        "validation": "Static metadata, timestamps, compiled-class and resource byte comparisons; no new build or gameplay run.",
    }


def inline_images(html):
    def replace(match):
        path = (ROOT / "docs" / unquote(match.group(1))).resolve()
        if not path.is_relative_to(ROOT / "docs/media") or not path.is_file():
            raise ValueError(f"Image must be an existing local docs/media file: {match.group(1)}")
        media_type = mimetypes.guess_type(path.name)[0]
        return 'src="data:' + media_type + ';base64,' + base64.b64encode(path.read_bytes()).decode() + '"'
    return re.sub(r'src="([^"]+)"', replace, html)


def render(version):
    parts = re.split(r"<!-- page: ([a-z-]+) -->", SOURCE.read_text())
    if parts[0].strip() or len(parts) != 17:
        raise ValueError("Guide must contain exactly eight explicit page markers")
    pages, navigation = [], []
    for number, (slug, text) in enumerate(zip(parts[1::2], parts[2::2]), 1):
        content = markdown.markdown(text, extensions=["tables", "fenced_code", "toc", "md_in_html"])
        content = inline_images(content)
        content = content.replace('href="player-guide.md', 'href="Retold-Player-Guide.html')
        pages.append(
            f'<section class="sheet" id="{slug}"><header class="running-head">'
            '<strong>RETOLD</strong><span>A LIVING-WORLD FIELD GUIDE</span></header>'
            f'<div class="page-content">{content}</div><footer class="page-footer">'
            f'<span>XEFENSOR · {escape(version)} ALPHA</span><span>Reference: '
            '<a href="Retold-Player-Guide.html">handbook</a> / '
            '<a href="Retold-Player-Guide.pdf">PDF</a></span>'
            f'<span class="page-number">{number:02d} / 08</span></footer></section>'
        )
        navigation.append(f'<a href="#{slug}">{number:02d} {escape(slug.replace("-", " "))}</a>')
    template = (ROOT / "docs/internal/klei-field-guide.template.html").read_text()
    return template.replace("@@NAV@@", "".join(navigation)).replace("@@PAGES@@", "\n".join(pages))


def validate_links(path):
    parser = References()
    parser.feed(path.read_text())
    if len(parser.ids) != len(set(parser.ids)):
        raise ValueError("Duplicate guide anchors")
    for link in parser.links:
        if link.startswith(("https:", "http:", "mailto:")):
            continue  # External links are preserved, not network-verified.
        name, _, fragment = link.partition("#")
        destination = path.parent / unquote(name) if name else path
        if not destination.is_file():
            raise ValueError(f"Broken local guide link: {link}")
        if fragment:
            target = References()
            target.feed(destination.read_text())
            if unquote(fragment) not in target.ids:
                raise ValueError(f"Broken guide anchor: {link}")
    if not parser.images or any(not src.startswith("data:image/") for src in parser.images):
        raise ValueError("Guide images must be embedded for offline use")
    return len(parser.links)


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--include-media", action="store_true", help="Include existing top-level gameplay media in the ZIP")
    args = cli.parse_args()
    settings = properties()
    for value in (settings["mod_version"], settings["minecraft_version"], settings["neo_version"]):
        if value not in SOURCE.read_text():
            raise ValueError(f"Guide does not identify the configured version: {value}")
    jar, identity = package_identity(settings)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    html_path = OUTPUT / f"{STEM}.html"
    html_path.write_text(render(settings["mod_version"]))
    shutil.copyfile(jar, OUTPUT / jar.name)
    for suffix in ("html", "pdf"):
        name = f"Retold-Player-Guide.{suffix}"
        shutil.copyfile(ROOT / "output/player-guide" / name, OUTPUT / name)
    for name in ("LICENSE", "LICENSE-CODE.md", "LICENSE-ASSETS.md", "ASSET_CREDITS.md"):
        shutil.copyfile(ROOT / name, OUTPUT / name)
    (OUTPUT / "BUILD.json").write_text(json.dumps(identity, indent=2) + "\n")
    links = validate_links(html_path)
    pdf = OUTPUT / f"{STEM}.pdf"
    render_pdf(html_path.read_text(), pdf, settings["mod_version"])
    from pypdf import PdfReader
    reader = PdfReader(pdf)
    if len(reader.pages) != 8:
        raise ValueError(f"Expected eight PDF pages, got {len(reader.pages)}; inspect page overflow")
    for number, page in enumerate(reader.pages, 1):
        text = " ".join(page.extract_text().split())
        if f"{number:02d} / 08" not in text:
            raise ValueError(f"Missing page footer on PDF page {number}")
    (OUTPUT / "START-HERE.txt").write_text(
        "RETOLD — FOR KLEI\n\n"
        f"Build: {jar.name} (development snapshot)\n"
        f"Minecraft Java {settings['minecraft_version']} / NeoForge {settings['neo_version']} / Java 25\n\n"
        "Put the JAR in your matching NeoForge instance’s mods folder. Start a fresh world.\n"
        "Multiplayer requires the same JAR on the server and every client.\n\n"
        "Open Retold-Field-Guide.pdf or Retold-Field-Guide.html. Page 2 covers Survival;\n"
        "page 7 has three Creative experiments. The full Player Guide is an optional\n"
        "reference with progression spoilers. Keep these files together for local links.\n\n"
        "Retold is experimental. AI assistance is used for development and some development\n"
        "artwork is generated; see the included asset credits. BUILD.json identifies this\n"
        "exact JAR; SHA256SUMS lists the included files. This package does not contain\n"
        "Minecraft, Java, or the NeoForge installer.\n\n"
        "Project: https://github.com/xefensor/Retold\n"
        "Contact: alex@alexejtusl.cz\n"
    )
    notes = (ROOT / "docs/internal/klei-outreach.md").read_text()
    message = notes.split("<!-- message:start -->", 1)[1].split("<!-- message:end -->", 1)[0].strip()
    (ROOT / "output/klei-message.txt").write_text(message + "\n")
    names = [f"{STEM}.html", f"{STEM}.pdf", jar.name, "Retold-Player-Guide.html", "Retold-Player-Guide.pdf",
             "LICENSE", "LICENSE-CODE.md", "LICENSE-ASSETS.md", "ASSET_CREDITS.md", "BUILD.json", "START-HERE.txt"]
    if args.include_media:
        names += sorted(p.name for p in OUTPUT.iterdir() if p.is_file() and p.suffix.lower() in MEDIA_EXTENSIONS
                        and not p.name.startswith("page-preview-"))
    names = sorted(set(names))
    (OUTPUT / "SHA256SUMS").write_text("".join(f"{digest(OUTPUT / name)}  {name}\n" for name in names))
    names.append("SHA256SUMS")
    archive_path = ROOT / "output/Retold-for-Klei.zip"
    with zipfile.ZipFile(archive_path, "w", compression=zipfile.ZIP_DEFLATED) as archive:
        for name in sorted(names):
            archive.write(OUTPUT / name, "Retold-for-Klei/" + name)
    with zipfile.ZipFile(archive_path) as archive:
        if archive.testzip() is not None:
            raise ValueError("ZIP integrity check failed")
        if hashlib.sha256(archive.read("Retold-for-Klei/" + jar.name)).hexdigest() != identity["artifact_sha256"]:
            raise ValueError("Packaged JAR checksum differs from the original")
    print(f"Exported eight PDF pages; validated {links} local/external link references (external URLs not fetched).")
    print(f"Compared {identity['compiled_classes_compared']} compiled classes and {identity['source_resources_compared']} source resources with the JAR.")
    print(f"Guide: {pdf.relative_to(ROOT)}")
    print(f"Package: {archive_path.relative_to(ROOT)} ({archive_path.stat().st_size:,} bytes)")
    print("Introduction draft: output/klei-message.txt")


if __name__ == "__main__":
    main()
