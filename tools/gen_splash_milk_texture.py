# -*- coding: utf-8 -*-
import os
from PIL import Image, ImageDraw
OUT = r"F:\AI项目\world_loves_you\src\mainesourcesssets\minecraft_love_you	extures\item"
os.makedirs(OUT, exist_ok=True)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
NIPPLE=(238,168,148,255); NIPPLE_DK=(210,130,112,255)
CAP=(246,220,130,255); CAP_DK=(216,186,92,255)
GLASS=(214,230,242,255); GLASS_DK=(158,182,204,255); GLASS_LT=(238,248,255,255)
MILK=(250,250,252,255); MILK_DK=(230,230,238,255); SCALE=(170,190,210,255)
d.ellipse((12,1,19,8), fill=NIPPLE, outline=NIPPLE_DK)
d.point((15,3), fill=GLASS_LT)
d.rectangle((10,8,21,13), fill=CAP, outline=CAP_DK)
d.line((10,10,21,10), fill=CAP_DK)
d.line((10,12,21,12), fill=CAP_DK)
d.polygon([(11,13),(20,13),(23,16),(8,16)], fill=GLASS)
d.rounded_rectangle((7,15,24,29), radius=4, fill=GLASS, outline=GLASS_DK)
d.rounded_rectangle((8,21,23,28), radius=3, fill=MILK)
d.line((9,21,22,21), fill=(255,255,255,255))
d.line((9,28,22,28), fill=MILK_DK)
for yy in (17,21,25):
    d.line((21,yy,23,yy), fill=SCALE)
d.line((9,17,9,26), fill=GLASS_LT)
d.line((8,29,23,29), fill=GLASS_DK)
img.save(os.path.join(OUT, "splash_milk.png"))
print("baby bottle saved")
