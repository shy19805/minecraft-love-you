# -*- coding: utf-8 -*-
"""世界爱你 功能自检：解析已注册模块（由 /wly list 生成）与计划表比对。
简化版：扫描 src/main/java/com/worldlovesyou/module/<id> 目录 + mixins.json，
输出计划 36 项的 已实现/未实现 状态表。
"""
import os
import json
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD_SRC = os.path.join(ROOT, "src", "main", "java", "com", "minecraftloveyou", "module")
MIXINS = os.path.join(ROOT, "src", "main", "resources", "minecraft_love_you.mixins.json")

PLAN = {
    # 序号: 模块id
    1: "ctm",
    2: "snow-cap",
    3: "anvil-master",
    4: "mend-hold",
    5: "elb",
    6: "enchant-reveal",
    7: "reroll",
    8: "(重复项-同6)",
    9: "enchant-tweak",
    10: "trade-display",
    11: "smart-tooltip",
    12: "bars",
    13: "eztweak",
    14: "maxhealth-fix",
    15: "ruler",
    16: "no-exp-warn",
    17: "harvest",
    18: "visible-traders",
    19: "eat-full",
    20: "big-stacks",
    21: "rendi",
    22: "unloaded-activity",
    23: "climbing",
    24: "open-water",
    25: "friendly-fire",
    26: "loot-beams",
    27: "splash-milk",
    28: "graves",
    29: "beacon-fill",
    30: "trajectory",
    31: "infarrows",
    32: "clickthrough",
    33: "smooth-bedrock",
    34: "spawner-plus",
    35: "portable-spawner",
    36: "powershot",
}

implemented = set()
for d in os.listdir(MOD_SRC):
    if os.path.isdir(os.path.join(MOD_SRC, d)):
        implemented.add(d)

# 计划表里的老名字 → 实际模块目录名（改过名/合并过的在这里登记，否则会误报「未实现」）
ALIAS = {
    "unloaded-activity": "offlinecatchup",  # 对应 unloadedactivity 模组，本项目实现为「离线追赶」
    "eztweak": "eztweak",                   # 已拆成 4 个子规则，目录仍在
    "bars": "bars",
    "loot-beams": "lootbeams",
    "portable-spawner": "portablespawner",
}

with open(MIXINS, encoding="utf-8") as f:
    mj = json.load(f)
mixin_count = len(mj.get("mixins", [])) + len(mj.get("client", []))

print(f"模块目录数: {len(implemented)} | mixin 数: {mixin_count}")
print("-" * 46)
done = 0
for idx in sorted(PLAN):
    mid = PLAN[idx]
    # 目录名把 '-' 转 '' 无连字符，做一次模糊匹配；ALIAS 优先
    folder_guess = ALIAS.get(mid, mid).replace("-", "")
    hit = any(d.replace("-", "") == folder_guess for d in implemented)
    status = "已实现" if hit or mid.startswith("(") else "未实现"
    if status == "已实现":
        done += 1
    print(f"{idx:>2}. {mid:<22} {status}")
print("-" * 46)
print(f"已完成: {done}/36")
