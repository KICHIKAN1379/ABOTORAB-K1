"""Launcher icon layers (adaptive, 432x432 = 108dp @4x). Safe zone is the centre 288px."""
import sys, pathlib, random
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from render import render_many

def stars(seed, n):
    r = random.Random(seed); out = []
    for _ in range(n):
        x, y = r.uniform(20, 412), r.uniform(20, 200)
        out.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r.uniform(.9,2.4):.1f}" fill="#fff" opacity="{r.uniform(.35,.9):.2f}"/>')
    return "".join(out)

def shell(inner, defs=""):
    return f'<!doctype html><html><body style="margin:0;background:transparent"><svg xmlns="http://www.w3.org/2000/svg" width="432" height="432" viewBox="0 0 432 432"><defs>{defs}</defs>{inner}</svg></body></html>'

BG = shell(f'''
  <rect width="432" height="432" fill="url(#bg)"/>
  <circle cx="360" cy="70" r="190" fill="#1EFCE5" opacity=".20" filter="url(#b60)"/>
  <circle cx="60" cy="400" r="190" fill="#B197FF" opacity=".28" filter="url(#b60)"/>
  {stars(11, 40)}''',
  '''<linearGradient id="bg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#021E43"/><stop offset=".55" stop-color="#0A3C94"/><stop offset="1" stop-color="#011D59"/></linearGradient>
  <filter id="b60" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="46"/></filter>''')

MOUNTAIN = "M86 330 L196 138 L226 182 L246 160 L346 330 Z"
MAIN = "M72 340 L216 112 L252 168 L276 142 L360 340 Z"

FG = shell(f'''
  <circle cx="216" cy="152" r="78" fill="#FF9F43" opacity=".42" filter="url(#b14)"/>
  <circle cx="216" cy="152" r="42" fill="#FFD66B"/>
  <circle cx="204" cy="140" r="16" fill="#FFF3C4" opacity=".7"/>
  <!-- far range -->
  <path d="M60 340 L128 224 L170 290 L214 232 L262 310 L304 252 L372 340Z" fill="#12489C" opacity=".85"/>
  <!-- main mountain -->
  <path d="{MAIN}" fill="url(#mt)"/>
  <path d="M216 112 L252 168 L276 142 L360 340 L300 340 L252 224Z" fill="#06224F" opacity=".38"/>
  <!-- snow cap -->
  <path d="M216 112 L180 168 L198 160 L212 182 L226 158 L244 176 L252 168Z" fill="#fff"/>
  <path d="M216 112 L252 168 L244 176 L226 158Z" fill="#CFE9FF" opacity=".8"/>
  <!-- ground -->
  <path d="M0 338 C80 322 150 348 216 340 C290 331 346 320 432 338 L432 432 L0 432Z" fill="url(#gr)"/>
  <path d="M0 338 C80 322 150 348 216 340 C290 331 346 320 432 338" fill="none" stroke="#7FF7EE" stroke-opacity=".5" stroke-width="2"/>
  <!-- neon trail -->
  <path d="M222 346 C150 322 270 296 188 262 C130 238 236 226 214 188" fill="none" stroke="#1EFCE5" stroke-width="15" stroke-linecap="round" opacity=".55" filter="url(#b6)"/>
  <path d="M222 346 C150 322 270 296 188 262 C130 238 236 226 214 188" fill="none" stroke="url(#tr)" stroke-width="9" stroke-linecap="round"/>
  <path d="M222 346 C150 322 270 296 188 262 C130 238 236 226 214 188" fill="none" stroke="#fff" stroke-width="2.4" stroke-linecap="round" opacity=".7"/>
  <!-- flag -->
  <rect x="213.4" y="64" width="4.2" height="52" rx="2" fill="#E9D7B8"/>
  <path d="M217 66 L262 80 L217 96Z" fill="url(#fg)"/>
  <circle cx="215.5" cy="62" r="5" fill="#FFD66B"/>
  <!-- sparkle -->
  <path d="M310 118 Q310 128 320 128 Q310 128 310 138 Q310 128 300 128 Q310 128 310 118Z" fill="#fff"/>
  <path d="M120 150 Q120 156 126 156 Q120 156 120 162 Q120 156 114 156 Q120 156 120 150Z" fill="#fff" opacity=".85"/>
''', '''<linearGradient id="mt" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#7CC6FF"/><stop offset=".5" stop-color="#2F6FE0"/><stop offset="1" stop-color="#0B2E7A"/></linearGradient>
  <linearGradient id="gr" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#0B3A8E"/><stop offset="1" stop-color="#021E43"/></linearGradient>
  <linearGradient id="tr" x1="0" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#1EFCE5"/><stop offset="1" stop-color="#8BFFF1"/></linearGradient>
  <linearGradient id="fg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FFB071"/><stop offset="1" stop-color="#E8432C"/></linearGradient>
  <filter id="b14" x="-60%" y="-60%" width="220%" height="220%"><feGaussianBlur stdDeviation="14"/></filter>
  <filter id="b6" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="6"/></filter>''')

MONO = shell(f'''
  <mask id="cut"><rect width="432" height="432" fill="#fff"/><path d="M222 346 C150 322 270 296 188 262 C130 238 236 226 214 188" fill="none" stroke="#000" stroke-width="12" stroke-linecap="round"/></mask>
  <g mask="url(#cut)"><path d="{MAIN}" fill="#000"/></g>
  <rect x="213.4" y="64" width="4.2" height="52" rx="2" fill="#000"/><path d="M217 66 L262 80 L217 96Z" fill="#000"/>''')

if __name__ == "__main__":
    out = pathlib.Path(sys.argv[1])
    render_many([(BG, out / "ic_launcher_background.png", 432, 432), (FG, out / "ic_launcher_foreground.png", 432, 432), (MONO, out / "ic_launcher_monochrome.png", 432, 432)])
    print("icon layers ->", out)
