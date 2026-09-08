# “世界爱你”模组 — 设计规格（Spec）

> 目标：一个 Fabric 模组，把 36 项常用游戏体验功能合并为一个**模块化、互不影响、全部可配置开关（默认全开）、主动功能可配快捷键**的整合包，依赖最小化（钠 Sodium + 模组菜单 Mod Menu + Cloth Config，另含 Fabric API 基础依赖）。
> 来源：用户清单中的 mcmod.cn 模组功能（本模组为重新实现，不复制原模组代码；灵感来源在计划中逐条标注）。

## 1. 范围与硬性约束（Global Constraints）

| 项 | 值 |
|---|---|
| 模组名 | 世界爱你（WorldLovesYou） |
| Mod ID | `world_loves_you` |
| 包名 | `com.worldlovesyou` |
| MC 版本 | **26.2**（Fabric Loader 0.19.3，Fabric API 0.155.2+26.2） |
| 构建 | Fabric Loom 1.17.1，Gradle 9.5.1（wrapper），JDK 25，`options.release = 25` |
| 运行时依赖 | 钠 sodium-fabric（26.2，0.9.1 release），模组菜单 modmenu（20.0.1），Cloth Config API（26.2.155+fabric） |
| 语言/注释 | 全部中文（UI、配置名、代码注释） |
| 配置总开关 | 每个功能一个 `enabled`（默认 true）；功能内部选项默认值以"与原模组一致、体验温和"为准 |
| 快捷键 | 使用原版 KeyBinding + Fabric `key-binding-api-v1`；凡"主动触发类"功能必须可配置快捷键；默认键位见功能表 |
| 模块互不影响 | 任一功能关闭后，其事件监听与 mixin 必须在入口处直接返回；模块之间不共享可变状态；一个模块崩溃不影响其它模块（异常捕获+日志） |
| 交付节奏 | 按批次（B0→B5）实现，每批产出可玩 jar |
| 复刻深度 | 核心玩法 + 关键配置项（不 100% 复刻边缘细节；原页面未说明的细节按"温和默认"实现并标注待实测） |

## 2. 核心架构：模块化框架（B0 完成）

### 2.1 模块接口
每个功能 = 一个 `WlyModule` 实现，一个配置类，一个（或零个）mixin 组。

```java
package com.worldlovesyou.core;

public interface WlyModule {
    /** 模块唯一 id，对应配置 enabled 键名 */
    String id();
    /** 默认 true */
    default boolean enabledByDefault() { return true; }
    /** 只读配置门控：false 时所有 hook 直接返回 */
    static boolean on(String moduleId) { return WlyConfig.enabled(moduleId); }
    void init();          // 服务端/双端注册（内部第一行必须检查 WlyModule.on(id)）
    default void initClient() {}
}
```

`ModuleManager`：注册列表、统一调用 `init/initClient`、把每个模块的配置段挂进 Cloth Config、把每个模块的 KeyBinding 批量注册；**异常隔离**：单个模块初始化失败仅记日志并置 disabled，不中断其它模块。

### 2.2 配置系统（Cloth Config + Mod Menu）
- 单一配置类 `WlyConfig`，`config/world_loves_you.json`。
- 结构：顶部分类"总开关"（每模块一行 enabled）；各模块独占子分类（分类名=功能名），只包含自己的选项。
- `ModMenuApi` 实现：`ModMenuConfigPlugin` 返回 Cloth Config 屏幕，并附"重置为默认"入口。
- 改动即时生效（`save()` 回调），个别注释标注"重启生效"。

### 2.3 快捷键
- `WlyKeys`：每个需要键的功能注册一个 `KeyBinding`（类名常量集中管理），默认键位写入功能表。
- 键位冲突提醒：初始化时遍历本模组注册键位，检测并存键位打 WARN 日志；提供 `/wly keys list` 指令查看。

### 2.4 Mixin 策略
- 优先 Fabric API 事件；事件无法覆盖时才加 mini mixin；所有 mixin 首行检查 `WlyModule.on(id)`。
- 每个 mixin 只服务一个模块；目标方法用 26.2 实际方法名（以本机 loom-cache 源码为准）。
- Mixin 全部列入 `world_loves_you.mixins.json`，`defaultRequire = 0`（应用失败只报错不阻止启动）。

### 2.5 项目结构（单 Gradle 工程）
```
F:\AI项目\world_loves_you\
├ build.gradle / settings.gradle / gradle.properties / gradlew.bat
├ src/main/java/com/worldlovesyou/{core, config, client, module/<feature>/*}
├ src/main/resources/{fabric.mod.json, world_loves_you.mixins.json, assets/lang/zh_cn.json, data/}
└ docs/superpowers/{specs, plans}/
```

## 3. 功能总表（36 项 → 模块映射）

> 图例：side：C=客户端 / S=服务端 / B=双端；风险：实现可行性风险；批次：实施先后。
> 锚点写法：事件=Fabric API 事件名，M=Mixin 目标（26.2 名）。

