"""Six illustrated starter avatars (512x512, full-bleed; the app crops them into the frame's circle)."""
import math, random

SKIN = {
    "light": ("#FFE3CC", "#F6C29C", "#D99A72"),
    "warm":  ("#F9D2AE", "#E7A978", "#BF7C52"),
    "tan":   ("#EBBE93", "#CF9566", "#A8693F"),
}

def defs_common():
    return """
    <filter id="soft" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="9"/></filter>
    <filter id="soft2" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="3.2"/></filter>
    <filter id="soft3" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="16"/></filter>
    """

def grad(id, stops, x1=0, y1=0, x2=0, y2=1, radial=False, cx=.5, cy=.4, r=.7):
    s = "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in stops)
    if radial:
        return f'<radialGradient id="{id}" cx="{cx}" cy="{cy}" r="{r}">{s}</radialGradient>'
    return f'<linearGradient id="{id}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">{s}</linearGradient>'

def face(skin, expr="smile", brow="#2A1F1C", lash=False, cheeks=True, cx=256, cy=244, scale=1.0, eye="#2B2020"):
    l, m, d = SKIN[skin]
    eyes = f'''
      <g transform="translate({cx},{cy}) scale({scale})">
        <!-- cheeks -->
        {'<ellipse cx="-62" cy="30" rx="22" ry="13" fill="#FF7A8A" opacity=".28" filter="url(#soft2)"/><ellipse cx="62" cy="30" rx="22" ry="13" fill="#FF7A8A" opacity=".28" filter="url(#soft2)"/>' if cheeks else ''}
        <!-- eyes -->
        <ellipse cx="-38" cy="0" rx="13" ry="16" fill="{eye}"/>
        <ellipse cx="38" cy="0" rx="13" ry="16" fill="{eye}"/>
        <circle cx="-42" cy="-6" r="5.2" fill="#fff"/><circle cx="34" cy="-6" r="5.2" fill="#fff"/>
        <circle cx="-33" cy="6" r="2.4" fill="#fff" opacity=".8"/><circle cx="43" cy="6" r="2.4" fill="#fff" opacity=".8"/>
        {'<path d="M-54 -10 q-6 -4 -10 -10 M-52 -4 q-8 -1 -13 -5 M54 -10 q6 -4 10 -10 M52 -4 q8 -1 13 -5" stroke="'+eye+'" stroke-width="3.2" fill="none" stroke-linecap="round"/>' if lash else ''}
        <!-- brows -->
        <path d="M-58 -30 q20 -14 40 -4" stroke="{brow}" stroke-width="7.5" fill="none" stroke-linecap="round"/>
        <path d="M58 -30 q-20 -14 -40 -4" stroke="{brow}" stroke-width="7.5" fill="none" stroke-linecap="round"/>
        <!-- nose -->
        <path d="M-5 22 q5 10 10 0" stroke="{d}" stroke-width="4.5" fill="none" stroke-linecap="round" opacity=".8"/>
        <!-- mouth -->
        {'<path d="M-24 52 q24 26 48 0" stroke="#9B3B3B" stroke-width="5.5" fill="none" stroke-linecap="round"/><path d="M-17 55 q17 16 34 0 q-17 7 -34 0z" fill="#fff" opacity=".9"/>' if expr=="smile" else '<path d="M-20 56 q20 14 40 0" stroke="#9B3B3B" stroke-width="5.5" fill="none" stroke-linecap="round"/>'}
      </g>'''
    return eyes

def head(skin, cx=256, cy=236, rx=94, ry=108, ears=True, uid="h"):
    l, m, d = SKIN[skin]
    g = grad(f"head{uid}", [(0, l), (.65, m), (1, d)], radial=True, cx=.38, cy=.32, r=.85)
    earsvg = f'''<ellipse cx="{cx-rx+4}" cy="{cy+16}" rx="17" ry="25" fill="{m}"/><ellipse cx="{cx+rx-4}" cy="{cy+16}" rx="17" ry="25" fill="{m}"/>
    <ellipse cx="{cx-rx+6}" cy="{cy+18}" rx="8" ry="14" fill="{d}" opacity=".5"/><ellipse cx="{cx+rx-6}" cy="{cy+18}" rx="8" ry="14" fill="{d}" opacity=".5"/>''' if ears else ""
    return g, earsvg + f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="url(#head{uid})"/>'

