# AGENTS.md — 我的世界爱你 项目规范

## 必读

- **动手改代码前，先通读 `docs/开发避坑手册.md`**——里面是本项目实际踩过的坑（类泄漏/配方格式/钩子改名/UV 网格/部署校验/按键注册/mixins 路径/@Shadow/点击签名/ordinal/编码/双版本兼容等）。凡与手册冲突的新写法，先在手册里核实再动手。
- **修完任何新坑，必须按手册末尾的格式追加一条**，不许只修不记。

## 双版本兼容（26.1.2 / 26.2）

一个 jar 同时跑 26.1.2 与 26.2；`fabric.mod.json` 声明 `minecraft: ">=26.1.2 <26.3"`、`java: ">=25"`。

- **构建基线默认 26.1.2（= 兼容下限）**，编译器强制只用两版都存在的 API：
  - `gradlew.bat build` → 26.1.2 基线（默认，发布产物）
  - `gradlew.bat build -Pmc=26.2` → 26.2 基线（等价产物，在 26.2 上开发/跑 runClient 时用）
- 两版**运行期**分叉点只有 2 处，必须走兼容写法：
  1. 取"当前屏幕"→ `com.minecraftloveyou.client.ClientCompat.currentScreen()`
     （26.2 是 `Gui.screen()` 方法；26.1.2 是 `Minecraft.screen` 公有字段）
  2. 注册方块实体类型 → `FabricBlockEntityTypeBuilder.create(...).build()`
     （26.1.2 的 `BlockEntityType(BlockEntitySupplier, Set<Block>)` 构造器是 **private**，26.2 才放开）
- 另有 1 处**编译期**分叉（不影响产物）：实体类型常量 26.1.2 挂 `EntityType.X`、26.2 挂 `EntityTypes.X`
  （两版类名不同 → 无法用同一符号编译）。发布代码**禁止**引用实体常量；自检等确需时用反射兜底（手册 #41）。
- 新增/改动 mixin 后跑 `python tools/mixin_audit.py`：用 javap 在两版 jar 上核对
  @Mixin 目标类、@Inject/@Redirect 的 method、@Accessor/@Invoker、@Shadow 字段。
- 两版都跑 Java 25 运行时，`java_version=25` 不要降。

## 工作流铁律

1. 改码 → `gradlew.bat build`（基线 26.1.2）→ 复制 jar 到**两个**测试实例 → `Get-FileHash` 比对源/目标 MD5 一致，才算部署。
2. `runServer` 绿 ≠ 完成：涉及玩家/界面的功能（mixin 注入、按键、GUI）需说明"待游戏内验证"并给验证清单。
3. 含中文的源码/JSON 一律用 UTF-8 工具链改（python io / write / edit），不用 pwsh Set-Content。
4. mixin 注入点先 `javap -c` 核对方法与调用序（ordinal），字段先确认声明层级（@Shadow 不跨类）。
5. 每个新功能交付前，按用户验收口径走一遍"触发路径是否真实可达"。
6. 改完任何 mixin，交付前必跑 `python tools/injection_audit.py --mc both`（`defaultRequire=0` 下注入失败是**静默**的）。
7. **`CHUNK_LOAD` / `CHUNK_UNLOAD` / `ENTITY_LOAD` 回调里禁止读写世界**（只准记录/入队）。
   在区块装载过程中写方块会触发邻居更新 → 重入 `ServerChunkCache.getChunk().join()` → **死锁**，
   症状是「永久卡在加载地形中」。世界操作一律搬到 `END_SERVER_TICK` 用 `getChunkNow` 取回区块后再做。
8. **注册到通用入口（`WorldLovesYouMod`）的模块类不得引用 `net.minecraft.client.*`**
   （专服启动会崩在 `Cannot load class ... in environment type SERVER`）。
   客户端逻辑拆到 `com.minecraftloveyou.client.*`，由 `WorldLovesYouClient` 注册。

## 测试实例

| 版本 | 路径 | mods |
|---|---|---|
| 26.2 | `E:\.minecraft\versions\26.2-Fabric 0.19.3\` | 151 个（整合包） |
| 26.1.2 | `E:\.minecraft\versions\26.1.2-Fabric 植物大战僵尸\` | 29 个 |

- 日志：`logs/latest.log`、崩溃：`crash-reports/`
- 用户上传的错误报告 zip 里有完整 latest.log（文件名 GBK 乱码属正常，解压后看 latest.log）。
- 部署文件名：`我的世界爱你-1.2.10.jar`（源：`build/libs/minecraft_love_you-1.2.10.jar`，MD5 `703fe88bdb2a9f9b1819721af6f44563`）。
  - **游戏运行中无法改名/删除 mods 里已加载的 jar**（Windows 文件占用，`mv` 报 `Device or resource busy`），
    但**可以覆盖写入**。所以「旧 jar 停用」这一步必须等玩家关掉游戏再做，否则会同时存在两个同 mod id 的 jar。
- 换版本号时**务必把旧 jar 改名停用**（如 `.jar.disabled`），否则两个同名 mod 会同时加载。
- **构建顺序陷阱**：跑过 `-Pmc=26.2` 之后，发布前必须再用**默认基线**构建一次
  （`gradlew.bat build`），否则 `build/libs/` 里留下的是 26.2 基线产物。
  改完 `gradle.properties` / 资源文件后，用 `gradlew.bat clean build` 更稳。

## 常用工具

- `tools/injection_audit.py`：**注入点可达性审计**（防静默失效）。核对每个 @Inject/@Redirect/@Accessor/@Shadow
  的方法是否声明、是否 abstract、`@At` 指向的成员是否真在字节码里、ordinal 是否越界。
  `python tools/injection_audit.py --mc both`
- `tools/mixin_audit.py`：mixin 目标跨版本（26.1.2 ↔ 26.2）存在性审计。
- `tools/check_features.py`：功能清单核对。
- `tools/scan_save_markers.py "<存档目录>" [关键词,...]`：扫描存档 region 文件，统计指定方块实体/方块
  出现在多少个区块（默认熔炉/高炉/烟熏炉/营火/箱子/木桶）。
- `tools/locate_save_markers.py "<存档目录>" [关键词,...]`：同上，但输出所在区块坐标
  —— 排查「离线追赶为什么没补到我的熔炉」这类问题时先跑它。
