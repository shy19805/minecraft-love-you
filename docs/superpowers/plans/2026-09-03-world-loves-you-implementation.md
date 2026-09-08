# “世界爱你”实施计划（2026-09-03）

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付“世界爱你”Fabric 26.2 模组：36 项功能全部模块化、可配置开关（默认全开）、主动功能可配快捷键，仅依赖 钠+模组菜单+Cloth Config。

**Architecture:** 单 Gradle 工程；核心为 `WlyModule/ModuleManager/WlyConfig/WlyKeys` 框架；每个功能一个独立包 `module/<id>`，事件优先、最少 mixin，所有 hook 首行做 `enabled` 门控；配置页由 Cloth Config + Mod Menu 提供。

**Tech Stack:** Minecraft 26.2 / Fabric Loader 0.19.3 / Fabric API 0.155.2+26.2 / fabric-loom 1.17.1 / Gradle 9.5.1 / JDK 25 / sodium-fabric 0.9.1 / modmenu 20.0.1 / cloth-config 26.2.155+fabric。

## Global Constraints

- mod id `world_loves_you`，包名 `com.worldlovesyou`，模组名“世界爱你”。
- 菜单/配置/日志/代码注释全部中文。
- 每个功能模块必须有 `enabled`（默认 true）；`disabled` 时行为与未安装一致。
- 主动触发类功能必须注册 `KeyBinding` 且可改键（vanilla 键位页即可）。
- 混入文件全部登记在 `world_loves_you.mixins.json`，`defaultRequire = 0`。
- 每批结束时 `gradlew.bat build` 必须通过（JAVA_HOME=jdk-25），产出 jar 部署到 `E:\.minecraft\versions\26.2-Fabric 0.19.3\mods`。
- 单个功能模块的实现文件全部位于 `src/main/java/com/worldlovesyou/module/<id>/`，配置段命名 `<id> 配置`。
- 不复制任何原模组代码；原页面未明确的细节按温和默认实现并在代码注释标 `// 待与用户确认`。

---

## 文件结构（先锁定）

```
F:\AI项目\world_loves_you\
├─ build.gradle  settings.gradle  gradle.properties  gradle/wrapper/*  gradlew(.bat)
├─ src/main/resources/fabric.mod.json
├─ src/main/resources/world_loves_you.mixins.json
├─ src/main/resources/assets/world_loves_you/lang/zh_cn.json
├─ src/main/java/com/worldlovesyou/
│  ├─ WorldLovesYouMod.java          (ModInitializer)
│  ├─ WorldLovesYouClient.java       (ClientModInitializer)
│  ├─ core/ WlyModule.java, ModuleManager.java, ModuleContext.java
│  ├─ config/ WlyConfig.java, ConfigScreenFactory.java(ModMenu 入口)
│  ├─ client/ WlyKeys.java
│  └─ module/<id>/ <Feature>.java + <Feature>Config.java + mixin/* (按需)
```

## 核心接口（B0 定稿，后续任务全部依赖）

```java
// core/WlyModule.java
public interface WlyModule {
    String id();
    default boolean clientOnly() { return false; }
    void init(ModuleContext ctx);            // 服务端/双端
    default void initClient(ModuleContext ctx) {}
}

// core/ModuleManager.java（核心逻辑）
public class ModuleManager {
    public static void register(WlyModule m);
    public static boolean enabled(String id);          // = WlyConfig.enabled(id)
    public static void initAll(boolean client);        // 逐个 try/catch，失败日志+标记禁用
    public static List<String> ids();
}

// config/WlyConfig.java：静态门面 + Cloth Config 构建
public class WlyConfig {
    public static boolean enabled(String id);
    public static ClothConfig.ScreenFactory<?> screen(); // 供 ModMenu 调用
}

// client/WlyKeys.java
public class WlyKeys {
    public static KeyBinding register(String moduleId, String name, int defaultKey); // KeyConflictContext.UNIVERSAL
    public static boolean active(String moduleId); // enabled && isDown
}
```

---

## 批 B0：工程骨架 + 模块框架（必须最先完成）

### Task 0.1 工程初始化

**Files:** Create `settings.gradle`, `gradle.properties`, `build.gradle`, `gradle/wrapper/`（从 diablo_affixes 拷贝 gradlew 与 wrapper 配置）、`src/main/resources/fabric.mod.json`

**Interfaces:** 无依赖。
**Produces:** 可运行 `gradlew.bat build` 的空模组 jar。