def neck_torso(skin, cloth_a, cloth_b, collar="#fff", v=True, uid="t", neck_y=330):
    l, m, d = SKIN[skin]
    return f'''
    <defs>{grad(f"cloth{uid}", [(0, cloth_a), (1, cloth_b)])}{grad(f"neck{uid}", [(0, d), (1, m)])}</defs>
    <path d="M30 530 C40 420 140 366 256 366 C372 366 472 420 482 530 Z" fill="url(#cloth{uid})"/>
    <path d="M30 530 C40 420 140 366 256 366 C372 366 472 420 482 530 Z" fill="none" stroke="#fff" stroke-opacity=".18" stroke-width="3"/>
    <rect x="216" y="300" width="80" height="90" rx="36" fill="url(#neck{uid})"/>
    <ellipse cx="256" cy="344" rx="56" ry="26" fill="#000" opacity=".16" filter="url(#soft2)"/>
    {f'<path d="M212 368 L256 440 L300 368 Z" fill="{m}"/><path d="M200 366 L252 446 L232 380 Z" fill="{collar}" opacity=".95"/><path d="M312 366 L260 446 L280 380 Z" fill="{collar}" opacity=".8"/>' if v else ''}
    '''

def gloss():
    return '<ellipse cx="150" cy="86" rx="190" ry="96" fill="url(#glossg)" transform="rotate(-18 150 86)"/>'

def wrap(inner, bg_defs, bg, extra_defs=""):
    return f'''<!doctype html><html><body style="margin:0;background:transparent">
    <svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512">
      <defs>{defs_common()}{bg_defs}{extra_defs}
        <linearGradient id="glossg" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".38"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
      </defs>
      {bg}
      {inner}
      <rect width="512" height="512" fill="url(#vig)" />
    </svg></body></html>'''

VIG = '<radialGradient id="vig" cx=".5" cy=".5" r=".75"><stop offset=".62" stop-color="#000" stop-opacity="0"/><stop offset="1" stop-color="#000" stop-opacity=".28"/></radialGradient>'

def mountains(base, snow, y=360, scale=1.0, op=1.0, shift=0):
    return f'''<g opacity="{op}" transform="translate({shift},0)">
      <path d="M-40 {y+150} L110 {y-120*scale} L190 {y-30*scale} L270 {y-170*scale} L380 {y-20*scale} L440 {y-90*scale} L560 {y+150} Z" fill="{base}"/>
      <path d="M110 {y-120*scale} l-34 54 l22 -10 l14 22 l14 -22 l20 12z" fill="{snow}" opacity=".95"/>
      <path d="M270 {y-170*scale} l-44 70 l28 -12 l16 26 l16 -26 l26 14z" fill="{snow}" opacity=".95"/>
    </g>'''

