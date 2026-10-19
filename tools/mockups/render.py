#!/usr/bin/env python3
"""Renders the livelooper control mockups (landscape) with Pillow.
Usage: python3 tools/mockups/render.py  -> docs/mockups/*.png
Supersampled 4x, saved at 2x logical size."""
import math, os, random
from PIL import Image, ImageDraw, ImageFont, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "..", "docs", "mockups")
SS = 4          # supersample factor (logical px -> render px)
FINAL = 2       # saved scale

B0, B1, B2 = (255, 122, 24), (255, 45, 111), (177, 75, 255)
TXT, DIM = (230, 232, 238), (142, 147, 156)
RED, GREEN, ORANGE, YEL, BLUE, CYAN = (255, 59, 48), (48, 209, 88), (255, 159, 10), (255, 214, 10), (10, 132, 255), (100, 210, 255)
LCD = (6, 18, 26)
TRACK = [B0, B1, B2, CYAN, GREEN]
_fc = {}


def F(size, w=700):
    k = (size, w)
    if k not in _fc:
        _fc[k] = ImageFont.truetype(os.path.join(HERE, "fonts", f"Manrope-{w}.ttf"), int(size * SS))
    return _fc[k]


class C:
    def __init__(s, w, h):
        s.w, s.h = w, h
        s.im = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 255))
        s.bg()

    def p(s, *v):
        return [int(round(x * SS)) for x in v]

    def bg(s):
        w, h = s.im.size
        g = Image.new("RGBA", (64, 64))
        px = g.load()
        for y in range(64):
            for x in range(64):
                fx, fy = x / 63, y / 63
                r = 22 + 18 * (1 - fx) * (1 - fy) + 6 * fx
                gg = 18 + 6 * (1 - fy)
                b = 26 + 22 * fx * (1 - fy) + 8 * fy
                px[x, y] = (int(r), int(gg), int(b), 255)
        s.im = g.resize((w, h), Image.BICUBIC)

    def layer(s):
        return Image.new("RGBA", s.im.size, (0, 0, 0, 0))

    def comp(s, l):
        s.im = Image.alpha_composite(s.im, l)

    def rr(s, box, r, fill=None, outline=None, width=1):
        l = s.layer(); d = ImageDraw.Draw(l)
        d.rounded_rectangle(s.p(*box), int(r * SS), fill=fill, outline=outline, width=int(width * SS))
        s.comp(l)

    def shadow(s, box, r, a=110, blur=10, dy=4):
        l = s.layer(); d = ImageDraw.Draw(l)
        x0, y0, x1, y1 = box
        d.rounded_rectangle(s.p(x0, y0 + dy, x1, y1 + dy), int(r * SS), fill=(0, 0, 0, a))
        s.comp(l.filter(ImageFilter.GaussianBlur(blur * SS)))

    def glass(s, box, r=16, tint=None, a=16):
        s.shadow(box, r)
        x0, y0, x1, y1 = box
        s.rr(box, r, fill=(255, 255, 255, a))
        if tint:  # coloured wash fading downward
            m = Image.new("L", (1, 64))
            for i in range(64):
                m.putpixel((0, i), int(70 * (1 - i / 63) ** 1.6))
            W, H = s.p(x1 - x0, y1 - y0)
            col = Image.new("RGBA", (W, H), tint + (255,))
            mask = Image.new("L", (W, H), 0)
            ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], int(r * SS), fill=255)
            mm = m.resize((W, H))
            from PIL import ImageChops
            col.putalpha(ImageChops.multiply(mask, mm))
            l = s.layer(); l.paste(col, s.p(x0, y0)[:2]); s.comp(l)
        s.rr(box, r, outline=(255, 255, 255, 34), width=1)
        s.rr((x0 + 1, y0 + 1, x1 - 1, y0 + 2), 1, fill=(255, 255, 255, 18))

    def grad(s, box, r, cols=(B0, B1, B2), alpha=255, vertical=False):
        x0, y0, x1, y1 = box
        W, H = s.p(x1 - x0, y1 - y0)
        g = Image.new("RGBA", (256, 1))
        for i in range(256):
            t = i / 255 * (len(cols) - 1)
            k = min(int(t), len(cols) - 2); f = t - k
            c = [int(cols[k][j] + (cols[k + 1][j] - cols[k][j]) * f) for j in range(3)]
            g.putpixel((i, 0), tuple(c) + (alpha,))
        g = g.resize((W, H)) if not vertical else g.rotate(-90, expand=True).resize((W, H))
        mask = Image.new("L", (W, H), 0)
        ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], int(r * SS), fill=alpha)
        g.putalpha(mask)
        l = s.layer(); l.paste(g, s.p(x0, y0)[:2]); s.comp(l)

    def t(s, x, y, txt, size=12, w=700, col=TXT, anchor="la"):
        d = ImageDraw.Draw(s.im)
        d.text(s.p(x, y), txt, font=F(size, w), fill=col, anchor=anchor)

    def tw(s, txt, size, w=700):
        return F(size, w).getlength(txt) / SS

    def gtext(s, x, y, txt, size, w=800, anchor="la"):
        m = Image.new("L", s.im.size, 0)
        ImageDraw.Draw(m).text(s.p(x, y), txt, font=F(size, w), fill=255, anchor=anchor)
        bb = m.getbbox()
        if not bb: return
        W, H = bb[2] - bb[0], bb[3] - bb[1]
        g = Image.new("RGBA", (256, 1))
        for i in range(256):
            t = i / 255 * 2; k = min(int(t), 1); f = t - k
            cs = (B0, B1, B2)
            g.putpixel((i, 0), tuple(int(cs[k][j] + (cs[k + 1][j] - cs[k][j]) * f) for j in range(3)) + (255,))
        g = g.resize((W, H)); l = s.layer(); l.paste(g, bb[:2]); l.putalpha(Image.composite(l.getchannel("A"), Image.new("L", s.im.size, 0), m))
        s.comp(l)

    def circle(s, cx, cy, r, fill=None, outline=None, width=1):
        l = s.layer(); d = ImageDraw.Draw(l)
        d.ellipse(s.p(cx - r, cy - r, cx + r, cy + r), fill=fill, outline=outline, width=int(width * SS))
        s.comp(l)

    def glow(s, cx, cy, r, col, a=120):
        l = s.layer(); d = ImageDraw.Draw(l)
        d.ellipse(s.p(cx - r, cy - r, cx + r, cy + r), fill=col + (a,))
        s.comp(l.filter(ImageFilter.GaussianBlur(r * SS * 0.45)))

    def arc(s, cx, cy, r, a0, a1, col, width):
        l = s.layer(); d = ImageDraw.Draw(l)
        d.arc(s.p(cx - r, cy - r, cx + r, cy + r), a0, a1, fill=col, width=int(width * SS))
        s.comp(l)

    def save(s, name):
        out = s.im.resize((s.w * FINAL, s.h * FINAL), Image.LANCZOS).convert("RGB")
        os.makedirs(OUT, exist_ok=True)
        out.save(os.path.join(OUT, name), optimize=True)