- [ ] Step 1: gradle.properties 写入 `minecraft_version=26.2 loader_version=0.19.3 fabric_version=0.155.2+26.2 mod_version=0.1.0 java_version=25`
- [ ] Step 2: build.gradle 复刻本机 diablo_affixes 工程依赖，追加：`"com.terraformersmc:modmenu:20.0.1"`（implementation）、`"me.shedaniel.cloth:cloth-config-fabric:26.2.155+fabric"`（implementation，maven: `https://maven.shedaniel.me/`）、`"net.caffeinemc:sodium:fabric-0.9.1+mc26.2"`（compileOnly，maven: `https://maven.caffeinemc.org/releases`）——钠坐标以 Modrinth 下载页为准，若拉不到改配本地文件依赖并注释说明。
- [ ] Step 3: fabric.mod.json 声明 entrypoints `main/com.worldlovesyou.WorldLovesYouMod`、`client/...WorldLovesYouClient`、`modmenu` 入口；depends `fabricloader>=0.16.0`、`fabric-api`、`minecraft~26.2`、`cloth-config`、`modmenu`、suggests `sodium`。
- [ ] Step 4: 运行 `gradlew.bat build`，首次联网拉依赖；期望 BUILD SUCCESSFUL。
- [ ] Step 5: git init + commit "chore: 世界爱你 工程骨架"(若用户要求 git)。

### Task 0.2 模块框架 + 配置门面

**Files:** Create `core/WlyModule.java`、`core/ModuleManager.java`、`config/WlyConfig.java`

**Produces:** `ModuleManager.register/enabled/initAll/ids`；`WlyConfig.enabled(String)`。

- [ ] Step 1: 实现 `WlyConfig`：Cloth Config `ConfigBuilder`；总开关分类"模块开关"内为每个已注册模块生成 `Boolean` 条目（id+中文名，默认 true）；选项变更回调写 `config/world_loves_you.json`（Gson）并即时生效。
- [ ] Step 2: 实现 `ModuleManager`：register 去重、initAll 逐个 `try{ m.init(ctx) }catch(Throwable t){ LOGGER.error("[功能{} 初始化失败，已禁用]", m.id(), t); forced-off 集合记录 }`；`enabled(id)` 返回配置值 且 不在 forced-off。
- [ ] Step 3: `WorldLovesYouMod.onInitialize()` 调 `ModuleManager.initAll(false)`；`WorldLovesYouClient` 调 `initAll(true)`。
- [ ] Step 4: `/wly list` 指令（CommandRegistrationCallback）：列出全部模块与 enabled 状态；`/wly keys list` 列出键位。
- [ ] Step 5: 建 1 个示例模块 `module/demo/DemoModule`（init 打日志）验证框架。
- [ ] Step 6: `gradlew.bat build` 通过 + 部署测试实例验证日志出现“世界爱你 已初始化，N 个模块”。

### Task 0.3 键位基础设施 + 钠兼容 spike

**Files:** Create `client/WlyKeys.java`；`client/compat/SodiumCompat.java`（spike 探针类）

**Produces:** `WlyKeys.register(id,name,defaultKey)/active(id)`；钠可用性检测布尔。

- [ ] Step 1: WlyKeys 用 `KeyBindingHelper.registerKeyBinding`；默认键冲突时 LOGGER.warn。
- [ ] Step 2: SodiumCompat 启动时探测 `net.caffeinemc.*` 类存在与否，把结果写 `WlyConfig.sodiumPresent()`（供渲染类模块分支）。
- [ ] Step 3: 测试实例验证键位出现在“按键绑定”列表、`/wly keys list` 可查。
- [ ] Step 4: commit "feat: 模块框架/配置/键位/钠探测"。

---

## 批 B1：低风险速胜（10 功能）

### Task 1.1 最大生命修复（maxhealth-fix）【S】
**Files:** `module/maxhealthfix/MaxHealthFixModule.java` + `MaxHealthFixMixin.java`（若需要）
**锚点:** 玩家登录/重生时，取其装备属性修饰提供的 `MAX_HEALTH` 重算并 `setHealth(min(cur, max))`；用 `ServerPlayConnectionEvents.JOIN` + `attribute` 重算即可，无 mixin。
**配置:** `enabled`（默认 true）。
**验收:** 穿+生命装备退出重进，血量上限仍保留；关闭后行为回到原版。