def stars(seed, n, color="#fff", box=(0, 0, 512, 260)):
    r = random.Random(seed)
    out = []
    for _ in range(n):
        x = r.uniform(box[0], box[2]); y = r.uniform(box[1], box[3]); s = r.uniform(1.5, 4.2)
        out.append(f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{s:.1f}" fill="{color}" opacity="{r.uniform(.35,.95):.2f}"/>')
    return "".join(out)

def sparkle(x, y, s, color="#fff", op=1):
    return f'<path d="M{x} {y-s} Q{x} {y} {x+s} {y} Q{x} {y} {x} {y+s} Q{x} {y} {x-s} {y} Q{x} {y} {x} {y-s}Z" fill="{color}" opacity="{op}"/>'

# ---------------------------------------------------------------- 0 رهرو (traveler boy)
def av0():
    bg_defs = grad("bg", [(0, "#8CF3E8"), (.55, "#1FB5B5"), (1, "#087F8C")], radial=True, cx=.3, cy=.2, r=1.1) + VIG
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>
      <circle cx="400" cy="130" r="64" fill="#FFF3C4" opacity=".9"/><circle cx="400" cy="130" r="92" fill="#FFF3C4" opacity=".25" filter="url(#soft)"/>
      {mountains("#0B5E73", "#EAFDFF", 380, 1.0, .75, -20)}
      {mountains("#0E3F62", "#CFEFFF", 430, .8, .95, 70)}'''
    gh, headsvg = head("light", uid="0")
    hair = '''<defs>''' + grad("hair0", [(0, "#4A3A33"), (1, "#1E1512")]) + '''</defs>
      <path d="M156 236 C140 96 372 84 358 236 C338 180 300 160 252 162 C206 164 170 190 156 236Z" fill="url(#hair0)"/>
      <path d="M180 150 C220 112 300 112 336 152 C298 134 226 134 180 150Z" fill="#fff" opacity=".22"/>
      <path d="M156 236 C150 200 160 176 176 160 C170 190 168 214 172 244Z" fill="#1E1512"/>
      <path d="M358 236 C364 200 352 176 338 160 C342 190 344 214 342 244Z" fill="#1E1512"/>'''
    body = neck_torso("light", "#5C73C8", "#243068", collar="#F2F6FF", uid="0")
    straps = '<path d="M168 392 C176 450 180 500 176 530 L212 530 C214 480 214 440 208 380Z M344 392 C336 450 332 500 336 530 L300 530 C298 480 298 440 304 380Z" fill="#FF9A5C" opacity=".95"/><rect x="190" y="470" width="22" height="16" rx="4" fill="#FFD09A"/><rect x="300" y="470" width="22" height="16" rx="4" fill="#FFD09A"/>'
    inner = body + straps + f'<defs>{gh}</defs>' + headsvg + face("light", "smile", brow="#2A1F1C") + hair + gloss()
    return wrap(inner, bg_defs, bg)

# ---------------------------------------------------------------- 1 پیش‌قدم (girl, coral scarf)
def av1():
    bg_defs = grad("bg", [(0, "#FFD3B8"), (.5, "#FF8F6B"), (1, "#C9502F")], radial=True, cx=.28, cy=.18, r=1.15) + VIG
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>
      <circle cx="110" cy="110" r="80" fill="#fff" opacity=".18"/><circle cx="430" cy="200" r="44" fill="#fff" opacity=".16"/><circle cx="420" cy="430" r="110" fill="#fff" opacity=".1"/>
      <path d="M0 440 C120 380 240 470 360 410 C430 376 480 396 512 410 L512 512 L0 512Z" fill="#8F2F1A" opacity=".35"/>
      {sparkle(86, 300, 18, "#fff", .9)}{sparkle(440, 96, 14, "#fff", .8)}{sparkle(60, 190, 9, "#fff", .8)}'''
    gh, headsvg = head("warm", uid="1", ears=False, rx=90, ry=104)
    scarf_defs = grad("sc1", [(0, "#35D6C6"), (1, "#0B7F86")], x1=0, y1=0, x2=1, y2=1) + grad("sc1b", [(0, "#2FC7B8"), (1, "#0A6B75")])
    back_scarf = f'<defs>{scarf_defs}</defs><path d="M120 250 C112 120 190 80 256 80 C322 80 400 120 392 250 C396 330 420 372 440 430 L72 430 C92 372 116 330 120 250Z" fill="url(#sc1b)"/>'
    front = '''<path d="M150 252 C140 112 372 112 362 252 C342 192 304 172 256 172 C208 172 170 192 150 252Z" fill="url(#sc1)"/>
      <path d="M174 184 C206 150 306 150 338 184" stroke="#fff" stroke-opacity=".28" stroke-width="7" fill="none" stroke-linecap="round"/>
      <path d="M150 252 C146 296 160 330 190 360 C176 322 170 290 174 256Z" fill="url(#sc1)"/>
      <path d="M362 252 C366 296 352 330 322 360 C336 322 342 290 338 256Z" fill="url(#sc1)"/>
      <path d="M200 178 q56 -30 112 0" stroke="#0A6B75" stroke-width="3" fill="none" opacity=".5"/>'''
    body = neck_torso("warm", "#4A5BB5", "#1C2658", collar="#FFFFFF", v=False, uid="1")
    pin = '<circle cx="256" cy="396" r="12" fill="#FFD66B"/><circle cx="252" cy="392" r="4" fill="#fff" opacity=".85"/>'
    inner = body + back_scarf + f'<defs>{gh}</defs>' + headsvg + face("warm", "smile", brow="#3A2418", lash=True, cy=250, scale=.98) + front + pin + gloss()
    # hide neck over scarf order: draw scarf bottom after torso
    return wrap(inner, bg_defs, bg)

# ---------------------------------------------------------------- 2 پژوهشگر (glasses, lab coat)
def av2():
    bg_defs = grad("bg", [(0, "#DCD1FF"), (.55, "#8A74E8"), (1, "#3E2E96")], radial=True, cx=.3, cy=.15, r=1.15) + VIG
    orbit = '<ellipse cx="256" cy="200" rx="230" ry="80" fill="none" stroke="#fff" stroke-opacity=".28" stroke-width="3" transform="rotate(-24 256 200)"/><ellipse cx="256" cy="200" rx="230" ry="80" fill="none" stroke="#fff" stroke-opacity=".2" stroke-width="3" transform="rotate(30 256 200)"/>'
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>{orbit}
      <circle cx="96" cy="116" r="12" fill="#FFE08A"/><circle cx="430" cy="300" r="9" fill="#7FF7EE"/><circle cx="410" cy="82" r="8" fill="#fff" opacity=".8"/>
      {sparkle(70, 250, 14, "#fff", .85)}{sparkle(452, 180, 18, "#FFE08A", .95)}'''
    gh, headsvg = head("tan", uid="2")
    hair = '''<defs>''' + grad("hair2", [(0, "#6B4A36"), (1, "#2D1E16")]) + '''</defs>
      <path d="M160 232 C150 104 362 96 352 232 C334 168 288 140 254 148 C218 156 176 172 160 232Z" fill="url(#hair2)"/>
      <path d="M170 164 C210 132 290 130 330 166 C290 150 214 152 170 164Z" fill="#fff" opacity=".2"/>'''
    body = neck_torso("tan", "#FFFFFF", "#D3D9EA", collar="#E8ECFA", uid="2")
    shirt = '<path d="M212 368 L256 450 L300 368 Z" fill="#5B8CFF"/><path d="M252 400 L256 380 L260 400 L256 470Z" fill="#2E58D6"/><rect x="170" y="440" width="34" height="46" rx="6" fill="#E3E8F7"/><rect x="176" y="436" width="22" height="8" rx="3" fill="#7FF7EE"/>'
    glasses = '''<g fill="none" stroke="#2D2A5C" stroke-width="7"><circle cx="218" cy="248" r="34" fill="#fff" fill-opacity=".14"/><circle cx="294" cy="248" r="34" fill="#fff" fill-opacity=".14"/><path d="M251 244 q5 -9 10 0" stroke-linecap="round"/><path d="M184 240 L164 232 M328 240 L348 232" stroke-linecap="round"/></g>
      <path d="M196 232 l20 -14 M272 232 l20 -14" stroke="#fff" stroke-width="5" stroke-linecap="round" opacity=".7"/>'''
    inner = body + shirt + f'<defs>{gh}</defs>' + headsvg + face("tan", "smile", brow="#2D1E16", cy=248) + hair + glasses + gloss()
    return wrap(inner, bg_defs, bg)

# ---------------------------------------------------------------- 3 قله‌نورد (mountaineer)
def av3():
    bg_defs = grad("bg", [(0, "#C8FFD9"), (.5, "#4CCB8A"), (1, "#176B49")], radial=True, cx=.3, cy=.15, r=1.2) + VIG
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>
      <circle cx="404" cy="118" r="54" fill="#FFF7C8" opacity=".95"/><circle cx="404" cy="118" r="86" fill="#FFF7C8" opacity=".3" filter="url(#soft)"/>
      {mountains("#1F7A5A", "#F3FFF8", 372, 1.05, .8, 30)}
      {mountains("#0F4F46", "#DFFFF1", 436, .75, 1, -60)}
      <path d="M0 470 C100 440 220 490 330 456 C410 432 470 444 512 456 L512 512 L0 512Z" fill="#0A3B34" opacity=".5"/>'''
    gh, headsvg = head("light", uid="3")
    body = neck_torso("light", "#FFB454", "#E0702A", collar="#FFF1D6", v=False, uid="3")
    jacket = '<path d="M256 372 L256 530" stroke="#B9531B" stroke-width="5"/><rect x="238" y="372" width="36" height="20" rx="6" fill="#FFE2B0"/><path d="M120 440 q-10 40 -4 90 M392 440 q10 40 4 90" stroke="#fff" stroke-opacity=".28" stroke-width="8" fill="none" stroke-linecap="round"/>'
    scarf = '<defs>' + grad("scf3", [(0, "#FF6A5C"), (1, "#C23A33")]) + '</defs><path d="M190 346 C220 380 292 380 322 346 L332 380 C290 410 222 410 180 380Z" fill="url(#scf3)"/><path d="M296 376 l30 78 l-34 8 l-18 -74Z" fill="url(#scf3)"/><path d="M200 358 q56 28 112 0" stroke="#fff" stroke-opacity=".3" stroke-width="5" fill="none"/>'
    beanie = '''<defs>''' + grad("bn3", [(0, "#FF6A5C"), (1, "#B8312B")]) + '''</defs>
      <path d="M158 218 C150 84 362 84 354 218 Z" fill="url(#bn3)"/>
      <rect x="150" y="196" width="212" height="46" rx="22" fill="#E8483B"/><rect x="150" y="196" width="212" height="46" rx="22" fill="none" stroke="#9E2A24" stroke-width="3"/>
      <path d="M176 200 v38 M196 198 v42 M216 197 v44 M236 196 v46 M256 196 v46 M276 196 v46 M296 197 v44 M316 198 v42 M336 200 v38" stroke="#B8312B" stroke-width="4" opacity=".75"/>
      <circle cx="256" cy="86" r="22" fill="#fff"/><circle cx="256" cy="86" r="22" fill="#E5E0D5" opacity=".6"/>
      <path d="M190 130 C220 108 292 108 322 130" stroke="#fff" stroke-opacity=".3" stroke-width="7" fill="none" stroke-linecap="round"/>'''
    goggles = '<rect x="188" y="236" width="136" height="40" rx="20" fill="#2B3A55" opacity=".0"/>'
    inner = body + jacket + f'<defs>{gh}</defs>' + headsvg + face("light", "smile", brow="#3A2A20", cy=258, scale=.98) + scarf + beanie + gloss()
    return wrap(inner, bg_defs, bg)

# ---------------------------------------------------------------- 4 ستاره (curly girl, neon night)
def av4():
    bg_defs = grad("bg", [(0, "#7A8CFF"), (.5, "#2B3FC6"), (1, "#0A1B5C")], radial=True, cx=.3, cy=.15, r=1.2) + VIG
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>
      <circle cx="400" cy="120" r="150" fill="#3DF6E6" opacity=".16" filter="url(#soft3)"/>
      <circle cx="90" cy="400" r="130" fill="#B197FF" opacity=".22" filter="url(#soft3)"/>
      {stars(4, 34)}
      {sparkle(80, 110, 20, "#fff", .95)}{sparkle(440, 76, 26, "#7FF7EE", 1)}{sparkle(456, 250, 14, "#FFE08A", .95)}{sparkle(58, 300, 12, "#fff", .85)}'''
    gh, headsvg = head("tan", uid="4", ears=False, rx=88, ry=102)
    hair_defs = grad("hr4", [(0, "#4A3030"), (1, "#190F0F")])
    puffs = ''.join(f'<circle cx="{256+math.cos(math.radians(a))*r:.0f}" cy="{214+math.sin(math.radians(a))*r*0.92:.0f}" r="{s}" fill="url(#hr4)"/>'
                    for a, r, s in [(180, 128, 46), (200, 130, 50), (222, 128, 52), (246, 126, 52), (270, 126, 54), (294, 126, 52), (318, 128, 52), (340, 130, 50), (360, 128, 46), (150, 118, 38), (30, 118, 38)])
    body = neck_torso("tan", "#3DF6E6", "#0E8FA3", collar="#D8FFFB", v=False, uid="4")
    top = '<path d="M196 380 q60 54 120 0" stroke="#fff" stroke-opacity=".5" stroke-width="6" fill="none" stroke-linecap="round"/><path d="M150 432 l40 -16 M360 432 l-40 -16" stroke="#fff" stroke-opacity=".35" stroke-width="5" stroke-linecap="round"/>'
    fringe = '<path d="M168 214 C176 150 336 150 344 214 C318 186 192 186 168 214Z" fill="url(#hr4)"/><path d="M200 168 C232 148 290 148 320 170" stroke="#fff" stroke-opacity=".22" stroke-width="7" fill="none" stroke-linecap="round"/>'
    star_clip = f'<g transform="translate(402 176) rotate(14)"><path d="M0 -26 L7 -8 L26 -8 L11 4 L17 24 L0 12 L-17 24 L-11 4 L-26 -8 L-7 -8Z" fill="#FFD66B"/></g>'
    inner = body + top + f'<defs>{hair_defs}{gh}</defs>' + puffs + headsvg + face("tan", "smile", brow="#190F0F", lash=True, cy=250, scale=.96) + fringe + star_clip + gloss()
    return wrap(inner, bg_defs, bg)

# ---------------------------------------------------------------- 5 سفیر (bearded mentor, gold)
def av5():
    bg_defs = grad("bg", [(0, "#FFF0B0"), (.5, "#F3B84A"), (1, "#B5701A")], radial=True, cx=.3, cy=.15, r=1.2) + VIG
    bg = f'''<rect width="512" height="512" fill="url(#bg)"/>
      <g opacity=".22" stroke="#fff" stroke-width="3" fill="none"><circle cx="256" cy="256" r="150"/><circle cx="256" cy="256" r="210"/><circle cx="256" cy="256" r="270"/></g>
      <path d="M256 20 L282 98 L360 74 L316 140 L396 160 L316 190 L350 262 L256 214 L162 262 L196 190 L116 160 L196 140 L152 74 L230 98Z" fill="#fff" opacity=".13"/>
      {sparkle(84, 118, 16, "#fff", .9)}{sparkle(438, 110, 20, "#fff", .95)}{sparkle(450, 330, 10, "#fff", .8)}'''
    gh, headsvg = head("warm", uid="5", rx=96, ry=110)
    body = neck_torso("warm", "#2FB488", "#0E6A52", collar="#F4FFF9", uid="5")
    tie = '<path d="M244 372 L268 372 L276 392 L256 470 L236 392Z" fill="#FFD66B"/><path d="M244 372 L268 372 L262 384 L250 384Z" fill="#E1A92A"/><path d="M252 400 L256 392 L260 400 L256 440Z" fill="#fff" opacity=".35"/>'
    hair = '''<defs>''' + grad("hr5", [(0, "#4B4B53"), (1, "#1F1F26")]) + '''</defs>
      <path d="M158 232 C146 98 366 96 354 232 C336 170 290 150 256 152 C222 154 176 172 158 232Z" fill="url(#hr5)"/>
      <path d="M176 164 C214 134 298 134 336 164 C296 148 216 148 176 164Z" fill="#fff" opacity=".18"/>
      <path d="M162 236 C160 332 202 388 256 388 C310 388 352 332 350 236 C332 276 298 292 256 292 C214 292 180 276 162 236Z" fill="url(#hr5)"/>
      <path d="M200 340 C226 372 286 372 312 340" stroke="#fff" stroke-opacity=".12" stroke-width="5" fill="none" stroke-linecap="round"/>
      <path d="M208 304 C230 284 282 284 304 304 C288 312 270 306 256 306 C242 306 224 312 208 304Z" fill="#17171c"/>'''
    mouth = '<path d="M236 322 q20 14 40 0" stroke="#E7A08A" stroke-width="6" stroke-linecap="round" fill="none"/>'
    inner = body + tie + f'<defs>{gh}</defs>' + headsvg + face("warm", "neutral", brow="#1F1F26", cy=246).replace('<path d="M-20 56 q20 14 40 0" stroke="#9B3B3B" stroke-width="5.5" fill="none" stroke-linecap="round"/>', '') + hair + mouth + gloss()
    return wrap(inner, bg_defs, bg)

AVATARS = [av0, av1, av2, av3, av4, av5]