| # | 功能（灵感来源 class） | 模块 id | side | 核心实现锚点 | 关键配置 | 快捷键 | 风险 | 批次 |
|---|---|---|---|---|---|---|---|---|
| 1 | 连接纹理/玻璃板修复（Continuity/4906） | `ctm` | C | M: 方块模型烘焙/钠 render API（Sodium 26.2） | 总开关、连接纹理开、玻璃板修复开 | 无 | 高 | B5 |
| 2 | 更好的雪覆盖（15977） | `snow-cap` | C | E: `ModelLoadingPlugin` 包裹雪上方块 baked model 加雪帽 | 开关、适用方块列表 | 无 | 中 | B4 |
| 3 | 附魔大师（19853） | `anvil-master` | B | M: `AnvilMenu`（去过贵+修复费用曲线）；等级罗马数字显示；`/enchant-clear` 指令 | 上限、罗马显示、修复折扣、指令开关 | 无 | 中 | B3 |
| 4 | 更好的经验修补（3834） | `mend-hold` | S | E: `UseItemCallback`（Shift+右键装备修复，扣经验） | 开关、每次修复消耗点数 | Shift+右键 | 低 | B2 |
| 5 | 附魔等级上限突破2（16646） | `elb` | B | M: 附魔等级钳制点；`/cenchant` 指令；铁砧叠加 | 上限255~2^31-1、叠加开关 | 无 | 高 | B4 |
| 6 | 附魔台显示全部潜在附魔（13842） | `enchant-reveal` | C | M: `EnchantmentScreen` 渲染钩子 | 开关、显示条数上限 | 无 | 中 | B3 |
| 7 | 刷新附魔（4287） | `reroll` | C | M: `EnchantmentScreen` 加按钮重掷显示种子 | 经验成本、青金石成本 | 可配按钮键 | 低 | B3 |
| 8 | 同 #6（重复项），不重复实现 | — | — | — | — | — | — | — |
| 9 | 轻松附魔 1-4（10721） | `enchant-tweak` | S | M: 附魔冲突过滤/(无限)不耗箭/弓→弩/经验修补扣经验 | 4 个子开关 | 无 | 高 | B3 |
| 10 | 交易附魔显示 TED（15979） | `trade-display` | C | M: `MerchantScreen` 渲染交易附魔信息 | 开关、循环速度、头顶显示 | 默认 H=切换 | 中 | B3 |
| 11 | 智能提示框：自动换行+等比例缩小+滚轮纵移 | `smart-tooltip` | C | M: `GuiGraphics` tooltip 渲染 + `Screen.mouseScrolled` | 最大宽、最小缩放、滚轮开关 | 无 | 中 | B2 |
| 12 | 状态栏与血量显示 AsteorBar（13236） | `bars` | C | E: `HudRenderCallback` + M: 实体名牌渲染 | 布局、颜色、尺寸、目标生物 | 默认 F8/F10 | 高 | B4 |
| 13 | Easy Tweak 规则集（11211） | `eztweak` | S | M: 末影人/苦力怕/恶魂/耕地/烈焰人掉落/铁砧上限等（子项各配开关） | 每规则一开关 | 无 | 高 | B4 |
| 14 | 最大生命修复（8422） | `maxhealth-fix` | S | E: 登录/重算时恢复装备提供的生命上限 | 开关 | 无 | 低 | B1 |
| 15 | 全局游戏规则锁定 The Ruler（16647） | `ruler` | S | E: 世界加载时注入自定义规则 JSON | 开关、规则 JSON 路径 | 无 | 低 | B1 |
| 16 | 去实验性设置警告（3448） | `no-exp-warn` | C | M: 实验设置警告弹窗路径（26.2 待钻） | 开关 | 无 | 低 | B1 |
| 17 | 右键收获并补种（9157） | `harvest` | B | E: `UseBlockCallback`(成熟作物) + 掉落+种回+耗种子 | 需锄头/耗耐久/给经验/音效/作物白名单 | 无 | 中 | B1 |
| 18 | 显示村民全部交易（15597） | `visible-traders` | C | M: `MerchantScreen` 交易控件解锁显示 | 开关 | 无 | 低 | B3 |
| 19 | 满饱仍可吃+饱和度增益（16076 类） | `eat-when-full` | S | M: `Player` 进食判定/`FoodData` 上限 | 开关、允许溢出、饱和度上限 | 无 | 低 | B1 |
| 20 | 无耐久物品堆叠至 100 万（20569） | `big-stacks` | B | M: `ItemStack` 最大堆叠读取/新建栈；容器类使用-1 | 上限(默认1,000,000)、黑名单 | 无 | 高 | B5 |
| 21 | 伤害免疫机制移除 ReNDI（15443） | `rendi` | S | M: `LivingEntity` 受击无敌帧/玩家攻击充能阈值 | 无敌帧、充能取消/击退阈值 | 无 | 中 | B4 |
| 22 | 未加载区块活动模拟（15662） | `unloaded-activity` | S | M: `ServerChunkCache` 区块加载时按离线时长补算方块实体/作物 | 每方块类开关、模拟上限 | 无 | 高 | B5 |
| 23 | 更好的攀爬（7258） | `climbing` | B | M: `Player` 梯子/攀爬移动逻辑 | 速度倍率、加速时长、可跳 | 无 | 低 | B2 |
| 24 | 始终开放水域（8506） | `open-water` | S | M: `FishingHook` 开阔水域判定恒真 | 开关 | 无 | 低 | B1 |
| 25 | 友军保护 Friendly Fire（8277） | `friendly-fire` | S | M: 宠物/幼年生物受击过滤 | 宠物/幼年/潜行绕过 | 无 | 中 | B2 |
| 26 | 战利品光束（4690） | `loot-beams` | C | E: `WorldRenderEvents` 绘制掉落物光柱/名牌 | 颜色、白黑名单、名牌 | 无 | 中 | B4 |
| 27 | 喷溅牛奶（5402） | `splash-milk` | B | 新物品+新实体：命中清除范围内效果 | 开关、两种瓶 | 无 | 中 | B2 |
| 28 | 墓碑（1350） | `graves` | S | E: `AFTER_DEATH` 生成墓碑方块实体储存背包/经验；右键取回 | 开关、保留经验、墓碑样式 | 无 | 中 | B2 |
| 29 | 更好的信标放置（5348） | `beacon-fill` | S | E: `UseBlockCallback`(信标+矿物块) 填充基座/破坏联动 | 两个与页面同名开关 | 潜行+右键整组 | 低 | B1 |
| 30 | 弹道估计（13980） | `trajectory` | C | E: `WorldRenderEvents.LAST` 画轨迹；射线检测实体播"叮" | 线/立方体、颜色大小、每物品开关 | 无 | 中 | B4 |
| 31 | 所有箭配无限（16728） | `infinity-arrows` | S | M: 弓/弩取箭逻辑（任意箭） | 开关 | 无 | 低 | B1 |
| 32 | Clickthrough 2.0（15627） | `clickthrough` | B | M: 客户端拾取射线过滤告示牌/展示框 | 开关、过滤列表 | 无 | 中 | B5 |
| 33 | 平滑基岩层（3510） | `smooth-bedrock` | S | M: 区块生成基岩层平整 | 主世界/下界/末地开关 | 无 | 高 | B5 |
| 34 | 刷怪笼增强（3858） | `spawner-plus` | S | E: 精准采集掉落/右击刷怪蛋换型/击杀掉落 | 每子项开关 | 无 | 中 | B4 |
| 35 | 便携刷怪笼（16094） | `portable-spawner` | B | 新物品携带刷怪笼实体类型+放置 | 开关、拾取条件 | 无 | 中 | B4 |
| 36 | Powershot 蓄力投掷（8710） | `powershot` | B | 长按右键蓄力→自定义投掷实体/初速 | 开关、最大射程、速度 | 长按右键（可改） | 中 | B5 |