### Task 1.2 全局规则锁定（ruler）【S】
**Files:** `module/ruler/RulerModule.java`
**锚点:** `ServerLifecycleEvents.SERVER_STARTED` + 每次 `ServerLevel` 加载时，读取 `config/world_loves_you/ruler.json`（`{"<ruleId>": value,...}`）写入该世界 `GameRules`；原版没有的规则跳过记 WARN。
**配置:** `enabled`、规则 JSON 路径（高级）。
**验收:** 配置 `doDaylightCycle:false` 后任意新老存档昼夜被锁定；关闭后不生效。

### Task 1.3 右键收获并补种（harvest）【B】
**Files:** `module/harvest/HarvestModule.java`（事件 + 客户端无逻辑）
**锚点:** `UseBlockCallback`：目标 `CropBlock`/`NetherWartBlock`/`CocoaBlock` 且 age==max → `level.getBlockState.getDrops` 掉落、`setBlock(age=0 state,3)`、消耗玩家背包 1 颗对应种子（没有则只收不种并提示）、可选锄头掉耐久、可选经验球、可选音效；`PASS` 其余情况。**连锁兼容:** 不拦截左键/连锁模组事件，仅在右键成熟作物时返回 `SUCCESS`，避免抢事件。
**配置:** enabled、requireHoe、damageOnHarvest、expOnHarvest、playSound、cropWhitelist（注册名列表，空=全部）。
**验收:** 右键成熟小麦收+补种；下界疣/可可豆可用；关 requireHoe 空手可收；与连锁挖矿同装测试不冲突（工作项）。

### Task 1.4 满饱仍可吃 + 饱和度增益（eat-when-full）【S】
**Files:** `module/eatfull/EatFullModule.java` + `EatFullMixin.java`
**锚点（26.2 名称待以 loom-cache 校核）:** M: `Player`（`canEat(boolean)` 恒真 when enabled）→ M: `FoodData`（`eat` 时按配置允许饱和度超过饥饿值与 20 上限）。
**配置:** enabled、allowOverflow、saturationCap（默认20，可调）。
**验收:** 满饥饿吃金胡萝卜仍可吃；溢出转饱和度；关闭后必须满格才能吃。

### Task 1.5 始终开放水域（open-water）【S】
**Files:** `module/openwater/OpenWaterModule.java` + `OpenWaterMixin.java`
**锚点:** M: `FishingHook.calculateOpenWater`（26.2 方法名以源码校核）恒真。
**配置:** enabled。
**验收:** 1 格水可钓宝藏；关闭后恢复原判定。

### Task 1.6 更好的信标放置（beacon-fill）【S】
**Files:** `module/beaconfill/BeaconFillModule.java`
**锚点:** `UseBlockCallback`（手持矿物方块右键 `BeaconBlock`）：按基座层几何放置方块（放不下则终止），潜行+右键放整组；`AFTER` 挖掘信标时破坏其基座并掉落（BlockBreakEvents）。
**配置:** enabled、breakBeaconBaseBlocks、dropReplacedBlockTopBeacon（沿用原模组键名）。
**验收:** 右键填层、替换块掉落在信标顶、破坏信标连带基座掉落；关闭功能恢复原版。

### Task 1.7 所有箭配无限（infinity-arrows）【S】
**Files:** `module/infarrows/InfArrowsModule.java` + `InfArrowsMixin.java`
**锚点:** M: `BowItem`/`CrossbowItem` 取箭与消耗逻辑：有"无限"时不要求普通箭、不消耗箭袋内箭。
**配置:** enabled。
**验收:** 无限弓配药箭不消耗药箭；普通行为在关闭后一致。

### Task 1.8 去实验性设置警告（no-exp-warn）【C】
**Files:** `module/noexpwarn/NoExpWarnModule.java` + mixin（26.2 弹窗创建点 B0 后先 spike 定位）
**锚点:** M: 创建/进入世界时实验性警告 Screen 的构造点（若为数据包警告则处理 `PackCombiner` 校验路径）。
**配置:** enabled。
**验收:** 无警告直接进世界；关闭后恢复弹窗。

### Task 1.9 墓碑（graves）【S】(从 B2 提前的常规项，视顺排关)
**Files:** `module/graves/GravesModule.java`、`GraveBlockEntity.java`
**锚点:** `ServerLivingEntityEvents.AFTER_DEATH`（玩家死亡）→ 生成墓碑方块+实体储存全部物品与经验；右键打开取回；墓碑长存/时间到爆出掉落（可配）。
**配置:** enabled、keepExperience、graveProtectTileTicks、dropOnExpire。
**验收:** 死亡生成墓碑、物品经验可取回；关闭后原版掉尸。

