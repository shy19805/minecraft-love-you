# 我的世界爱你 · World Loves You

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2-00aa00)](https://www.minecraft.net/)
[![Modloader](https://img.shields.io/badge/Modloader-Fabric-dbd0b4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](#)
[![GitHub Release](https://img.shields.io/github/v/release/shy19805/minecraft-love-you?sort=semver)](https://github.com/shy19805/minecraft-love-you/releases)
[![GitHub Stars](https://img.shields.io/github/stars/shy19805/minecraft-love-you?style=social)](https://github.com/shy19805/minecraft-love-you)

**我的世界爱你**（World Loves You）是一个面向 Minecraft 26.2（Fabric）的轻量、高性能、可配置的生存与玩法增强模组，把 **40 个实用小功能**整合进一个模组，所有功能均可独立开关、按需调整。

**World Loves You** is a lightweight, high-performance, fully-configurable survival & gameplay enhancement mod for Minecraft 26.2 (Fabric). It packs **40 practical features** into one mod — each individually toggleable and tunable to your taste.

> 🆕 v1.0.0：**中英双语**（简体中文 / English）——界面、配置、指令、提示全部随游戏语言切换
> 40 个功能模块 / 4 大分组 / 全部可在游戏内独立开关 / 生成类改动默认关闭
> 40 feature modules · 4 categories · every toggle independent · generation-modifying features off by default

---

## 📥 下载与安装 / Download & Install

| 需求 / Requirement | 版本 / Version |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.x（推荐 0.19.5） |
| Fabric API | 0.155.2+26.2 |

1. 安装 [Fabric Loader](https://fabricmc.net/) 与 [Fabric API](https://modrinth.com/mod/fabric-api)
2. 从 **[GitHub Releases](https://github.com/shy19805/minecraft-love-you/releases)** 下载最新 jar（或自行构建，见下文）
3. 将 jar 放入 `.minecraft/mods/`
4. 启动游戏，在 Mod 菜单中按需配置

Install [Fabric Loader](https://fabricmc.net/) and [Fabric API](https://modrinth.com/mod/fabric-api), download the latest jar from **GitHub Releases**, drop it into `.minecraft/mods/`, and configure in-game via Mod Menu.

---

## ✨ 功能模块一览 / Feature Modules

### ⚔️ 战斗与规则 / Combat & Rules

| 模块 Module | 功能 / Feature |
|---|---|
| 力量射击 Powershot | 投掷物初速倍率可调（0~10），可选的投掷物伤害（0~50，默认关）。Adjustable throwable velocity (0–10); optional damage (0–50, off). |
| 更好的攀爬 Climbing | 攀爬速度倍率（0~10，默认2）+ 俯视加速下滑。Climb speed + look-down slide. |
| 无限箭 Infinity Arrows | 弓箭不再消耗弹药。Arrows never run out. |
| 友军保护 Friendly Fire | 关闭友军误伤。Disable friendly fire. |
| 苦力怕不爆炸 Creeper No Explode | 苦力怕完全不再爆炸。Creepers never explode. |
| 恶魂不破坏 Ghast No Grief | 恶魂火球不破坏方块（保留伤害）。No ghast fireball grief. |
| 末影人不搬方块 Enderman No Grief | 末影人不能搬走方块。Endermen can't carry blocks. |
| 耕地防踩踏 Farmland Protect | 生物不再踩坏耕地。Farmland trample protection. |
| 刷新附魔 Reroll | 附魔台潜行右键消耗经验，重掷三个附魔选项。Sneak-right-click to reroll enchants. |
| 清附魔 Enchant Clear | `/enchant-clear [includeCurse]` 清除手持物品附魔。Clear enchantments from the held item. |
| 附魔直显 Enchant Reveal | 悬停附魔台档位显示全部候选。Hover enchant slots to see all candidates. |
| 冲突附魔共存 Enchant Tweak | 相互冲突的附魔可以共存。Conflicting enchants can coexist. |
| 附魔等级突破 Enchant Level Break | 突破原版附魔等级上限。Break vanilla level caps. |
| 全键无冲 Non-Conflicting Keys | 消除按键冲突警告。Remove duplicate-keybind warnings. |
| 移除受击无敌帧 Rend Invuln | 移除受击无敌帧（兼容近战连击）。Remove i-frames (melee combo friendly). |

### 🌾 农业与生存 / Farming & Survival

| 模块 Module | 功能 / Feature |
|---|---|
| 右键收获并补种 Harvest | 一键连锁收获 + 自动补种，上限可调。Chain-harvest & auto-replant, caps configurable. |
| 满饱仍可吃 Eat Full | 饱和度可积累到自定义上限（默认100）。Saturation cap configurable (default 100). |
| 快吃 Eat Fast | 进食速度倍率（0~2，0=瞬间）。Eating speed multiplier (0–2). |
| 墓碑 Graves | 死亡暂存背包与经验，不遮挡邻块面。Inventory & XP stored on death; noOcclusion. |
| 最大生命修复 Max Health Fix | 重进世界后最大生命不再塌陷。Max HP no longer collapses on rejoin. |
| 经验修补 Mend Hold | 潜行右键用自身经验修复工具。Sneak-right-click to repair with XP. |
| 信标填充 Beacon Fill | 信标自动填充。Beacons auto-fill. |
| 藤蔓延伸 Vine Extend | 藤蔓与梯子沿墙延伸生长。Vines & ladders extend down walls. |
| 开放水域 Open Water | 玩家周围水域视为开放（钓鱼/刷怪）。Water near you counts as open. |
| 雨雪积雪 Snow Cap | 雨天自动积雪。Rain builds up snow layers. |
| 平滑基岩 Smooth Bedrock | 主世界基岩压平为单层（默认关）。Flat single-layer bedrock (off by default). |

### 📦 物品与便利 / Items & Convenience

| 模块 Module | 功能 / Feature |
|---|---|
| 大堆叠 Big Stacks | 堆叠上限可调（默认999，最大1000万）+ 科学计数显示。Configurable stack cap (999–10M) + compact number display. |
| 一键整理 Sort | 一键整理背包/箱子（快捷栏不动）。One-key sort (hotbar untouched). |
| 点击穿透 Clickthrough | 右键穿透告示牌/展示框操作后方方块。Click through signs/item frames. |
| 智能提示 Smart Tooltip | Tooltip 显示耐久/堆叠/物品ID。Durability, stack count and item ID in tooltips. |
| 交易显示 Trade Display | 村民交易详情、库存、折扣一目了然。Trade details, stock & discounts. |
| 喷溅奶瓶 Splash Milk | 可投掷的喷溅奶瓶（清除状态效果）。Throwable splash milk (clears effects). |
| 测距尺 Ruler | 精确测量两点距离。Measure distances precisely. |
| 去实验警告 No Exp Warn | 去除世界创建的实验设置警告。Skip experimental-settings warning. |
| 村民可见标记 Visible Traders | 显示村民全部交易/职业。Show all villager trades. |
| 连接纹理 CTM | 玻璃无缝连接纹理。Seamless glass textures. |

### 🐾 生物与探索 / Mobs & Exploration

| 模块 Module | 功能 / Feature |
|---|---|
| 驯服避险 Tame Safety | 宠物自动远离岩浆/火/仙人掌，溺水上浮。Pets avoid hazards & surface when drowning. |
| 绿宝石吸引 Emerald Lure | 手持绿宝石吸引村民跟随。Hold emeralds to lure villagers. |
| 刷怪笼增强 Spawner Plus | 刷怪笼采集与放置增强。Spawner pick-up & placement enhancement. |

---

## 🎮 指令 / Commands

| 指令 / Command | 说明 / Description |
|---|---|
| `/wly list` | 列出全部模块及开关状态。List all modules with on/off status. |
| `/wly keys list` | 列出主动功能键位及当前绑定。List active-feature key binds. |
| `/enchant-clear [includeCurse]` | 清除手持物品附魔（可选是否连诅咒一起清）。Clear held item's enchantments (optionally including curses). |
| `/bigstacktest <count>` | 调试用：把主手物品数量改为指定值。Debug: set main-hand item count. |

---

## ⚙️ 配置 / Configuration

- **游戏内配置（Mod Menu → Cloth Config）**：所有模块按 **4 大分组**排列；**联动配置项紧邻其主开关下方**（如：力量射击开关 → 初速倍率 → 投掷物伤害开关 → 伤害上限）。
  In-game config: modules grouped into **4 categories**; **linked sub-options sit directly under their main toggle**.
- **配置文件**：`config/minecraft_love_you.json`，所有自定义值重启后保留。
  Config file `config/minecraft_love_you.json` — values persist across restarts.
- **按键**：主动功能按键在游戏内"按键绑定"（Key Binds）中修改。
  Active-feature keys are rebindable in vanilla Key Binds.

---

## 🛠️ 开发 / Development

```bash
# 环境：JDK 25 · Loom 1.17.1 · Gradle 8.12（wrapper）
./gradlew.bat build --no-daemon        # 产物在 build/libs/
./gradlew.bat runServer --no-daemon    # 启动开发服务器

# 服务端运行时自检（12 项断言：大堆叠/墓碑遮挡/力量射击伤害/语言键对齐等）
WLY_SELFTEST=1 ./gradlew.bat runServer --no-daemon
```

- 技术栈：Minecraft 26.2 · Fabric Loader 0.19.x · Fabric API 0.155.2+26.2 · Cloth Config · Mod Menu
- 代码结构：`core/`（模块框架/网络/配置数据）· `module/<功能>/`（各功能模块）· `mixin/`（底层注入）· `client/`（客户端逻辑）
- 语言：`assets/minecraft_love_you/lang/`（`en_us.json` + `zh_cn.json`，键集对齐；SelfTest T10 自动校验）

---

## 📜 版本历史 / Changelog

### v1.0.0（2026）
- 🆕 **中英双语**：en_us 语言文件补齐 41 个模块键 + 配置 GUI + 指令 + 提示，界面随游戏语言切换
- 🆕 配置界面全面使用翻译键（模块名/分组/联动项/tooltip 不再显示裸键名）
- 🆕 元数据双语文案 + GitHub 仓库链接
- 🔧 修复：36 个模块 `displayName()` 缺失闭合括号的编译错误
- 🔧 修复：demo 模块 / eztweak 4 子规则名称改为语言键
- 🔧 修复：墓碑容器名、Xaero 路点名、村民交易提示、刷新附魔按钮等改为可翻译组件

### v0.1.0（首个发布版）
- 40 个功能模块、分组配置 GUI、/wly 指令、服务端自检套件

---

## 📜 许可证 / License

MIT © 我的世界爱你（World Loves You）— 自由使用、修改与分发（注明出处即可）。
MIT © World Loves You — free to use, modify and distribute with attribution.