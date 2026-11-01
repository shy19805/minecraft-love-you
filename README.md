# 我的世界爱你 · World Loves You

[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.2-00aa00)](https://www.minecraft.net/)
[![Modloader](https://img.shields.io/badge/Modloader-Fabric-dbd0b4)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](#)
[![GitHub Release](https://img.shields.io/github/v/release/shy19805/minecraft-love-you?sort=semver)](https://github.com/shy19805/minecraft-love-you/releases)
[![GitHub Stars](https://img.shields.io/github/stars/shy19805/minecraft-love-you?style=social)](https://github.com/shy19805/minecraft-love-you)

**我的世界爱你**（World Loves You）是一个面向 Minecraft **26.1.2 / 26.2**（Fabric）的轻量、高性能、可配置的生存与玩法增强模组，把 **41 个实用小功能**整合进一个模组，所有功能均可独立开关、按需调整。

**World Loves You** is a lightweight, high-performance, fully-configurable survival & gameplay enhancement mod for Minecraft **26.1.2 / 26.2** (Fabric). It packs **42 practical features** into one mod — each individually toggleable and tunable to your taste.

> 🆕 v1.0.0：**中英双语**（简体中文 / English）——界面、配置、指令、提示全部随游戏语言切换
> 42 个功能模块 / 4 大分组 / 全部可在游戏内独立开关 / 生成类改动与规则类改动默认关闭
> 42 feature modules · 4 categories · every toggle independent · generation-modifying and rule-modifying features off by default

---

## 📥 下载与安装 / Download & Install

| 需求 / Requirement | 版本 / Version |
|---|---|
| Minecraft | **26.1.2 ~ 26.2**（同一个 jar 两版通用） |
| Fabric Loader | 0.19.x |
| Fabric API | 0.155.2+26.1.2 / 0.155.2+26.2 |
| Java | 25 |

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
| 力量射击 Powershot | **仅对玩家与玩家的宠物**投出的投掷物生效：初速倍率可调（0~10），可选伤害（0~50，默认关）。**Players & their pets only**: adjustable throwable velocity (0–10); optional damage (0–50, off). |
| 更好的攀爬 Climbing | 攀爬速度倍率（0~10，默认2）+ 俯视加速下滑。Climb speed + look-down slide. |
| 无限箭 Infinity Arrows | 弓箭不再消耗弹药；可另开「免箭矢库存」——背包一支箭都没有也能射。Arrows never run out; optionally fire with zero arrows in stock. |
| 友军保护 Friendly Fire | 关闭友军误伤。Disable friendly fire. |
| 苦力怕不爆炸 Creeper No Explode | 苦力怕完全不再爆炸（**默认关**）。Creepers never explode (off by default). |
| 恶魂不破坏 Ghast No Grief | 恶魂火球不破坏方块，保留伤害（**默认关**）。No ghast fireball grief (off by default). |
| 末影人不搬方块 Enderman No Grief | 末影人不能搬走方块（**默认关**）。Endermen can't carry blocks (off by default). |
| 耕地防踩踏 Farmland Protect | 生物不再踩坏耕地（**默认关**）。Farmland trample protection (off by default). |
| 刷新附魔 Reroll | 附魔台潜行右键消耗经验，重掷三个附魔选项。Sneak-right-click to reroll enchants. |
| 清附魔 Enchant Clear | `/enchant-clear [includeCurse]` 清除手持物品附魔。Clear enchantments from the held item. |
| 附魔直显 Enchant Reveal | 悬停附魔台档位显示全部候选。Hover enchant slots to see all candidates. |
| 冲突附魔共存 Enchant Tweak | 相互冲突的附魔可以共存。Conflicting enchants can coexist. |
| 附魔等级突破 Enchant Level Break | 突破原版附魔等级上限。Break vanilla level caps. |
| 铁砧无上限与清附魔 Anvil Master | 解除铁砧 40 级「过于昂贵」上限（经验照付）；附魔等级以罗马数字显示；`/enchant-clear` 清除主手全部附魔。Lifts the anvil's 40-level cap; Roman-numeral enchant levels; `/enchant-clear`. |
| 铁砧不会过于昂贵 Anvil No Limit | 铁砧不再因「过于昂贵」拒绝取件，界面上的「过于昂贵！」红字一并消失。No more "Too Expensive!" refusal or label. |
| 铁砧修复 Anvil Repair | 手持铁锭**左键**铁砧，消耗铁锭升一档：损坏 → 开裂 → 完好（默认每档 3 锭，可调）。拿着铁锭左键铁砧**永远不会把它挖掉**：已完好则无反应，铁锭不足则动作栏提示；没拿铁锭时交回原版。Left-click an anvil with iron ingots to upgrade one damage tier (3 ingots per tier by default). Holding ingots never breaks the anvil. |
| 经验值计费 XP-Point Pricing | 铁砧与附魔台改为按**经验值点数**结算（门槛与扣费都换成经验条上的真实点数）。Anvil & enchanting table charge XP **points** instead of levels. |
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
| 离线追赶 Offline Catch-Up | 凡是「现实时间在流逝、但世界没推进」的时段都补齐：停机/崩溃/区块卸载、暂停或最小化挂机、电脑睡眠、跳过夜晚。Furnaces smelt, crops grow, babies age during unloaded / paused / slept-through time; caught up on return. |

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
| 去实验警告 No Exp Warn | 创建世界与**进入已有世界**都不再弹实验性/自定义世界警告。Skips the experimental / custom-world warnings when creating **and opening** a world. |
| 村民可见标记 Visible Traders | 显示村民全部交易/职业。Show all villager trades. |
| 连接纹理 CTM | 玻璃无缝连接纹理。注意：这是**安装即生效的静态贴图覆盖**，配置页的开关只作标记、无法在游戏内撤销；要恢复原版玻璃请在「资源包」界面处理（改完按 F3+T 重载）。Seamless glass textures. Note: a static texture override that applies as soon as the mod is installed — the config toggle is a marker only; use the Resource Packs screen (then F3+T) to restore vanilla glass. |

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
# 环境：JDK 25 · Loom 1.17.1 · Gradle 9.5.1（wrapper）
./gradlew.bat build --no-daemon              # 26.1.2 基线（默认，兼容下限）→ build/libs/
./gradlew.bat build -Pmc=26.2 --no-daemon    # 26.2 基线（产物等价，用于 26.2 开发环境）
./gradlew.bat runServer --no-daemon          # 启动开发服务器

# 服务端运行时自检（12 项断言：大堆叠/墓碑遮挡/力量射击伤害/语言键对齐等）
WLY_SELFTEST=1 ./gradlew.bat runServer --no-daemon

# mixin 跨版本审计：两版 jar 上逐个 javap 核对注入点/影子字段
python tools/mixin_audit.py
```

- 技术栈：Minecraft **26.1.2 ~ 26.2**（同一 jar 通吃）· Fabric Loader 0.19.x · Fabric API 0.155.2 · Cloth Config · Mod Menu
- 双版本策略：构建基线钉在 **26.1.2**，编译器强制只用两版都存在的 API；两版实测只有 2 个分叉点，已由 `ClientCompat` 与 `FabricBlockEntityTypeBuilder` 兜住。详见 `docs/开发避坑手册.md` #47
- 代码结构：`core/`（模块框架/网络/配置数据）· `module/<功能>/`（各功能模块）· `mixin/`（底层注入）· `client/`（客户端逻辑）
- 语言：`assets/minecraft_love_you/lang/`（`en_us.json` + `zh_cn.json`，键集对齐；SelfTest T10 自动校验）

---

## 📜 版本历史 / Changelog

### v1.2.9（2026）

- 🐛 **补上 v1.2.8 那处兜底的溢出边界**：饵钓高到 `lureSpeed` 饱和成 `Integer.MAX_VALUE`（约 2100 万级，`(int)` 强转 float 会饱和）时，`lureSpeed + 1` 会**溢出回绕成负数**，兜底当场失效。现在在 `FishingHook.<init>` 入口把 `lureSpeed` 饱和到 **5000**（≈饵钓 50 级；掷点上限只有 600，≥600 后 `max(roll − lureSpeed, 1)` 已恒等于 1，再高无收益）。
  - 实现用 `@ModifyVariable(argsOnly = true, index = 4)` 改**构造参数的局部槽**，而不是往 `<init>` TAIL 注入赋值——后者会被 Mixin 放进独立 handler 方法，JVM 的 final 域校验按方法判定，第一次创建浮漂就 `IllegalAccessError`（本机实测，见手册 #76）。
  - **全区间实测**：自检 T17 现在逐档扫 `lureSpeed ∈ {0,100,300,500,600,800,5000,20000,Integer.MAX_VALUE}`，每档 300 次真构造真调用 `catchingFish`，断言最差值 ≥ 1；并断言喂进 `MAX_VALUE` 后字段确实被压成 5000。**结论：饵钓等级再高也不会再次失效。**
- 🔧 修 `tools/injection_audit.py` 误报：`<init>` / `<clinit>` 目标被一律判成"方法不存在"→ HIGH。javap 用**类的简单名**打印构造器、用 `static {};` 打印类初始化，脚本按字面名查必然查不到。同时记录该护栏的边界：**它只按方法名校验存在性，从不校验描述符**，所以描述符对不对仍须靠 `require=1` + 真跑（手册 #77）。
- 📖 手册追加 **#76**（mixin handler 是独立方法 → 写不了目标类 final 实例域，改构造参数而非赋值）与 **#77**（审计看不见 `<init>` 目标；及"静态审计绿 ≠ 注入绑对"）。
- 🎯 **作用域收敛**：`powershot` 的初速与命中伤害只对**玩家本人 + 玩家的宠物**生效（雪傀儡、驯服的狼/猫/马/羊驼等），怪物与发射器的投掷物回归原版；`rendi` 只清**非玩家阵营**生物的无敌帧，玩家与宠物保留受击无敌帧。两处共用 `core/PlayerSide.isPlayerOrPet()` 单点口径（手册 #52），自检 T12 用「无主雪球不伤 / 玩家雪球掉血」成对断言。
- ⚡ **启用状态缓存**：`ModuleManager.enabled()` 处在 mixin 热路径上（堆叠上限查询、每次受击、每次投掷、每 tick 攀爬），原本每次都进 `synchronized` 的配置装载再做两次哈希查找；现在首次解析后存入 `ConcurrentHashMap`，`put` / `reload` / `forceOff` 时整体失效。配置装载自身也改为 volatile 快路径 + 双重检查。
- 🧪 自检新增 **T18**：改开关 → `WlyModule.on()` 必须立刻翻转，改回必须复原（成对断言，防「缓存永不过期」这类静默失效）。
- 自检 **18 PASS / 0 FAIL**；`injection_audit --mc both` **66 项 HIGH 0 / MED 0**；`mixin_audit` 52 目标两版一致。

### v1.2.8（2026）

- 🐛 **修复：饵钓 ≥ 6 级的钓竿永远不咬钩**（新增 `mixin/LureTimerGuardMixin`，挂在 `elb` 上）。
  - **根因**：`FishingRodItem.use` 里 `lureSpeed = (int)(getFishingTimeReduction() × 20f)`，饵钓每级贡献 5.0 ⇒ `lureSpeed = 100 × 等级`；而 `catchingFish` 的播种式是 `timeUntilLured = nextInt(100,600) − lureSpeed`。饵钓 ≥ 6 时该值**恒 ≤ 0**，`timeUntilLured > 0` 分支永不进入 ⇒ `timeUntilHooked` 永不被赋值 ⇒ `nibble` 恒 0 ⇒ 不咬钩。原版封顶饵钓 3（300 < 600）所以碰不到，是 `elb` 把等级放开后掉进了原版没定义域的区间。
  - **修法**：只把这一次掷点改成 `max(roll, lureSpeed + 1)`，原版随后 `− lureSpeed` 自然得 `max(roll − lureSpeed, 1)`。饵钓越高咬钩越快（符合附魔语义），任何等级都不死锁；`nibble` 与 `timeUntilHooked` 两处掷点不动。
  - **凭据**：自检 T17 反射构造 `lureSpeed=800` 的浮漂真调 `catchingFish` —— 兜底开 400 次最小值 **1**，兜底关最小值 **−700**（=100−800，原版死锁值）。开/关成对断言，防止兜底判在假路径上恒绿。
- 🔧 新增只读诊断指令 **`/wly fish`**：把「浮漂为什么不咬钩」判定链上每道门按顺序打 YES/NO，并附 `nibble / timeUntilHooked / timeUntilLured / lureSpeed / openWater / outOfWaterTime / life` 实测值。本次根因就是靠它一次抛竿定位的（`lureSpeed=800`）。纯只读、不写世界、不主动打日志。
- 自检 **17 PASS / 0 FAIL**；`injection_audit --mc both` 65 项 HIGH 0 / MED 0；`mixin_audit` 52 目标两版一致。26.1.2 / 26.2 的 `catchingFish` 三处 `Mth.nextInt` 偏移量已逐个核对同构（ordinal 2 = `(100,600)`）。
- 📖 手册追加 **#74**（突破原版上限必须逐个核对下游公式定义域；以及**不许拿猜出来的常数做排除结论**）与 **#75**（反向锁要成对断言「关=复现原缺陷」）。

### v1.2.7（2026）

- 🆕 **无限不需要背包有箭**（`infinity-arrows` 的联动子项，默认开）：带「无限」附魔的弓 / 弩，即使背包里**一支箭都没有**也能上弦发射，射出的是一支不占库存的普通箭。配置界面在「所有箭配无限」开关正下方，关掉即回到原版（原版必须先留至少 1 支箭）。
  - 注入点选 `Player.getProjectile`：`javap` 26.1.2 确认 `BowItem.use`（能否开始拉弓）与 `BowItem.releaseUsing`（能否射出）**两处都以它返回 EMPTY 作为唯一的"没弹药"判据**，补在一处即两处全盖住；背包里只要有真弹药就仍然优先用真弹药（药箭 / 光灵箭不变）。
  - 两版 API 一致（`Player.getProjectile` / `ProjectileWeaponItem.draw` / `Enchantments.INFINITY` 均已逐项核对 26.1.2 ↔ 26.2）。
- 自检 16 PASS / 0 FAIL；`injection_audit --mc both` 64 项 HIGH 0 / MED 0；`mixin_audit` 51 目标两版声明一致。

### v1.2.6（2026）

- 🔧 **铁砧修复 `anvil-repair` 调整**（按反馈）：
  - 每档消耗铁锭默认值 5 → **3**。
  - **手持铁锭左键铁砧永远不会把铁砧挖掉**：铁砧已完好时左键**无反应**（原来会走原版挖掉），铁锭不足时只提示不修复。只有**没拿铁锭**时才交回原版（否则没法正常挖掉铁砧）。

### v1.2.5（2026）

- 🆕 **铁砧修复 `anvil-repair`**：手持铁锭**左键**铁砧 → 消耗铁锭把铁砧升一档（损坏 → 开裂 → 完好），每档消耗铁锭数可调（默认 3，范围 1~31）。走 `AttackBlockCallback`（左键铁砧），不再依赖任何按键位 —— 拿着铁锭左键铁砧永远不会把它挖掉；铁砧已完好时无反应；铁锭不足时动作栏提示所需数量；创造模式不扣铁锭。
- 🔧 修复：`en_us.json` 缺 `wly.module.anvil-no-limit` 键（英文界面显示裸键名）。自检 T10 从「抽样 contains」升级为「中英键集必须完全一致」，此类漏键不再可能溜过。
- 自检 16 PASS / 0 FAIL（新增 T16 断言铁砧修复映射链与朝向保留）。

### v1.2.4（2026）

- 🆕 **铁砧不会过于昂贵 `anvil-no-limit`**：铁砧不再因「过于昂贵」拒绝取件 —— 解除 40 级硬上限，也解除「待改造物品本身是堆叠时费用被钉死 40」的钳制；界面上的「过于昂贵！」红字一并消失。费用照付。
- 🆕 **经验值计费 `xp-points`**：铁砧与附魔台不再按「经验**等级**」结算，改为按「经验**值（点）**」结算 —— 门槛与扣费都换成经验条上的真实点数（费用 30 就只要 30 点，而不是 30 级）。覆盖 `AnvilMenu.mayPickup` / `onTake` 与 `EnchantmentMenu.clickMenuButton` / 其扣费 lambda。
- 🔧 修复：v1.2.4 开发中新增的两处 mixin `@At target` 写了冒号形式的方法描述符，导致启动期硬崩 `InvalidMemberDescriptorException`；已改为括号形式 `name(args)ret`（详见避坑手册 #69）。
- 🛠️ `tools/injection_audit.py` 新增描述符语法校验 `malformed_target()`，补齐「静态审计全绿但启动崩」的盲区。
- 自检 15 PASS / 0 FAIL（新增 T15 断言经验值换算：0 级=0 / 15 级=315 / 30 级=1395）。

### v1.1.0（2026）

- 🆕 **离线追赶 `offline-catchup`**（参考 Unloaded Activity）：凡是「现实时间在流逝、但世界没推进」的时段，回来时按现实时间一次性补偿。四条触发通路：①服务器停机 / 崩溃 / 强杀后重进；②跑远导致区块卸载后再回来；③**世界停摆**（暂停菜单、窗口失焦自动暂停、电脑睡眠休眠、长时间卡死——此时区块从未卸载，`CHUNK_LOAD` 不会触发）；④**用床跳过夜晚**（被跳过的世界时钟在原版里是直接抹掉的）。补偿内容：熔炉继续烧炼、营火继续烤、作物继续生长、幼年动物继续长大。停机时间戳落在存档目录、区块时间戳在内存（LRU 有界），取较新者；单区块 50ms 时间预算、每 tick 8ms 总预算。
- 力量射击作用域收窄：**只对玩家本人与玩家的宠物**投出的投掷物生效（速度与伤害）；箭 / 三叉戟 / 烟花不再受影响，怪物与发射器的投射物保持原版。
- 去实验警告补全「进入已有世界」路径：不再弹「实验性 / 自定义世界」的备份询问与实验性确认。
- 自检 T12 升级为双向断言（无主投掷物不造成伤害 / 玩家投掷物正常伤害）。

### v1.0.0（2026）
- 🆕 **中英双语**：en_us 语言文件补齐 41 个模块键 + 配置 GUI + 指令 + 提示，界面随游戏语言切换
- 🆕 配置界面全面使用翻译键（模块名/分组/联动项/tooltip 不再显示裸键名）
- 🆕 元数据双语文案 + GitHub 仓库链接
- 🔧 修复：36 个模块 `displayName()` 缺失闭合括号的编译错误
- 🔧 修复：demo 模块 / eztweak 4 子规则名称改为语言键
- 🔧 修复：墓碑容器名、Xaero 路点名、村民交易提示、刷新附魔按钮等改为可翻译组件

### v0.1.0（首个发布版）
- 42 个功能模块、分组配置 GUI、/wly 指令、服务端自检套件

---

## 📜 许可证 / License

MIT © 我的世界爱你（World Loves You）— 自由使用、修改与分发（注明出处即可）。
MIT © World Loves You — free to use, modify and distribute with attribution.