# 我的世界爱你 / World Loves You

一个面向 Minecraft 26.2（Fabric）的轻量、可配置的生存/玩法增强模组。
A lightweight, fully-configurable survival/gameplay enhancement mod for Minecraft 26.2 (Fabric).

> 40 个功能模块，全部可在游戏内 Mod 配置界面单独开关；默认关闭生成类改动功能。
> 40 feature modules, each individually toggleable in the in-game Mod Config screen; generation-modifying features are off by default.

---

## 🌟 特色 / Features

### ⚔️ 战斗与规则 / Combat & Rules
- **力量射击（Powershot）** — 投掷物初速倍率可调（0~10，0=没力气只扔脚底，10=十倍速）；可选的投掷物伤害子功能（伤害随初速增长，0~50，默认关闭）。
  Adjustable throwable launch velocity (0–10: 0 = no strength, throws at your feet; 10 = 10× speed). Optional throwable damage sub-feature (scales with velocity, 0–50, off by default).
- **更好的攀爬（Climbing）** — 攀爬速度倍率（0~10）+ 俯视加速下滑。
  Climb speed multiplier (0–10) + faster slide when looking down.
- **无限箭（Infinity Arrows）** — 弓箭无限弹药。Infinite arrows.
- **友军伤害控制（Friendly Fire）** — 关闭友军伤害。Disable friendly fire.
- **苦力怕/恶魂/末影人/耕地保护（eztweak）** — 苦力怕不炸地形、恶魂不破坏、末影人不搬方块、耕地防践踏。
  No creeper terrain damage, no ghast griefing, no enderman block theft, farmland trample protection.
- **刷新附魔（Reroll）** — 附魔台潜行右键重掷三选项。Sneak-right-click enchanting table to reroll options.
- **附魔显示增强（Enchant Reveal / Full Name / Compatible）** — 附魔效果直显、非冲突键位、兼容提示。
  Reveal enchantment effects, non-conflicting keys, compatibility hints.

### 🌾 农业与生存 / Farming & Survival
- **右键收获并补种（Harvest）** — 一键连锁收获+补种（可调上限/同种限制/种植上限）。
  One-click chain harvest + replant (configurable chain limit, same-species, plant limit).
- **满饱仍可吃（Eat Full）** — 饱和度可积累至自定义上限（默认100）。Saturation up to a configurable cap (default 100).
- **快吃（Eat Fast）** — 进食速度倍率（0~2）。Eating speed multiplier (0–2).
- **墓碑（Graves）** — 死亡掉落暂存墓碑，不遮挡邻块面（noOcclusion）。Death loot stored in a grave, non-occluding (noOcclusion).
- **锄头回耐/信标填充/藤蔓延伸/开放水域/雪覆盖/平滑基岩** — 农田工具修复、自动填充信标、藤蔓蔓延、鱼类开放水域、下雪积雪、主世界基岩压平为单层（默认关）。
  Mending-hold, beacon auto-fill, vine extension, open-water fishing, snow accumulation, flat single-layer bedrock (default off).

### 📦 物品与便利 / Items & Convenience
- **大堆叠（Big Stacks）** — 堆叠上限可调（默认999，最大1000万），科学计数法显示（1000→1K、100万→1M）+ 字号自适应。
  Configurable stack size (default 999, max 10M), scientific notation display (1000→1K, 1M) with auto font scaling.
- **一键整理（Sort）** — 按键整理背包/箱子（快捷栏不动）。Keybind to sort inventory/chests (hotbar untouched).
- **点击穿透（Clickthrough）** — 右键穿透告示牌操作后方方块/展示框。Right-click through signs to interact behind them / item frames.
- **智能提示（Smart Tooltip）** — 物品耐久/堆叠/ID tooltip。Item durability/stack/id tooltips.
- **交易显示（Trade Display）** — 村民交易详情与库存显示。Villager trade details & stock display.
- **奶瓶/测距尺/经验警告/村民可见** — 喷溅奶瓶、测距尺、低经验警告、村民标记。
  Splash milk, distance ruler, no-exp-warning, visible traders.

### 🐾 生物与探索 / Mobs & Exploration
- **驯服避险（Tame Safety）** — 驯服宠物自动远离岩浆/火/仙人掌等危险。Pets auto-avoid lava/fire/cactus hazards.
- **绿宝石吸引（Emerald Lure）** — 手持绿宝石吸引村民跟随。Holding emeralds lures villagers.
- **刷怪笼增强（Spawner Plus）** — 刷怪笼采集/放置增强。Spawner pick-up & placement enhancement.

---

## ⚙️ 配置 / Configuration

- 游戏内 Mod 菜单（Cloth Config）：所有模块按 **4 大分组**（战斗与规则 / 农业与生存 / 物品与便利 / 生物与探索）排列，联动配置项紧邻其主开关下方。
  In-game Mod Menu (Cloth Config): all modules grouped into **4 categories** (Combat & Rules / Farming & Survival / Items & Convenience / Mobs & Exploration), with linked sub-options placed right below their main toggle.
- 配置文件：`config/minecraft_love_you.json`，重启保留所有自定义值。
  Config file: `config/minecraft_love_you.json` — all custom values persist across restarts.
- 主动功能按键在游戏内"按键绑定"中修改。Active feature keys are rebindable in vanilla Key Binds.

## 🛠️ 开发 / Development

- 环境：Minecraft 26.2 · Fabric Loader 0.19.x · Fabric API 0.155.2+26.2 · Loom 1.17.1 · JDK 25
- 构建：`./gradlew.bat build --no-daemon`（产物在 `build/libs/`）
- 服务端自检：`WLY_SELFTEST=1 ./gradlew.bat runServer --no-daemon`（12 项运行时断言：大堆叠/墓碑/力量射击伤害等）
  Server self-test: run with `WLY_SELFTEST=1` — 12 runtime assertions (big stacks, grave occlusion, powershot damage, etc.)

## 📜 许可证 / License

MIT © 我的世界爱你（World Loves You）