### Task 1.10 `/wly` 功能自检与回归单
**Files:** 无新文件（工具脚本 `tools/check_features.py`：解析 fabric.mod.json＋模块清单，比对计划表 36 项注册情况）
**验收:** B1 结束后 `gradlew build` + check_features.py 输出 36 项状态表（已实现/未实现）。

---

## 批 B2：常规交互（8 功能）

### Task 2.1 更好的经验修补（mend-hold）【S】
**锚点:** `UseItemCallback`（Shift+右键手持或装备栏"经验修补"物品）→ 按配置每 `mendCostPerLevel` 点经验修复 1 耐久，跨所有槽位（护甲/主手/副手）。键位固定 Shift+右键（不占 KeyBinding）。
**配置:** enabled、costPerDurability。
**验收:** Shift+右键修复、经验正确扣除；关闭后行为消失。

### Task 2.2 智能提示框（smart-tooltip）【C】
**锚点:** M: `GuiGraphics` tooltip 绘制（26.2 submit 渲染路径 spike）：先量整行物品 tooltip 宽度，超出 `maxWidth` 时文字按原组件逐行换行；仍超宽则 `PoseStack.scale(scalefac)` 等比例缩小（下限 `minScale`）；高度超屏时记录并调用新建 `VerticalTooltipScroll` 滚动偏移；M: `Screen.mouseScrolled` 在 hover 提示框时改滚动值。
**配置:** enabled、maxWidthPx、minScale、enableWheelScroll。
**验收:** 超宽 tooltip 换行+缩小+滚轮纵移；原版短 tooltip 显示不变；关闭后原样。

### Task 2.3 更好的攀爬（climbing）【B】
**锚点:** M: `Player.travel`/`aiStep`：检测 `onClimbable`；速度乘数、下看下滑加速、攀爬中可跳。纯客户端移动预测+服务器移动同步（服务端同样改以对齐）。
**配置:** enabled、speedMultiplier、allowJumpOnClimb、downSpeedMultiplier。
**验收:** 梯子/藤蔓爬速提升、可跳；关闭后原版手感。

### Task 2.4 友军保护（friendly-fire）【S】
**锚点:** M: `LivingEntity` 受击路径（`actuallyHurt`/`hurtOrSimulate`）：伤害来源=主人或同主人宠物→取消；幼年动物免疫除玩家潜行外全部；标签系统用配置黑/白名单替代数据包标签（简化）。
**配置:** enabled、protectPets、protectBabies、sneakBypass。
**验收:** 打宠物/幼崽不掉血，潜行可杀；非友方正常；关闭后恢复。

### Task 2.5 喷溅牛奶（splash-milk）【B】
**锚点:** 新 `SplashMilkItem extends ThrowableItem`+自定义 `MilkSplashEntity`（半径内 `removeAllEffects`+牛奶粒子）；合成配方（奶桶+火药 形状不限）；语言文件。
**配置:** enabled、giveRecipes、splashRange。
**验收:** 投掷命中清 buff；对队友/自己生效；关闭后物品不可得。

### Task 2.6 墓碑（graves）【S】
→ 已在 Task 1.9 提前，本槽位留空（若 1.9 未做则此处补齐）。

### Task 2.7 Easy Tweak 第一批规则（eztweak）【S】
**锚点:** 4 条低耦合规则先行：禁末影人搬方块（M: `Enderman` 拿取目标）、禁苦力怕破坏（M: `Creeper` 爆炸调用/爆炸事件过滤）、禁恶魂火球破坏（M: `LargeFireball`/爆炸事件）、防耕地踩踏（M: `FarmBlock.fallOn` 取消转化）。每条一个 mixin + 独立配置键。
**配置:** enabled + 每条规则单独开关（全部默认 true）。
**验收:** 逐规则开关生效；B4 再补齐余下规则。

### Task 2.8 经验/状态自检工具（bars 预留）——跳过，见 B4。

---

## 批 B3：附魔/交易 GUI 组（6 功能）

实现顺序：#7 reroll → #6 enchant-reveal → #18 visible-traders → #10 trade-display → #3 anvil-master → #9 enchant-tweak。

### Task 3.1 刷新附魔（reroll）【C】
**锚点:** M: `EnchantmentScreen.init` 注入按钮“刷新(键位)”；点击按配置扣除经验/青金石（`menu.cost` 改造：客户端预检查、服务器同步只发 `clickMenuButton`）；重掷显示三元组用本地随机种子即可（显示阶段客户端自愈）。
**配置:** enabled、experienceCost(默认1)、lapisCost(默认0)、key。
**验收:** 按钮刷新预览并扣费；关闭按钮消失。