# ---------- widgets ----------
STATE = {"PLAY": GREEN, "DUB": ORANGE, "STOP": CYAN, "REC": RED, "EMPTY": (120, 125, 132)}


def wave(c, box, col, prog, seed, quiet=False):
    x0, y0, x1, y1 = box
    rnd = random.Random(seed)
    mid = (y0 + y1) / 2; hh = (y1 - y0) / 2 - 2
    n = int((x1 - x0) / 1.6)
    l = c.layer(); d = ImageDraw.Draw(l)
    env = 0
    for i in range(n):
        if i % max(6, n // 6) == 0: env = 0.6 + 0.4 * rnd.random()
        env *= 0.93
        a = hh * max(0.08, env) * (0.5 + 0.5 * rnd.random()) * (0.4 if quiet else 1)
        x = x0 + i * 1.6
        played = (i / n) < prog
        cc = col + ((235 if played else 120),)
        d.line(c.p(x, mid - a, x, mid + a), fill=cc, width=max(1, int(0.9 * SS)))
    c.comp(l)
    px = x0 + (x1 - x0) * prog
    c.rr((px - 0.8, y0, px + 0.8, y1), 0.8, fill=(255, 255, 255, 230))


def big_button(c, cx, cy, r, col, state, label="REC", sub="PLAY / DUB"):
    lit = state in ("PLAY", "DUB", "REC")
    if lit: c.glow(cx, cy, r * 1.25, col, 90)
    c.circle(cx, cy, r + r * 0.16, fill=(12, 13, 16, 255))
    seg = 24
    for i in range(seg):
        a0 = i * 360 / seg - 90 + 2; a1 = a0 + 360 / seg - 5
        on = lit and (state != "PLAY" or i < seg * 0.3)
        c.arc(cx, cy, r + r * 0.09, a0, a1, (col if on else (40, 60, 48) if state in ("STOP", "EMPTY") else tuple(v // 4 for v in col)) + (255,), r * 0.07)
    base = col if lit else tuple(int(v * 0.45 + 60 * 0.55) for v in col)
    for k in range(10, 0, -1):
        f = k / 10
        cc = tuple(int(v * (0.55 + 0.45 * (1 - f))) for v in base)
        c.circle(cx, cy - r * 0.05 * (1 - f), r * f, fill=cc + (255,))
    c.circle(cx, cy - r * 0.48, r * 0.55, fill=(255, 255, 255, 34))
    c.t(cx, cy - r * 0.28, label, max(8, r * 0.2), 800, (255, 255, 255, 235), "mm")
    d = r * 0.11
    c.circle(cx - r * 0.17, cy + r * 0.06, d, fill=(255, 255, 255, 240) if lit else RED + (255,))
    l = c.layer(); ImageDraw.Draw(l).polygon(c.p(cx + r * 0.08, cy + r * 0.06 - d, cx + r * 0.08, cy + r * 0.06 + d, cx + r * 0.29, cy + r * 0.06), fill=(255, 255, 255, 240) if lit else GREEN + (255,))
    c.comp(l)
    c.t(cx, cy + r * 0.38, sub, max(6, r * 0.12), 700, (255, 255, 255, 190), "mm")


def hbar(c, box, val, col, label=None, unity=0.5):
    x0, y0, x1, y1 = box
    if label:
        c.t(x0, y0 - 11, label, 7.5, 700, DIM)
        c.t(x1, y0 - 11, f"{int(val * 200 if unity == .5 else val * 100)} %", 7.5, 700, TXT, "ra")
    c.rr(box, (y1 - y0) / 2, fill=(0, 0, 0, 120))
    if val > 0:
        c.grad((x0, y0, x0 + (x1 - x0) * val, y1), (y1 - y0) / 2, (GREEN, (150, 220, 60)))
    ux = x0 + (x1 - x0) * unity
    c.rr((ux - 0.8, y0 - 3, ux + 0.8, y1 + 3), 0.8, fill=(230, 232, 238, 220))


def btn(c, box, label, size=9, active=None, grad=False, r=8):
    if grad:
        c.shadow(box, r, 90, 6, 2); c.grad(box, r)
    elif active:
        c.shadow(box, r, 90, 6, 2)
        c.rr(box, r, fill=tuple(v // 3 for v in active) + (255,), outline=active + (255,), width=1.2)
    else:
        c.shadow(box, r, 90, 6, 2)
        c.rr(box, r, fill=(44, 46, 52, 255), outline=(255, 255, 255, 26))
        c.rr((box[0] + 1, box[1] + 1, box[2] - 1, box[1] + (box[3] - box[1]) * .5), r, fill=(255, 255, 255, 10))
    x0, y0, x1, y1 = box
    c.t((x0 + x1) / 2, (y0 + y1) / 2, label, size, 800, (255, 230, 200) if active else TXT, "mm")


def knob(c, cx, cy, r, val, col, label):
    c.arc(cx, cy, r + 3, 135, 405, (40, 42, 48, 255), 3)
    c.arc(cx, cy, r + 3, 135, 135 + 270 * val, col + (255,), 3)
    c.shadow((cx - r, cy - r, cx + r, cy + r), r, 120, 4, 2)
    for k in range(6, 0, -1):
        f = k / 6; v = int(30 + 40 * (1 - f))
        c.circle(cx, cy - r * .08 * (1 - f), r * f, fill=(v, v + 2, v + 6, 255))
    a = math.radians(135 + 270 * val)
    l = c.layer(); ImageDraw.Draw(l).line(c.p(cx + math.cos(a) * r * .3, cy + math.sin(a) * r * .3, cx + math.cos(a) * r * .85, cy + math.sin(a) * r * .85), fill=(240, 240, 245, 255), width=int(2 * SS))
    c.comp(l)
    c.t(cx, cy + r + 9, label, 7, 700, DIM, "mm")


def lcd(c, box, r=10):
    c.rr(box, r, fill=(10, 11, 14, 255))
    x0, y0, x1, y1 = box
    c.rr((x0 + 3, y0 + 3, x1 - 3, y1 - 3), r - 2, fill=LCD + (255,))


def header(c, tabs_active="LIVE", conn=("USB", "2.8 ms", GREEN), bpm="120.0", bar="5.4", tabs=("LIVE", "MIX", "FX", "SETUP")):
    W = c.w
    c.t(18, 12, "livelooper", 17, 800, TXT)
    c.gtext(18 + c.tw("livelooper", 17, 800) + 6, 15.5, "control", 12, 800)
    c.t(18, 35, "designed by groovex", 6.5, 600, DIM)
    # tabs
    tx = 180
    for tname in tabs:
        wdt = c.tw(tname, 8.5, 800) + 22
        if tname == tabs_active:
            c.grad((tx, 14, tx + wdt, 36), 11)
        else:
            c.rr((tx, 14, tx + wdt, 36), 11, fill=(255, 255, 255, 14), outline=(255, 255, 255, 30))
        c.t(tx + wdt / 2, 25, tname, 8.5, 800, TXT, "mm")
        tx += wdt + 6
    # transport lcd
    lx = W / 2 + 20
    lcd(c, (lx, 8, lx + 210, 44), 9)
    c.t(lx + 14, 26, bpm, 18, 800, (127, 227, 255), "lm")
    c.t(lx + 76, 30, "BPM", 6, 700, (60, 150, 190), "lm")
    c.t(lx + 104, 26, bar, 18, 800, (127, 227, 255), "lm")
    for i in range(4):
        c.circle(lx + 156 + i * 11, 30, 3.6, fill=(ORANGE if i == 0 else CYAN) + (255,))
    c.t(lx + 151, 17, "BAR . BEAT", 5.5, 700, (60, 150, 190))
    # connection pill
    name, lat, col = conn
    pw = 104
    px0 = W - 18 - pw - 34
    c.rr((px0, 14, px0 + pw, 36), 11, fill=(0, 0, 0, 90), outline=col + (150,))
    c.glow(px0 + 13, 25, 5, col, 200); c.circle(px0 + 13, 25, 3.5, fill=col + (255,))
    c.t(px0 + 23, 25, f"{name} · {lat}", 8.5, 800, TXT, "lm")
    # menu
    mx = W - 18 - 26
    c.rr((mx, 12, mx + 26, 38), 9, fill=(255, 255, 255, 14), outline=(255, 255, 255, 30))
    for i in range(3):
        c.rr((mx + 8, 19 + i * 5.5, mx + 18, 20.5 + i * 5.5), 1, fill=(230, 232, 238, 230))


TRACKS = [("PLAY", .62, "4 BARS", .5), ("DUB", .83, "2 BARS", .5), ("STOP", 0, "1 BAR", .4), ("REC", .74, "AUTO", .5), ("EMPTY", 0, "8 BARS", .5)]


def track_card(c, i, box, state, prog, length, level, compact=True, seg=False):
    x0, y0, x1, y1 = box
    col = TRACK[i]; sc = STATE[state]
    c.glass(box, 14, tint=col, a=14)
    c.rr((x0 + 10, y0 + 10, x0 + 13, y0 + 22), 1.5, fill=col + (255,))
    c.t(x0 + 19, y0 + 16, f"TRACK {i + 1}", 9, 800, TXT, "lm")
    pill = length
    pw = c.tw(pill, 6.5, 800) + 12
    dimmed = state not in ("EMPTY", "REC")
    c.rr((x1 - 10 - pw, y0 + 9, x1 - 10, y0 + 23), 7, fill=(0, 0, 0, 110), outline=(col if not dimmed else (110, 110, 118)) + (200,))
    c.t(x1 - 10 - pw / 2, y0 + 16, pill, 6.5, 800, TXT if not dimmed else DIM, "mm")
    w = x1 - x0
    lh = 92 if compact else 96
    lb = (x0 + 8, y0 + 30, x1 - 8, y0 + 30 + lh)
    lcd(c, lb, 9)
    c.t(lb[0] + 10, lb[1] + 15, state, 15 if compact else 20, 800, sc + (255,), "lm")
    if state != "EMPTY":
        c.t(lb[0] + 10, lb[1] + (28 if compact else 34), f"{['1.6', '1.1', '0.8', '3.2', ''][i]} BARS", 5.8, 700, (90, 170, 200))
        c.t(lb[2] - 10, lb[1] + (28 if compact else 34), f"{int(prog * 100)} %", 5.8, 700, (90, 170, 200), "ra")
        wave(c, (lb[0] + 8, lb[1] + (34 if compact else 42), lb[2] - 8, lb[3] - 6), col, prog if prog else .02, i + 3, quiet=state == "STOP")
    else:
        c.rr((lb[0] + 12, (lb[1] + lb[3]) / 2 + 6, lb[2] - 12, (lb[1] + lb[3]) / 2 + 8), 1, fill=(40, 60, 70, 255))
    return lb[3]


# ---------- mockup 1: phone live ----------
def m1():
    c = C(1200, 540)
    header(c)
    n = 5; gx = 18; gap = 10; top = 54; bot = 470
    cw = (c.w - 2 * gx - gap * (n - 1)) / n
    for i, (st, pr, ln, lv) in enumerate(TRACKS):
        x0 = gx + i * (cw + gap)
        yb = track_card(c, i, (x0, top, x0 + cw, bot), st, pr, ln, lv)
        r = min(cw / 2 - 26, (bot - 62 - yb) / 2 - 14)
        big_button(c, x0 + cw / 2, (yb + bot - 62) / 2, r, STATE[st] if st != "EMPTY" else (70, 130, 90), st)
        hbar(c, (x0 + 12, bot - 50, x0 + cw - 12, bot - 44), lv * (1.0 if i != 2 else .8), col=None, label="LEVEL")
        btn(c, (x0 + 10, bot - 36, x0 + cw / 2 - 3, bot - 10), "■ STOP", 8)
        btn(c, (x0 + cw / 2 + 3, bot - 36, x0 + cw - 10, bot - 10), "MUTE", 8, active=(ORANGE if i == 2 else None))
    # bottom bar
    y = 482
    btn(c, (18, y, 200, y + 44), "ALL START / STOP", 9.5)
    btn(c, (210, y, 320, y + 44), "UNDO", 9.5)
    btn(c, (330, y, 440, y + 44), "TAP", 9.5)
    btn(c, (450, y, 580, y + 44), "CAPTURE", 9.5)
    c.glass((592, y, 1182, y + 44), 10, a=10)
    c.t(606, y + 5, "PERFORMANCE FX", 6.5, 800, DIM)
    for k, (nm, a) in enumerate([("ECHO 1/8", ORANGE), ("REPEAT", None), ("STUTTER", None), ("FILL", None)]):
        btn(c, (606 + k * 92, y + 20, 690 + k * 92, y + 38), nm, 7, active=a, r=6)
    c.t(980, y + 5, "MASTER", 6.5, 800, DIM)
    hbar(c, (980, y + 27, 1168, y + 32), .5, None)
    c.save("01-phone-live.png")


# ---------- mockup 2: tablet full surface ----------
def m2():
    c = C(1280, 800)
    header(c, "MIX", conn=("USB", "2.6 ms", GREEN))
    n = 5; gx = 18; gap = 10; top = 56; bot = 622
    right = 870
    cw = (right - gx - gap * (n - 1)) / n
    for i, (st, pr, ln, lv) in enumerate(TRACKS):
        x0 = gx + i * (cw + gap); x1 = x0 + cw
        yb = track_card(c, i, (x0, top, x1, bot), st, pr, ln, lv, compact=False)
        r = 48
        big_button(c, x0 + cw / 2, yb + 14 + r + 6, r, STATE[st] if st != "EMPTY" else (70, 130, 90), st)
        # loop length segmented
        sy = yb + 14 + 2 * r + 34
        c.t(x0 + 10, sy - 10, "LOOP LENGTH", 6, 800, DIM)
        opts = ["A", "1", "2", "4", "8", "16"]
        sel = {"AUTO": 0, "1 BAR": 1, "2 BARS": 2, "4 BARS": 3, "8 BARS": 4}[ln]
        sw = (cw - 20) / 6
        c.rr((x0 + 10, sy, x1 - 10, sy + 18), 6, fill=(0, 0, 0, 110))
        for k, o in enumerate(opts):
            bx = x0 + 10 + k * sw
            if k == sel: c.grad((bx + 1, sy + 1, bx + sw - 1, sy + 17), 5)
            c.t(bx + sw / 2, sy + 9, o, 7, 800, TXT if k == sel else DIM, "mm")
        # fader + meter
        fy0, fy1 = sy + 34, bot - 52
        fx = x0 + cw / 2 - 8
        c.rr((fx - 3, fy0, fx + 3, fy1), 3, fill=(0, 0, 0, 140))
        mv = [.7, .85, .1, .6, 0][i]
        for side in (fx + 14, fx + 20):
            c.rr((side, fy0, side + 4, fy1), 2, fill=(0, 0, 0, 140))
            if mv: c.grad((side, fy1 - (fy1 - fy0) * mv, side + 4, fy1), 2, (RED, YEL, GREEN), vertical=True)
        ky = fy1 - (fy1 - fy0) * (lv * 1.5 if i != 2 else .6)
        c.shadow((fx - 12, ky - 7, fx + 12, ky + 7), 4, 140, 3, 2)
        c.rr((fx - 12, ky - 7, fx + 12, ky + 7), 4, fill=(70, 73, 80, 255), outline=(255, 255, 255, 50))
        c.rr((fx - 9, ky - 0.6, fx + 9, ky + 0.6), 0.5, fill=col_or(i))
        knob(c, x0 + 24, fy0 + 14, 10, .5 + (i - 2) * .12, TRACK[i], "PAN")
        knob(c, x0 + 24, fy0 + 62, 10, [.3, .6, .2, .0, .0][i], TRACK[i], "SEND")
        c.t(x1 - 10, fy0 + 4, ["0.0", "-1.2", "-6.0", "0.0", "0.0"][i] + " dB", 6.5, 700, DIM, "ra")
        btn(c, (x0 + 10, bot - 42, x0 + cw / 3 - 1, bot - 14), "M", 9, active=(ORANGE if i == 2 else None), r=7)
        btn(c, (x0 + cw / 3 + 2, bot - 42, x0 + 2 * cw / 3 - 2, bot - 14), "S", 9, r=7)
        btn(c, (x0 + 2 * cw / 3 + 1, bot - 42, x1 - 10, bot - 14), "REV", 7.5, r=7)
    # right: XY + FX
    rx0 = right + 12; rx1 = c.w - 18
    c.glass((rx0, top, rx1, bot), 14, tint=B2, a=12)
    c.t(rx0 + 14, top + 18, "XY PAD", 8, 800, TXT, "lm")
    c.t(rx1 - 14, top + 18, "FILTER × RESONANCE", 6.5, 700, DIM, "rm")
    xy(c, (rx0 + 12, top + 32, rx1 - 12, top + 380))
    c.t(rx0 + 14, top + 400, "PERFORMANCE FX", 6.5, 800, DIM)
    names = [("ECHO 1/8", ORANGE), ("REPEAT", None), ("STUTTER", None), ("FILL", None), ("TAPE STOP", B2), ("REVERSE", None)]
    bw = (rx1 - rx0 - 24 - 12) / 3
    for k, (nm, a) in enumerate(names):
        bx = rx0 + 12 + (k % 3) * (bw + 6); by = top + 410 + (k // 3) * 36
        btn(c, (bx, by, bx + bw, by + 30), nm, 7.5, active=a)
    kx = rx0 + 40
    for k, (nm, v, cl) in enumerate([("FILTER", .45, CYAN), ("BEAT FX", .7, ORANGE), ("PUNCH", .55, B2), ("MASTER", .75, GREEN)]):
        knob(c, kx + k * ((rx1 - rx0 - 80) / 3), bot - 46, 14, v, cl, nm)
    # bottom transport
    y = 638
    c.glass((18, y, c.w - 18, 782), 14, a=10)
    c.t(32, y + 18, "SCENES", 7, 800, DIM, "lm")
    for k in range(8):
        bx = 32 + k * 112
        act = k == 2
        c.rr((bx, y + 28, bx + 104, y + 96), 10, fill=(255, 255, 255, 12), outline=((B1 + (255,)) if act else (255, 255, 255, 30)), width=1.4 if act else 1)
        if act: c.grad((bx + 1, y + 29, bx + 103, y + 34), 2)
        c.t(bx + 10, y + 46, f"SCENE {k + 1}", 8, 800, TXT if k < 5 else DIM)
        for j in range(5):
            on = k < 5 and (j + k) % 3 != 0
            c.rr((bx + 10 + j * 18, y + 70, bx + 24 + j * 18, y + 86), 3, fill=(TRACK[j] + (220,)) if on else (255, 255, 255, 18))
    btn(c, (940, y + 28, 1100, y + 60), "ALL START / STOP", 9.5, grad=True)
    btn(c, (1110, y + 28, 1248, y + 60), "CAPTURE", 9.5)
    btn(c, (940, y + 66, 1018, y + 96), "UNDO", 8.5)
    btn(c, (1026, y + 66, 1100, y + 96), "REDO", 8.5)
    btn(c, (1110, y + 66, 1248, y + 96), "TAP 120.0", 8.5)
    c.t(32, y + 116, "MIDI LEARN: long-press any control  ·  every touch control maps to a livelooper parameter", 7, 600, DIM)
    c.t(1248, y + 116, "Tablet · 2560 × 1600 · landscape", 7, 600, (100, 104, 112), "ra")
    c.save("02-tablet-mix.png")


def col_or(i):
    return TRACK[i] + (255,)


def xy(c, box, labels=True):
    x0, y0, x1, y1 = box
    lcd(c, box, 12)
    for k in range(1, 8):
        gx = x0 + (x1 - x0) * k / 8; gy = y0 + (y1 - y0) * k / 8
        c.rr((gx - .3, y0 + 6, gx + .3, y1 - 6), 0, fill=(127, 227, 255, 22))
        c.rr((x0 + 6, gy - .3, x1 - 6, gy + .3), 0, fill=(127, 227, 255, 22))
    px, py = x0 + (x1 - x0) * .64, y0 + (y1 - y0) * .36
    # trail
    pts = [(x0 + (x1 - x0) * (.22 + .42 * t), y0 + (y1 - y0) * (.75 - .39 * t + .08 * math.sin(t * 6))) for t in [i / 40 for i in range(41)]]
    l = c.layer(); d = ImageDraw.Draw(l)
    for k in range(len(pts) - 1):
        a = int(40 + 200 * k / 40)
        t = k / 40
        cc = tuple(int(B0[j] + (B2[j] - B0[j]) * t) for j in range(3))
        d.line(c.p(*pts[k], *pts[k + 1]), fill=cc + (a,), width=int(3 * SS))
    c.comp(l)
    c.rr((px - .5, y0 + 6, px + .5, y1 - 6), 0, fill=(255, 255, 255, 60))
    c.rr((x0 + 6, py - .5, x1 - 6, py + .5), 0, fill=(255, 255, 255, 60))
    c.glow(px, py, 26, B1, 170)
    c.circle(px, py, 13, fill=(255, 255, 255, 40), outline=(255, 255, 255, 220), width=2)
    c.circle(px, py, 4, fill=(255, 255, 255, 255))
    if labels:
        c.t(px + 18, py - 16, "CUTOFF 2.4 kHz  ·  RES 64 %", 7, 800, (127, 227, 255))


# ---------- mockup 3: phone FX / XY ----------
def m3():
    c = C(1200, 540)
    header(c, "FX", conn=("WLAN", "7.9 ms", YEL))
    top = 54
    c.glass((18, top, 700, 522), 14, tint=B2, a=12)
    c.t(32, top + 18, "XY PAD", 9, 800, TXT, "lm")
    for k, (nm, act) in enumerate([("FILTER", True), ("ECHO", False), ("REPEAT", False), ("PUNCH", False)]):
        bx = 120 + k * 78
        if act: c.grad((bx, top + 8, bx + 70, top + 28), 10)
        else: c.rr((bx, top + 8, bx + 70, top + 28), 10, fill=(255, 255, 255, 14), outline=(255, 255, 255, 30))
        c.t(bx + 35, top + 18, nm, 7.5, 800, TXT, "mm")
    c.t(686, top + 18, "X: CUTOFF   Y: RESONANCE", 6.5, 700, DIM, "rm")
    xy(c, (30, top + 38, 688, 470))
    btn(c, (30, 480, 150, 512), "HOLD", 8.5, active=B1)
    btn(c, (158, 480, 278, 512), "SNAP BACK", 8.5)
    btn(c, (286, 480, 406, 512), "QUANTISE 1/16", 8)
    c.t(688, 496, "multi-touch · 2 fingers = 2 effects", 7, 600, DIM, "rm")
    # right column
    rx = 712; rx1 = 1182
    c.glass((rx, top, rx1, 300), 14, a=12)
    c.t(rx + 14, top + 18, "TARGET", 7, 800, DIM, "lm")
    for k in range(6):
        nm = f"T{k + 1}" if k < 5 else "MASTER"
        bx = rx + 14 + k * 58 if k < 5 else rx + 14 + 5 * 58
        bw = 52 if k < 5 else rx1 - 14 - bx
        on = k in (0, 1)
        if on:
            c.rr((bx, top + 32, bx + bw, top + 58), 8, fill=tuple(v // 3 for v in TRACK[k]) + (255,), outline=TRACK[k] + (255,), width=1.3)
        else:
            c.rr((bx, top + 32, bx + bw, top + 58), 8, fill=(44, 46, 52, 255), outline=(255, 255, 255, 26))
        c.t(bx + bw / 2, top + 45, nm, 8, 800, TXT if on or k == 5 else DIM, "mm")
    c.t(rx + 14, top + 80, "PERFORMANCE FX", 7, 800, DIM, "lm")
    names = [("ECHO 1/8", ORANGE), ("REPEAT", None), ("STUTTER", None), ("FILL", None), ("TAPE STOP", B2), ("REVERSE", None), ("GATE 1/16", None), ("WASH", None)]
    bw = (rx1 - rx - 28 - 18) / 4
    for k, (nm, a) in enumerate(names):
        bx = rx + 14 + (k % 4) * (bw + 6); by = top + 94 + (k // 4) * 40
        btn(c, (bx, by, bx + bw, by + 34), nm, 7.5, active=a)
    c.t(rx + 14, top + 192, "PUNCH FX · TAPE STOP", 6.5, 800, DIM)
    c.rr((rx + 14, top + 206, rx1 - 14, top + 214), 4, fill=(0, 0, 0, 130))
    c.grad((rx + 14, top + 206, rx + 14 + (rx1 - rx - 28) * .62, top + 214), 4, (B1, B2))
    c.t(rx1 - 14, top + 228, "62 %  ·  hold to intensify", 6.5, 700, DIM, "ra")
    c.glass((rx, 312, rx1, 522), 14, a=12)
    for k, (nm, v, cl) in enumerate([("FILTER", .45, CYAN), ("BEAT FX", .7, ORANGE), ("PUNCH", .55, B2), ("MASTER", .75, GREEN)]):
        knob(c, rx + 62 + k * 110, 370, 22, v, cl, nm)
    c.t(rx + 14, 430, "MINI LOOPS", 7, 800, DIM)
    for i, (st, pr, ln, lv) in enumerate(TRACKS):
        bx = rx + 14 + i * 90
        c.rr((bx, 442, bx + 82, 510), 10, fill=(10, 11, 14, 255))
        c.circle(bx + 14, 456, 4, fill=STATE[st] + (255,))
        c.t(bx + 22, 456, st, 7, 800, STATE[st] + (255,), "lm")
        if st != "EMPTY": wave(c, (bx + 6, 468, bx + 76, 504), TRACK[i], pr if pr else .02, i + 3, quiet=st == "STOP")
    c.save("03-phone-fx-xy.png")


# ---------- mockup 4: phone connect + update (DE) ----------
def m4():
    c = C(1200, 540)
    header(c, "SETUP", conn=("USB", "2.8 ms", GREEN), tabs=("LIVE", "MIX", "FX", "SETUP"))
    top = 54
    c.glass((18, top, 640, 522), 14, tint=B0, a=12)
    c.t(34, top + 22, "Verbindung", 14, 800, TXT, "lm")
    c.t(34, top + 42, "Gefundene livelooper-Instanzen (USB zuerst, dann WLAN per Auto-Discovery)", 7.5, 600, DIM, "lm")
    rows = [("USB", "Studio-PC", "livelooper 1.7.0 · Protokoll 1", "2.8 ms", GREEN, True),
            ("WLAN", "MacBook-Buehne", "livelooper 1.7.0 · Protokoll 1", "7.9 ms", YEL, False),
            ("WLAN", "Proberaum-PC", "livelooper 1.6.2 · Protokoll 1", "11.4 ms", ORANGE, False)]
    for k, (tp, nm, ver, lat, cl, con) in enumerate(rows):
        y = top + 60 + k * 62
        c.rr((32, y, 626, y + 54), 12, fill=(255, 255, 255, 18 if con else 10), outline=((B1 + (200,)) if con else (255, 255, 255, 28)), width=1.3 if con else 1)
        c.rr((44, y + 15, 92, y + 39), 8, fill=(0, 0, 0, 120), outline=cl + (180,))
        c.t(68, y + 27, tp, 8, 800, TXT, "mm")
        c.t(106, y + 20, nm, 11, 800, TXT, "lm")
        c.t(106, y + 37, ver, 7.5, 600, DIM, "lm")
        c.circle(436, y + 27, 3.5, fill=cl + (255,))
        c.t(446, y + 27, lat, 9, 800, TXT, "lm")
        if con: btn(c, (510, y + 12, 614, y + 42), "VERBUNDEN", 8, grad=True)
        else: btn(c, (510, y + 12, 614, y + 42), "VERBINDEN", 8)
    y = top + 258
    c.rr((32, y, 626, y + 150), 12, fill=(255, 255, 255, 10), outline=(255, 255, 255, 28))
    c.t(46, y + 20, "Kopplung", 11, 800, TXT, "lm")
    c.t(46, y + 38, "Code eingeben, den livelooper unter Einstellungen → Remote anzeigt:", 7.5, 600, DIM, "lm")
    for k, ch in enumerate("482913"):
        bx = 46 + k * 50 + (14 if k > 2 else 0)
        lcd(c, (bx, y + 54, bx + 42, y + 104), 9)
        c.t(bx + 21, y + 79, ch, 22, 800, (127, 227, 255), "mm")
    c.t(380, y + 66, "Gerät bleibt gekoppelt", 8, 700, TXT, "lm")
    c.t(380, y + 82, "(Schlüssel pro Host, jederzeit widerrufbar)", 7, 600, DIM, "lm")
    c.rr((380, y + 96, 412, y + 112), 8, fill=GREEN + (255,)); c.circle(404, y + 104, 6, fill=(255, 255, 255, 255))
    c.t(46, y + 130, "Latenz-Ziel: USB < 5 ms, WLAN < 15 ms Touch → Audio-Thread", 7, 600, (100, 104, 112), "lm")
    c.t(34, 506, "Sprache: Deutsch / English  ·  folgt der Systemsprache", 7, 600, DIM, "lm")
    # update card
    ux = 652
    c.glass((ux, top, 1182, 522), 14, tint=B2, a=12)
    c.t(ux + 16, top + 22, "Update", 14, 800, TXT, "lm")
    c.rr((ux + 16, top + 42, 1166, top + 128), 12, fill=(255, 255, 255, 14), outline=(B1 + (160,)), width=1.2)
    c.gtext(ux + 30, top + 62, "Neue Version verfügbar", 12, 800, "lm")
    c.t(ux + 30, top + 82, "livelooper control 0.2.0  ·  installiert: 0.1.3", 8.5, 700, TXT, "lm")
    c.t(ux + 30, top + 100, "GitHub Releases · signierte APK · 14.2 MB", 7.5, 600, DIM, "lm")
    btn(c, (ux + 30, top + 140, ux + 270, top + 176), "HERUNTERLADEN & INSTALLIEREN", 8.5, grad=True)
    btn(c, (ux + 280, top + 140, 1166, top + 176), "SPÄTER", 8.5)
    c.rr((ux + 30, top + 188, 1166, top + 194), 3, fill=(0, 0, 0, 130))
    c.grad((ux + 30, top + 188, ux + 30 + (1166 - ux - 30) * .38, top + 194), 3)
    c.t(1166, top + 206, "38 % · Signatur wird nach dem Download geprüft", 6.5, 700, DIM, "ra")
    c.t(ux + 30, top + 232, "Neu in 0.2.0", 9, 800, TXT, "lm")
    for k, li in enumerate(["XY-Pad mit zwei Fingern (zwei Effekte gleichzeitig)", "Loop-Länge direkt auf den Track-Karten", "WLAN-Discovery schneller, Latenzanzeige pro Host", "Fehlerbehebungen bei der Kopplung"]):
        c.circle(ux + 34, top + 252 + k * 18, 2, fill=B1 + (255,))
        c.t(ux + 44, top + 252 + k * 18, li, 7.5, 600, DIM, "lm")
    c.t(ux + 30, top + 342, "Automatisch nach Updates suchen", 8, 700, TXT, "lm")
    c.rr((1134, top + 334, 1166, top + 350), 8, fill=B1 + (255,)); c.circle(1158, top + 342, 6, fill=(255, 255, 255, 255))
    c.t(ux + 30, top + 364, "Kanal", 8, 700, TXT, "lm")
    for k, nm in enumerate(["Stabil", "Beta"]):
        bx = 1040 + k * 64
        if k == 0: c.grad((bx, top + 355, bx + 58, top + 373), 9)
        else: c.rr((bx, top + 355, bx + 58, top + 373), 9, fill=(255, 255, 255, 14), outline=(255, 255, 255, 30))
        c.t(bx + 29, top + 364, nm, 7.5, 800, TXT, "mm")
    logo = Image.open(os.path.join(HERE, "beattactics-icon.png")).convert("RGBA")
    sz = int(44 * SS); logo = logo.resize((sz, sz), Image.LANCZOS)
    l = c.layer(); l.paste(logo, c.p(ux + 30, 448)[:2], logo); c.comp(l)
    c.t(ux + 84, 462, "designed by groovex", 10, 800, TXT, "lm")
    c.t(ux + 84, 478, "livelooper control · Android · AGPLv3", 7, 600, DIM, "lm")
    c.save("04-phone-connect-update-de.png")


if __name__ == "__main__":
    import sys
    for f in (sys.argv[1:] or ['m1','m2','m3','m4']): globals()[f]()
    print("ok")