> 备注：20（100 万堆叠）涉及存档/网络包大容量，26.2 的 `ItemStack` 计数值为 int，1,000,000 在 int 范围内，但需核对同步/渲染性能，B5 先做 9999 试点再放开上限。
> 钠适配：所有渲染类模块（1/11/12/26/30）一律走 `VertexConsumer`/官方渲染钩子，不直改钠内部；B0 会对钠 26.2 做兼容性 spike。

## 4. 交付批次（每批可玩 jar）

- **B0 骨架**：工程+Gradle+Loom 依赖拉通、`WlyModule/ModuleManager/WlyConfig/WlyKeys`、Mod Menu 入口、Cloth Config 总开关页、`/wly` 列表指令、zh_cn 语言文件、mixin 管理骨架 → `gradlew build` 通过。
- **B1 低风险速胜（10 个）**：#14 #15 #17 #19 #24 #29 #31 #16 #25(预留) —— 具体见实施计划。
- **B2 常规交互（8 个）**：#4 #11 #23 #25 #27 #28 #13(第一批子规则) #18(预留)。
- **B3 附魔/交易 GUI 组（6 个）**：#3 #6 #7 #9 #10 #18。
- **B4 渲染与战斗组（8 个）**：#2 #5 #12 #21 #26 #30 #34 #35。
- **B5 高风险收尾（6 个）**：#1 #20 #22 #32 #33 #36。

## 5. 验收与测试

- 每批：`gradlew.bat build` 通过 + 资源校验脚本通过 + jar 可进测试实例（`E:\.minecraft\versions\26.2-Fabric 0.19.3\mods`）。
- 功能级：每模块在计划中给出 3-5 条游戏内人工测试步骤（含"关闭功能后行为消失"的验证）。
- 总装：与用户现役模组（含暗黑词条）共载冒烟测试；冲突 → 记录并调整锚点。
- 风险最高项（#1 CTM、#20 大堆叠、#22 离线模拟、#33 基岩世界生成、#32 点击穿透）每项单独 spike，spike 失败则该功能标记"实验性"并在配置中默认仍开但文档注明。

## 6. 明确的非目标（本次不做）

- 不打包钠/模组菜单/Cloth Config（依赖由用户安装）。
- 不复制原模组代码/资源；全部重新实现。
- 不做整合包、不做 Forge/NeoForge 版本。
- 不提供非中文语言文件（后续可加）。