### Task 3.2 附魔台显示全部潜在附魔（enchant-reveal）【C】
**锚点:** M: `EnchantmentScreen.render` 追加第二行文本：对每个选项调用 `EnchantmentHelper.getAvailableEnchantmentResults`（客户端可算）列出候选。
**配置:** enabled、rowLimit。
**验收:** 30 级除首附魔外显示其余候选；关闭恢复原样。

### Task 3.3 显示村民全部交易（visible-traders）【C】
**锚点:** M: `MerchantScreen` 交易按钮渲染：对未解锁项去除暗化/锁标（视觉仅限，不改服务器数据）。
**配置:** enabled。
**验收:** 未解锁交易直接可见；拿取仍受原限制？——规范：可见但点击仍走原版锁定（避免作弊争议；若需可买再做服务端同步，注记待确认）。

### Task 3.4 交易附魔显示（trade-display）【C】
**锚点:** M: `MerchantScreen` 渲染商品附魔信息；键 H 切换。
**配置:** enabled、toggleKey、showOverHeadMode、cycleSpeed。
**验收:** 查看村民时显示附魔信息、H 键切换；与原版/其他交易 GUI 无渲染竞争（提交前实测 diablo_affixes 环境）。

### Task 3.5 附魔大师（anvil-master）【B】
**锚点:** M: `AnvilMenu`（成本上限/too expensive 判定替换）、M: 附魔名称/等级显示（罗马数字）、`/enchant-clear entity|block` 指令。
**配置:** enabled、removeTooExpensive、maxCost、romanBelow、repairDiscount。
**验收:** 过高成本可继续、罗马数字显示、指令清附魔；关闭回到原版。

### Task 3.6 轻松附魔 1-4（enchant-tweak）【S】
**锚点:** ①冲突过滤 M: `EnchantmentHelper` 兼容判定；②无限不耗箭 M: 弓/弩取箭；③弓→弩生效 M: 附魔支持物品判定（like 铁砧可用性）;④经验修补扣经验 M: `MendingEnchantment` 处理路径。
**配置:** enabled + 4 子开关。
**验收:** 4 项逐开关验证；与原版附魔书/其他附魔模组叠装测试（标注兼容矩阵）。

---

## 批 B4：渲染与战斗组（7 功能）

### Task 4.1 更好的雪覆盖（snow-cap）【C】
**锚点:** `ModelLoadingPlugin` 修改块模型：被雪覆盖方块（顶部 is snow）烘焙追加一层 12/16 高雪帽 cube（客户端 only，钠兼容走 baked model）。
**配置:** enabled、blocks 列表。
**验收:** 下雪后草/土/木板顶上出雪层；关闭消失。

### Task 4.2 附魔等级上限突破2（elb）【B】
**锚点:** 附魔等级钳制点 M（26.2 以源码校准；含 `ItemStack` enchant 与 `EnchantmentHelper` combine）；`/cenchant` 指令；铁砧叠加与上限可配。
**配置:** enabled、maxLevelCap(2^31-1)、roman、mergeAllowed。
**验收:** >255 级可施加显示；超出上限回落；关闭恢复原版 255。

### Task 4.3 状态栏与血量（bars）【C】
**锚点:** E: `HudRenderCallback` 绘制条形（配置布局/配色）；M: 生物头顶血量（用 `LivingEntityRenderer` 名牌渲染点或纯投影 3D→2D 画条）。
**配置:** enabled、layout、colors、sizes、targetMobs、showExp/fatigue；键 F8 布局轮换、F10 开关 HUD。
**验收:** 条与血量显示、键位轮换；关闭完全消失；与钠叠加帧数误差 ≤2fps 抽测。

### Task 4.4 伤害免疫机制移除（rendi）【S】
**锚点:** M: 受击无敌帧清除/缩放；M: `Player.attack` 充能不足取消与击退过滤。
**配置:** enabled、IFramesMs、cancelBelowCharge、noKnockbackBelowCharge。
**验收:** 连击无无敌帧；充能阈值生效；关闭原版机制。

### Task 4.5 战利品光束（loot-beams）【C】
**锚点:** E: `WorldRenderEvents.AFTER_ENTITIES` 遍历 `ItemEntity` 画光柱+名牌（`VertexConsumer`，深度测试开）。
**配置:** enabled、beamColorMode、textOverlay、whitelist/blacklist、height。
**验收:** 掉落物出现光柱；颜色随名称；黑白名单；关闭消失。

