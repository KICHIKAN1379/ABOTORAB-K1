"""Eight ornate circular frames (512x512, transparent). Hole radius is exactly R_IN."""
import math, random

R_IN = 190  # transparent opening radius (px of 512)

def ring(r_in, r_out, bg, extra=""):
    mask = f"radial-gradient(circle at 50% 50%, transparent 0 {r_in}px, #000 {r_in+0.8}px {r_out-0.8}px, transparent {r_out}px)"
    return f'<div style="position:absolute;inset:0;background:{bg};-webkit-mask:{mask};mask:{mask};{extra}"></div>'

def page(body, glow=None):
    flt = f"filter:drop-shadow(0 0 7px {glow});" if glow else "filter:drop-shadow(0 3px 4px rgba(0,0,0,.35));"
    hole = f"radial-gradient(circle at 50% 50%, transparent 0 {R_IN-0.5}px, #000 {R_IN+0.5}px)"
    return f'''<!doctype html><html><body style="margin:0;background:transparent">
    <div style="position:relative;width:512px;height:512px;-webkit-mask:{hole};mask:{hole}">
      <div style="position:absolute;inset:0;{flt}">{body}</div>
    </div></body></html>'''

def svg(inner, defs=""):
    return f'<svg style="position:absolute;inset:0" xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><defs>{defs}</defs>{inner}</svg>'

def polar(r, deg):
    a = math.radians(deg - 90)
    return 256 + r * math.cos(a), 256 + r * math.sin(a)

def gem(x, y, s, c1, c2, rot=0):
    return f'''<g transform="translate({x:.1f} {y:.1f}) rotate({rot})">
      <path d="M0 {-s} L{s*.8:.1f} 0 L0 {s} L{-s*.8:.1f} 0Z" fill="url(#g_{c1[1:]})" stroke="#fff" stroke-opacity=".75" stroke-width="1.6"/>
      <path d="M0 {-s} L{s*.8:.1f} 0 L0 0Z" fill="#fff" opacity=".4"/><path d="M{-s*.8:.1f} 0 L0 0 L0 {s}Z" fill="#000" opacity=".12"/></g>'''

def gem_defs(*cols):
    out = ""
    for c1, c2 in cols:
        out += f'<linearGradient id="g_{c1[1:]}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></linearGradient>'
    return out

def bead(x, y, r, c1, c2):
    return f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="url(#bd_{c1[1:]})"/><circle cx="{x-r*.3:.1f}" cy="{y-r*.35:.1f}" r="{r*.32:.1f}" fill="#fff" opacity=".85"/>'

def bead_defs(*cols):
    return "".join(f'<radialGradient id="bd_{c1[1:]}" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="{c1}"/><stop offset="1" stop-color="{c2}"/></radialGradient>' for c1, c2 in cols)

def star5(x, y, s, fill, rot=0, op=1):
    pts = []
    for i in range(10):
        r = s if i % 2 == 0 else s * .42
        a = math.radians(-90 + i * 36 + rot)
        pts.append(f"{x + r*math.cos(a):.1f},{y + r*math.sin(a):.1f}")
    return f'<polygon points="{" ".join(pts)}" fill="{fill}" opacity="{op}"/>'

def flare(x, y, s, color="#fff"):
    return f'<path d="M{x} {y-s} Q{x} {y} {x+s} {y} Q{x} {y} {x} {y+s} Q{x} {y} {x-s} {y} Q{x} {y} {x} {y-s}Z" fill="{color}"/>'

# 0 ─ فیروزه‌ای: neon cyan
def f0():
    band = ring(R_IN, 228, "conic-gradient(from 200deg, #1EFCE5, #37A9FF, #6C7BFF, #37A9FF, #1EFCE5, #9BFFF3, #1EFCE5)")
    inner = ring(R_IN, R_IN + 7, "linear-gradient(135deg,#ffffffcc,#1EFCE5aa 50%,#0A3E86aa)")
    outer = ring(220, 228, "linear-gradient(315deg,#ffffffcc,#7FF7EE 40%,#1C6FD6)")
    mid = ring(R_IN + 14, R_IN + 17, "rgba(255,255,255,.55)")
    mid2 = ring(R_IN + 32, R_IN + 34, "rgba(8,40,110,.45)")
    dots = "".join(f'<circle cx="{polar(209,i*15)[0]:.1f}" cy="{polar(209,i*15)[1]:.1f}" r="{3.2 if i%2 else 4.4}" fill="#fff" opacity="{.95 if i%2==0 else .6}"/>' for i in range(24))
    gems = "".join(gem(*polar(209, d), 10, "#7FF7EE", "#1B7BE8", rot=d) for d in (0, 90, 180, 270))
    return page(band + inner + outer + mid + mid2 + svg(dots + gems, gem_defs(("#7FF7EE", "#1B7BE8"))), glow="#1EFCE5")

