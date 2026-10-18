"""Shared Chromium renderer: HTML/SVG -> transparent PNG."""
import pathlib
from playwright.sync_api import sync_playwright

def render_many(jobs, size=512):
    """jobs: list of (html, out_path, w, h)."""
    with sync_playwright() as p:
        browser = p.chromium.launch()
        for html, out, w, h in jobs:
            page = browser.new_page(viewport={"width": w, "height": h}, device_scale_factor=1)
            page.set_content(html, wait_until="load")
            pathlib.Path(out).parent.mkdir(parents=True, exist_ok=True)
            page.screenshot(path=str(out), omit_background=True, clip={"x": 0, "y": 0, "width": w, "height": h})
            page.close()
        browser.close()
