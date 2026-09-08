# -*- coding: utf-8 -*-
"""中式墓碑 64x64 精细纹理集（黑碑+金文+灰白底座+红褐香炉）"""
import os, random
from PIL import Image, ImageDraw

OUT = r"F:\AI项目\world_loves_you\src\main\resources\assets\minecraft_love_you\textures\block"
os.makedirs(OUT, exist_ok=True)

def noise(base, amp, size=64, seed=1):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        for x in range(size):
            n = rnd.randint(-amp, amp)
            px[x, y] = (max(0,min(255,base[0]+n)), max(0,min(255,base[1]+n)), max(0,min(255,base[2]+n)), 255)
    return img

# ===== 正面 =====
img = Image.new("RGBA", (64, 64), (0,0,0,0))
d = ImageDraw.Draw(img)
# 底座
for y in range(50, 64):
    for x in range(64):
        n = random.Random(x*7+y*13).randint(-8, 8)
        d.point((x,y), fill=(200+n,200+n,194+n,255))
d.line((0,50,64,50), fill=(160,160,156))
d.line((0,56,64,56), fill=(170,170,166))
# 主碑黑+圆弧顶
d.rectangle((14,12,49,49), fill=(28,28,30,255))
for x in range(14,50):
    t=(x-14)/35.0
    h=int(round(8*(1-(2*t-1)**2)**0.5))
    for y in range(12-h,12):
        d.point((x,y), fill=(30,30,32,255))
d.line((16,14,16,48), fill=(52,52,56))
d.rectangle((17,15,46,47), outline=(150,116,48))
# 金"奠"
GOLD=(212,172,74); cx=32
d.line((cx-4,20,cx+4,20), fill=GOLD)
d.point((cx,18), fill=GOLD)
d.line((cx-3,24,cx+3,24), fill=GOLD)
d.rectangle((cx-3,24,cx+3,32), outline=GOLD)
d.line((cx-2,27,cx+2,27), fill=GOLD)
d.line((cx-5,35,cx+5,35), fill=GOLD)
d.line((cx-4,36,cx-6,42), fill=GOLD)
d.line((cx+4,36,cx+6,42), fill=GOLD)
WHT=(216,216,210)
for y in range(20,44,4):
    d.point((21,y), fill=WHT); d.point((43,y), fill=WHT)
img.save(os.path.join(OUT,"grave_front.png"))

# ===== base =====
noise((206,206,200),8,64,5).save(os.path.join(OUT,"grave_base.png"))

# ===== 香炉 incense 32x32 =====
inc = Image.new("RGBA",(32,32),(0,0,0,0))
di = ImageDraw.Draw(inc)
di.ellipse((6,8,25,26), fill=(126,58,44), outline=(88,38,30), width=2)
di.ellipse((9,11,22,21), fill=(148,74,56))
di.ellipse((12,12,19,15), fill=(60,26,22))
di.rectangle((7,25,11,28), fill=(88,38,30))
di.rectangle((14,25,17,28), fill=(88,38,30))
di.rectangle((20,25,24,28), fill=(88,38,30))
di.arc((4,6,16,14), 200,340, fill=(88,38,30), width=2)
di.arc((16,6,28,14), 200,340, fill=(88,38,30), width=2)
inc.save(os.path.join(OUT,"incense.png"))

# ===== side =====
side = noise((188,188,182),9,64,11)
d3 = ImageDraw.Draw(side)
d3.rectangle((0,50,63,63), fill=(210,210,204))
d3.line((0,50,64,50), fill=(158,158,152))
d3.rectangle((0,12,10,50), fill=(34,34,36))
side.save(os.path.join(OUT,"grave_side.png"))

# ===== top =====
top = noise((206,206,200),7,64,21)
d4 = ImageDraw.Draw(top)
d4.ellipse((14,10,50,54), fill=(30,30,32), outline=(60,60,64), width=2)
top.save(os.path.join(OUT,"grave_top.png"))

print("saved:", sorted(os.listdir(OUT)))