# 1 ─ طلایی: ornate gold
def f1():
    gold = "conic-gradient(from 10deg, #FFE9A0, #E7B53F, #8A5A12, #F6D36B, #FFF3C4, #C58F26, #7A4C0E, #E9BF55, #FFE9A0)"
    band = ring(R_IN, 232, gold)
    bevel_in = ring(R_IN, R_IN + 9, "linear-gradient(160deg,#6E430B,#C48A22 40%,#FFF0B8)")
    bevel_out = ring(222, 232, "linear-gradient(340deg,#FFF3C4,#D9A53A 50%,#6E430B)")
    groove = ring(R_IN + 20, R_IN + 24, "rgba(90,52,8,.55)")
    hi = ring(R_IN + 26, R_IN + 28, "rgba(255,246,206,.8)")
    beads = "".join(bead(*polar(211, i*30), 7.5, "#FFF6CF", "#C98C1E") for i in range(12))
    leaves = ""
    for i in range(24):
        d = i * 15 + 7.5
        x, y = polar(236, d)
        leaves += f'<g transform="translate({x:.1f} {y:.1f}) rotate({d})"><path d="M0 0 C-7 -8 -6 -18 0 -22 C6 -18 7 -8 0 0Z" fill="url(#lf)"/><path d="M0 -3 L0 -17" stroke="#7A4C0E" stroke-opacity=".5" stroke-width="1.2"/></g>'
    top = gem(256, 22, 17, "#7FD8FF", "#1F6FD8") 
    defs = bead_defs(("#FFF6CF", "#C98C1E")) + gem_defs(("#7FD8FF", "#1F6FD8")) + '<linearGradient id="lf" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#FFE9A0"/><stop offset="1" stop-color="#B07A18"/></linearGradient>'
    return page(band + bevel_in + bevel_out + groove + hi + svg(leaves + beads + top, defs))

# 2 ─ ارغوانی: crystal facets
def f2():
    facets = "repeating-conic-gradient(from 0deg, #7B5CF0 0 11.25deg, #B9A2FF 11.25deg 22.5deg, #5C3FCB 22.5deg 33.75deg, #A58AFF 33.75deg 45deg)"
    band = ring(R_IN, 230, facets)
    sheen = ring(R_IN, 230, "linear-gradient(135deg, rgba(255,255,255,.55), rgba(255,255,255,0) 45%, rgba(20,0,80,.35))")
    inner = ring(R_IN, R_IN + 5, "linear-gradient(135deg,#fff,#CDBBFF 50%,#5B3FD0)")
    outer = ring(223, 230, "linear-gradient(315deg,#fff,#D8CBFF 40%,#4A2FBF)")
    mid = ring(R_IN + 18, R_IN + 20, "rgba(255,255,255,.6)")
    lines = "".join(f'<line x1="{polar(R_IN+5,i*22.5)[0]:.1f}" y1="{polar(R_IN+5,i*22.5)[1]:.1f}" x2="{polar(223,i*22.5)[0]:.1f}" y2="{polar(223,i*22.5)[1]:.1f}" stroke="#fff" stroke-opacity=".35" stroke-width="1.4"/>' for i in range(16))
    gems = "".join(gem(*polar(207, d), 11, "#F2EAFF", "#8E6BFF", rot=d) for d in range(0, 360, 90))
    return page(band + sheen + inner + outer + mid + svg(lines + gems, gem_defs(("#F2EAFF", "#8E6BFF"))), glow="#B197FF")

