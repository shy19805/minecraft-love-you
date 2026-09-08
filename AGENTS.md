# AGENTS.md — 我的世界爱你 项目规范

## 必读

- **动手改代码前，先通读 `docs/开发避坑手册.md`**——里面是本项目在 26.2 上实际踩过的坑（类泄漏/配方格式/钩子改名/UV 网格/部署校验/按键注册/mixins 路径/@Shadow/点击签名/ordinal/编码等）。凡与手册冲突的新写法，先在手册里核实再动手。
- **修完任何新坑，必须按手册末尾的格式追加一条**，不许只修不记。

## 工作流铁律

1. 改码 → `gradlew build` → 复制 jar 到 `E:\.minecraft\versions\26.2-Fabric 0.19.5-测试\mods\我的世界爱你-0.1.0.jar` → `Get-FileHash` 比对源/目标 MD5 一致，才算部署。
2. `runServer` 绿 ≠ 完成：涉及玩家/界面的功能（mixin 注入、按键、GUI）需说明"待游戏内验证"并给验证清单。
3. 含中文的源码/JSON 一律用 UTF-8 工具链改（python io / write / edit），不用 pwsh Set-Content。
4. mixin 注入点先 `javap -c` 核对方法与调用序（ordinal），字段先确认声明层级（@Shadow 不跨类）。
5. 每个新功能交付前，按用户验收口径走一遍"触发路径是否真实可达"。

## 测试实例

- 路径：`E:\.minecraft\versions\26.2-Fabric 0.19.5-测试\`
- 日志：`logs/latest.log`、崩溃：`crash-reports/`
- 用户上传的错误报告 zip 里有完整 latest.log（文件名 GBK 乱码属正常，解压后看 latest.log）。
