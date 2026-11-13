"""Offline PDF rendering for the companion; Qt handles text layout, pypdf adds navigation.

The shared HTML content is adapted to Qt's documented rich-text HTML subset:
https://doc.qt.io/qt-6/richtext-html-subset.html
No web engine, browser subprocess, game client, or display server is used.
"""

from html import escape
from html.parser import HTMLParser
import os
from pathlib import Path
import re
import tempfile


PRINT_CSS = """
body { color: #253b32; font-family: 'Atkinson Hyperlegible Next', 'DejaVu Sans'; }
p { margin-top: 0px; margin-bottom: 11px; line-height: 140%; }
h1 { font-family: 'DejaVu Serif'; color: #345e46; font-size: 64pt; margin-top: 0px; margin-bottom: 3px; }
h2 { font-family: 'DejaVu Serif'; color: #345e46; font-size: 27pt; margin-top: 0px; margin-bottom: 20px; }
h3 { color: #345e46; font-size: 12pt; margin-top: 18px; margin-bottom: 7px; }
.eyebrow { color: #a26444; font-size: 8pt; font-weight: bold; margin-bottom: 15px; }
.subtitle { font-family: 'DejaVu Serif'; color: #345e46; font-size: 21pt; margin-bottom: 28px; }
.edition { color: #657469; font-size: 8pt; margin-top: 16px; }
.caption { color: #657469; font-size: 7.8pt; line-height: 130%; margin-top: 6px; }
.closing { color: #345e46; margin-top: 20px; }
li { margin-bottom: 5px; }
table { margin-top: 8px; margin-bottom: 12px; }
th { background-color: #f0f4ea; color: #345e46; text-align: left; font-size: 9.5pt; }
td { font-size: 9.5pt; }
.note td { background-color: #f0f4ea; font-size: 9.5pt; }
a { color: #315f45; text-decoration: underline; }
pre { background-color: #f0f4ea; font-family: 'DejaVu Sans Mono'; font-size: 8pt; }
code { font-family: 'DejaVu Sans Mono'; font-size: 8.5pt; }
.recipes td { font-size: 8.5pt; }
"""


class PrintMarkup(HTMLParser):
    """Translate only the three layout widgets into Qt-supported tables."""

    def __init__(self):
        super().__init__()
        self.output = []
        self.stack = []

    def handle_starttag(self, tag, attributes):
        attrs = dict(attributes)
        category = attrs.get("class", "")
        parent = self.stack[-1][1] if self.stack else ""
        closing = f"</{tag}>"
        if tag == "div" and category in ("flow", "timeline", "recipe-grid"):
            css = "recipes" if category == "recipe-grid" else "flow"
            start, closing = f'<table class="{css}" width="100%" cellspacing="5" cellpadding="8"><tr>', "</tr></table>"
        elif parent in ("flow", "timeline", "recipe-grid"):
            start, closing = '<td valign="middle" bgcolor="#f0f4ea">', "</td>"
        elif tag == "aside":
            start, closing = '<table class="note" width="100%" cellspacing="0" cellpadding="10"><tr><td>', "</td></tr></table>"
        elif tag == "figure":
            start, closing = "<div>", "</div>"
        elif tag == "figcaption":
            start, closing = '<p class="caption">', "</p>"
        else:
            if tag == "table":
                attrs.update(width="100%", cellspacing="0", cellpadding="7", border="0")
            if tag == "img":
                attrs.update(width="416", height="234")
            start = "<" + tag + "".join(f' {key}="{escape(value or "", quote=True)}"' for key, value in attrs.items()) + ">"
            if tag == "img":
                start = '<p align="center">' + start + "</p>"
        self.output.append(start)
        if tag not in ("img", "br", "hr", "meta", "link"):
            self.stack.append((tag, category, closing))

    def handle_endtag(self, tag):
        if self.stack and self.stack[-1][0] == tag:
            self.output.append(self.stack.pop()[2])

    def handle_data(self, data):
        self.output.append(escape(data))


def anchor_rectangles(document, left, top, page_height):
    """Get hyperlink hit areas from actual text lines, including wrapped anchors."""
    block = document.begin()
    while block.isValid():
        bounds = document.documentLayout().blockBoundingRect(block)
        layout = block.layout()
        iterator = block.begin()
        while not iterator.atEnd():
            fragment = iterator.fragment()
            href = fragment.charFormat().anchorHref() if fragment.isValid() else ""
            if href:
                start = fragment.position() - block.position()
                end = start + fragment.length()
                for number in range(layout.lineCount()):
                    line = layout.lineAt(number)
                    first = max(start, line.textStart())
                    last = min(end, line.textStart() + line.textLength())
                    if first >= last:
                        continue
                    x1, x2 = line.cursorToX(first)[0], line.cursorToX(last)[0]
                    x = left + bounds.x() + min(x1, x2)
                    y = top + bounds.y() + line.y()
                    # PDF coordinates are points from the lower left; Qt uses 96-DPI pixels.
                    rect = (x * .75, (page_height - y - line.height()) * .75,
                            (x + abs(x2 - x1)) * .75, (page_height - y) * .75)
                    yield href, rect
            iterator += 1
        block = block.next()