# 3 ─ مرجانی: coral rope
def f3():
    base = ring(R_IN, 228, "linear-gradient(135deg,#FF9A78,#E0533A 60%,#A93220)")
    segs = ""
    n = 56
    for i in range(n):
        d = i * 360 / n
        x, y = polar(209, d)
        col = "url(#rp1)" if i % 2 == 0 else "url(#rp2)"
        segs += f'<g transform="translate({x:.1f} {y:.1f}) rotate({d+28})"><rect x="-6" y="-24" width="12" height="48" rx="6" fill="{col}"/><rect x="-2.5" y="-20" width="3" height="30" rx="1.5" fill="#fff" opacity=".35"/></g>'
    inner = ring(R_IN, R_IN + 5, "linear-gradient(135deg,#FFD9CC,#E0533A 60%,#8A2414)")
    outer = ring(222, 228, "linear-gradient(315deg,#FFE0D4,#F27A5E 50%,#8A2414)")
    pearls = "".join(bead(*polar(233, d), 6.5, "#FFFFFF", "#E8C9BD") for d in range(0, 360, 30))
    defs = '<linearGradient id="rp1" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#FF8B6A"/><stop offset="1" stop-color="#C73A22"/></linearGradient><linearGradient id="rp2" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#FFC1A8"/><stop offset="1" stop-color="#F0694C"/></linearGradient>' + bead_defs(("#FFFFFF", "#E8C9BD"))
    return page(base + svg(segs, defs) + inner + outer + svg(pearls, defs))

# 4 ─ زمردی: emerald with leaves
def f4():
    band = ring(R_IN, 224, "conic-gradient(from 40deg, #7CF2B4, #19A56A, #0B6B45, #3ED08E, #A8FFD0, #12875A, #0A5C3B, #34C98A, #7CF2B4)")
    inner = ring(R_IN, R_IN + 7, "linear-gradient(135deg,#E8FFF3,#3ED08E 50%,#0A5C3B)")
    outer = ring(216, 224, "linear-gradient(315deg,#DFFFEF,#4FD99C 50%,#0A5C3B)")
    gold_in = ring(R_IN + 8, R_IN + 11, "linear-gradient(135deg,#FFF1B8,#D9A53A)")
    leaves = ""
    for i in range(32):
        d = i * 360 / 32
        x, y = polar(224, d)
        L = 26 if i % 2 == 0 else 20
        leaves += f'<g transform="translate({x:.1f} {y:.1f}) rotate({d})"><path d="M0 2 C-9 -6 -8 -{L-4} 0 -{L} C8 -{L-4} 9 -6 0 2Z" fill="url(#el)"/><path d="M0 0 L0 -{L-5}" stroke="#08452D" stroke-opacity=".5" stroke-width="1.3"/></g>'
    gems = "".join(gem(*polar(207, d), 13, "#B5FFD8", "#0E9A60", rot=d) for d in (0, 90, 180, 270)) + "".join(bead(*polar(207, d), 4.5, "#FFF1B8", "#D9A53A") for d in range(45, 360, 90))
    defs = '<linearGradient id="el" x1="0" y1="1" x2="0" y2="0"><stop offset="0" stop-color="#0E8A58"/><stop offset="1" stop-color="#7CF2B4"/></linearGradient>' + gem_defs(("#B5FFD8", "#0E9A60")) + bead_defs(("#FFF1B8", "#D9A53A"))
    return page(svg(leaves, defs) + band + inner + outer + gold_in + svg(gems, defs), glow="#58E39A")

# 5 ─ ستاره‌ای: night sky
def f5():
    band = ring(R_IN, 228, "linear-gradient(160deg,#3A4DD8,#1B1F7A 55%,#0A0F45)")
    glow = ring(R_IN, 228, "radial-gradient(circle at 50% 50%, rgba(125,160,255,.0) 0 195px, rgba(125,160,255,.35) 210px, rgba(125,160,255,0) 228px)")
    inner = ring(R_IN, R_IN + 5, "linear-gradient(135deg,#FFF3C4,#E7B53F 55%,#8A5A12)")
    outer = ring(222, 228, "linear-gradient(315deg,#FFF3C4,#E7B53F 55%,#8A5A12)")
    r = random.Random(7)
    tiny = ""
    for _ in range(90):
        rr = r.uniform(R_IN + 9, 220); d = r.uniform(0, 360); x, y = polar(rr, d)
        tiny += f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r.uniform(.8,2.2):.1f}" fill="#fff" opacity="{r.uniform(.4,.95):.2f}"/>'
    big = "".join(star5(*polar(209, d), 13 if i % 2 == 0 else 9, "#FFD66B", rot=d) for i, d in enumerate(range(0, 360, 45)))
    fl = flare(*polar(209, 22.5), 8, "#fff") + flare(*polar(209, 202.5), 8, "#fff") + flare(*polar(209, 112.5), 6, "#BFD3FF") + flare(*polar(209, 292.5), 6, "#BFD3FF")
    return page(band + glow + inner + outer + svg(tiny + big + fl), glow="#7DA0FF")