### Task 4.6 弹道估计（trajectory）【C】
**锚点:** E: `WorldRenderEvents.LAST` 对手持发射物绘制预测曲线（立方块或线段样式），沿轨迹持续 raycast 检测实体命中播"叮"。
**配置:** enabled、renderStyle、color、size、perItem 开关、soundAlert。
**验收:** 拉弓出轨迹、样式/颜色配置生效；关闭消失。

### Task 4.7 刷怪笼增强 + 便携刷怪笼（spawner-plus / portable-spawner）【S/B 合并实现】
**锚点:** `UseBlockCallback` 刷怪蛋右击换类型；破坏掉刷怪笼（可配需求精准采集）；击杀掉落；新物品“便携刷怪笼”点击刷怪笼存类型、对地放置。
**配置:** 两个 enabled 与各自子项。
**验收:** 全流程验证 + 关闭全消失。

---

## 批 B5：高风险收尾（6 功能）

### Task 5.1 连接纹理（ctm）【C】
**锚点:** 钠 26.2 的模型加载/渲染 API spike 先行失败则降级 MVP：仅“玻璃板剔面修复+单材质块可选连接纹理（相邻同方块共享边框平滑）”，后续再扩 OptiFine CTM 子集。
**配置:** enabled、glassFix、simpleCTM。
**验收:** 玻璃板无接缝、连接纹理开/关对比；与钠共存帧率抽查。

### Task 5.2 超级堆叠（big-stacks）【B】
**锚点:** M: `ItemStack` 最大堆叠读取（无耐久物品按配置上限，默认 1,000,000，int 上限兜底）；M: 使用类物品（桶/汤）`finishUsingItem` 只 -1；背包/容器同步与渲染性能验证（≥1 万栈压测；异常则上限标实验）。
**配置:** enabled、globalMax、useItemsShrinkOne、blacklist。
**验收:** 无耐久物堆至百万级；水桶/蘑菇煲单次消耗；关闭恢复 64/16。

### Task 5.3 未加载区块活动模拟（unloaded-activity）【S】
**锚点:** M: `ServerLevel` 区块加载回调→按配置 tick 每类方块实体（熔炉进度、作物年龄、糖/竹子成长、铜氧化、海龟蛋孵化等）用"快速老化"函数补算，步数上限防卡顿。
**配置:** enabled＋每类开关＋maxCatchUpTicks。
**验收:** 远行返回后作物/熔炉如未离开；关闭后原样。

### Task 5.4 Clickthrough 2.0（clickthrough）【B】
**锚点:** 客户端 M: 拾取射线跳过告示牌/展示框方块与实体，命中后方容器则交互；服务端可选补丁保证合法性（如果能纯客户端完成则免服务端）。
**配置:** enabled、filters。
**验收:** 点展示框后箱子可开；原地空点仍正常编辑告示牌（潜行）。

### Task 5.5 平滑基岩层（smooth-bedrock）【S】
**锚点:** M: `ChunkGenerator`（主世界/下界/末地）基岩层生成替换为固定 y 平整层；已有区块不回溯（只影响新生成）。
**配置:** enabled、perDimension 开关。
**验收:** 边境底层平整；三界开关分别生效。

### Task 5.6 Powershot 蓄力投掷（powershot）【B】
**锚点:** 客户端 KeyBinding（默认长按右键不注册键位，用右键长按状态机）：蓄力期间显示力度条（HUD），松开发自定义包；服务端接收后生成投掷实体并按力度设置初速/弧线；可选中方块/物品目标（仅对可投掷物品启用）。
**配置:** enabled、maxChargeTicks、maxRange、throwBlocks。
**验收:** 蓄力→投掷、距离与力度正比；物品投出后对手可见；关闭恢复原交互。

---

## 验收与回归总纲

1. 每批结束：`gradlew.bat build` 通过；`tools/check_features.py` 36 项状态表导出；jar 进测试实例。
2. 每模块关闭后不残留行为（回归项）。
3. B3 结束做 附魔/交易 GUI 兼容矩阵；B4 结束做 渲染帧率抽查（钠开/关）。
4. 全功能交付时与用户现役整合包（含暗黑词条）共载冒烟，输出冲突清单与处置。

**风险预案：** B5 的两项（5.2 大堆叠、5.3 离线模拟）若 spike 不通过，按上述标注降级或进入"实验性"状态；不阻塞其它模块交付。