def render_pdf(html, destination, version):
    os.environ.setdefault("QT_QPA_PLATFORM", "offscreen")
    from PySide6.QtCore import QMarginsF, QPointF, QRectF, Qt
    from PySide6.QtGui import QColor, QFont, QGuiApplication, QPainter, QPageSize, QPdfWriter, QTextCursor, QTextDocument, QTextFormat
    from pypdf import PdfReader, PdfWriter
    from pypdf.annotations import Link

    application = QGuiApplication.instance() or QGuiApplication([])
    pages = re.findall(r'<section class="sheet" id="([^"]+)">.*?<div class="page-content">(.*?)</div><footer', html, re.S)
    if len(pages) != 8:
        raise ValueError("PDF renderer expected eight rendered guide sections")
    targets = {slug: number for number, (slug, _) in enumerate(pages)}
    with tempfile.TemporaryDirectory(prefix="retold-guide-pdf-") as temporary:
        raw_pdf = Path(temporary) / "guide.pdf"
        writer = QPdfWriter(str(raw_pdf))
        writer.setResolution(96)
        writer.setPageSize(QPageSize(QPageSize.A4))
        writer.setPageMargins(QMarginsF(0, 0, 0, 0))
        writer.setTitle("Retold — A living-world field guide")
        writer.setCreator("Retold documentation exporter · Qt rich text")
        painter = QPainter(writer)
        links = []
        left, top = 64, 101
        body_width, body_height = writer.width() - left * 2, writer.height() - top - 84
        try:
            for index, (slug, content) in enumerate(pages):
                if index:
                    writer.newPage()
                painter.fillRect(QRectF(0, 0, writer.width(), writer.height()), QColor("#fffefa"))
                painter.setPen(QColor("#657469"))
                painter.setFont(QFont("DejaVu Sans", 7))
                painter.drawText(QPointF(left, 56), "RETOLD")
                painter.drawText(QRectF(left, 41, body_width, 20), Qt.AlignRight, "A LIVING-WORLD FIELD GUIDE")
                painter.setPen(QColor("#d3dccf"))
                painter.drawLine(left, 70, writer.width() - left, 70)
                markup = PrintMarkup()
                markup.feed(content)
                document = QTextDocument()
                document.documentLayout().setPaintDevice(writer)
                document.setDocumentMargin(0)
                body_font = QFont("Atkinson Hyperlegible Next")
                body_font.setPointSizeF(9.6 if slug in ("getting-started", "playground", "further-exploration") else 10.1)
                document.setDefaultFont(body_font)
                document.setDefaultStyleSheet(PRINT_CSS)
                document.setHtml("".join(markup.output))
                block = document.begin()
                while block.isValid():
                    level = block.blockFormat().headingLevel()
                    if level in (1, 2):
                        cursor = QTextCursor(block)
                        cursor.select(QTextCursor.BlockUnderCursor)
                        format = cursor.charFormat()
                        format.clearProperty(QTextFormat.FontSizeAdjustment)
                        format.setFontPointSize(64 if level == 1 else 27)
                        cursor.setCharFormat(format)
                    block = block.next()
                document.setTextWidth(body_width)
                if document.size().height() > body_height:
                    raise ValueError(f"PDF page {index + 1} ({slug}) overflows: {document.size().height():.1f} > {body_height:.1f} pixels")
                links += [(index, href, rect) for href, rect in anchor_rectangles(document, left, top, writer.height())]
                painter.save()
                painter.translate(left, top)
                document.drawContents(painter)
                painter.restore()
                painter.setPen(QColor("#d3dccf"))
                painter.drawLine(left, writer.height() - 66, writer.width() - left, writer.height() - 66)
                painter.setPen(QColor("#657469"))
                painter.setFont(QFont("DejaVu Sans", 7))
                painter.drawText(QPointF(left, writer.height() - 43), f"XEFENSOR · {version} ALPHA")
                painter.drawText(QRectF(left, writer.height() - 57, body_width, 20), Qt.AlignRight, f"{index + 1:02d} / 08")
                print(f"PDF page {index + 1}: {document.size().height():.0f}/{body_height} pixels")
        finally:
            painter.end()
        output = PdfWriter(clone_from=PdfReader(raw_pdf))
        # Qt emits unresolved named destinations for manual page frames. Replace its
        # link annotations with rectangles tied to our actual eight-page mapping.
        output.remove_annotations(subtypes="/Link")
        for index, href, rect in links:
            annotation = Link(rect=rect, target_page_index=targets[href[1:]]) if href.startswith("#") else Link(rect=rect, url=href)
            output.add_annotation(index, annotation)
        for title, index in targets.items():
            output.add_outline_item(f"{index + 1:02d} · {title.replace('-', ' ').title()}", index)
        output.add_metadata({"/Title": "Retold — A living-world field guide", "/Author": "Xefensor"})
        output.write(destination)
    # Retain the Qt application until after the documents and paint devices have finished.
    _ = application