# 6 ─ الماسی: silver + diamonds
def f6():
    silver = "conic-gradient(from 0deg, #FFFFFF, #AEB9CF, #EEF3FF, #7E8AA6, #FFFFFF, #B9C6DE, #F7FAFF, #8794B0, #FFFFFF)"
    band = ring(R_IN, 230, silver)
    inner = ring(R_IN, R_IN + 8, "linear-gradient(150deg,#6C789A,#E8EEFF 50%,#fff)")
    outer = ring(220, 230, "linear-gradient(330deg,#fff,#B7C3DB 50%,#66728F)")
    groove = ring(R_IN + 18, R_IN + 21, "rgba(80,96,130,.45)")
    small = "".join(gem(*polar(210, d), 9, "#FFFFFF", "#8FD3FF", rot=d) for d in range(0, 360, 15) if d % 90)
    big = "".join(gem(*polar(210, d), 17, "#E8FAFF", "#47B7FF", rot=d) for d in range(0, 360, 90))
    fl = "".join(flare(*polar(210, d + 8), 10, "#fff") for d in range(0, 360, 90))
    return page(band + inner + outer + groove + svg(small + big + fl, gem_defs(("#FFFFFF", "#8FD3FF"), ("#E8FAFF", "#47B7FF"))), glow="#9FD8FF")

# 7 ─ قله: mountain-peak ring with flag
def f7():
    band = ring(R_IN, 214, "linear-gradient(160deg,#2E7BD6,#0F3E86 60%,#06224F)")
    inner = ring(R_IN, R_IN + 6, "linear-gradient(135deg,#E8F6FF,#5BB5FF 55%,#0B3A80)")
    outer = ring(208, 214, "linear-gradient(315deg,#E8F6FF,#7CC6FF 50%,#0B3A80)")
    snowline = ring(R_IN + 14, R_IN + 17, "rgba(255,255,255,.55)")
    peaks = ""
    spec = [(-100, 30, 0.9), (-80, 38, 1.1), (-62, 30, .9), (-42, 40, 1.15), (-22, 32, .95), (0, 52, 1.4), (22, 32, .95), (42, 40, 1.15), (62, 30, .9), (80, 38, 1.1), (100, 30, .9)]
    for d, h, w in spec:
        x, y = polar(212, d)
        peaks += f'<g transform="translate({x:.1f} {y:.1f}) rotate({d})"><path d="M{-22*w:.1f} 4 L0 {-h} L{22*w:.1f} 4Z" fill="url(#mt)"/><path d="M0 {-h} L{-8*w:.1f} {-h+14} L{-2*w:.1f} {-h+11} L{2*w:.1f} {-h+17} L{8*w:.1f} {-h+14}Z" fill="#fff"/></g>'
    fx, fy = polar(212, 0)
    flag = f'<g transform="translate({fx:.1f} {fy-52:.1f})"><rect x="-1.5" y="-26" width="3" height="28" fill="#6B4A36"/><path d="M1.5 -26 L24 -19 L1.5 -12Z" fill="url(#fg)"/></g>'
    base = "".join(bead(*polar(197, d), 4, "#FFF6CF", "#E0A32E") for d in range(120, 250, 18))
    defs = '<linearGradient id="mt" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#8FD0FF"/><stop offset="1" stop-color="#1857B0"/></linearGradient><linearGradient id="fg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#FF9A5C"/><stop offset="1" stop-color="#E0402B"/></linearGradient>' + bead_defs(("#FFF6CF", "#E0A32E"))
    return page(svg(peaks, defs) + band + inner + outer + snowline + svg(flag + base, defs), glow="#5BB5FF")

FRAMES = [f0, f1, f2, f3, f4, f5, f6, f7]
