#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""mixin 跨版本目标审计（26.1.2 / 26.2）。

用途：本项目一份源码要同时跑在 26.1.2 与 26.2 上。mixin 的注入点/影子字段是**字符串或
字节码**，编译期不校验；某一版改名或搬家后，最坏情况是启动崩（@Shadow 找不到字段），
最常见是**静默失效**（注入点不命中，defaultRequire=0 不报错）。

本脚本把每个 mixin 的 @Mixin 目标、@Inject/@Redirect/@WrapOperation/@ModifyVariable 的
method、@Accessor/@Invoker、@Shadow 字段，逐个用 javap 在**两个版本的 jar** 上核对，输出：

  * 目标类缺失        → 另一版启动必崩
  * 成员只在基线版有  → 另一版静默失效（回归信号）
  * 两版都没有        → 继承自父类/祖父类，属正常（mixin 本就不该挂在这一层）

用法：
    python tools/mixin_audit.py                 # 默认比对 26.1.2 <-> 26.2
    python tools/mixin_audit.py --base 26.2 --other 26.1.2

前置：两个版本的 loom 缓存 jar 都已存在（各跑过一次对应基线的构建即可）：
    ./gradlew build                 # 默认 mc=26.1.2
    ./gradlew build -Pmc=26.2
"""

import argparse
import os
import re
import subprocess
import sys

LOOM_CACHE = os.path.expanduser("~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf")
PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIXIN_SRC = os.path.join(PROJECT, "src", "main", "java", "com", "minecraftloveyou", "mixin")
SRC_ROOT = os.path.join(PROJECT, "src", "main", "java")

IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([\w.$]+);", re.M)


def jar_path(version):
    return os.path.join(LOOM_CACHE, version, f"minecraft-merged-deobf-{version}.jar")


def harvest_imports():
    """simple name -> FQCN，用于把 `Enchantment.class` 这种短名还原成全限定名。"""
    imports = {}
    for root, _, files in os.walk(SRC_ROOT):
        for f in files:
            if not f.endswith(".java"):
                continue
            with open(os.path.join(root, f), encoding="utf-8") as fh:
                for m in IMPORT_RE.finditer(fh.read()):
                    fq = m.group(1)
                    imports[fq.split(".")[-1]] = fq
    return imports


def fqcn(simple, imports):
    return imports.get(simple, simple)


def parse_mixins(imports):
    """返回 [(文件名, 目标类FQCN, {成员名})]。"""
    out = []
    for root, _, files in os.walk(MIXIN_SRC):
        for fname in sorted(files):
            if not fname.endswith(".java"):
                continue
            path = os.path.join(root, fname)
            with open(path, encoding="utf-8") as fh:
                txt = fh.read()

            targets = []
            for m in re.finditer(r"@Mixin\((.*?)\)", txt, re.S):
                body = m.group(1)
                targets += re.findall(r'targets\s*=\s*"([^"]+)"', body)
                for t in re.findall(r"([\w.$]+)\.class", body):
                    head, _, tail = t.strip().rpartition(".")
                    # 形如 Outer.Inner.class（head 不含点）才按内部类还原
                    targets.append(fqcn(head, imports) + "$" + tail if head and "." not in head
                                   else fqcn(t.strip(), imports))

            members = set()
            for ann in ("Inject", "Redirect", "WrapOperation", "ModifyVariable"):
                for m in re.finditer(r"@" + ann + r"\((.*?)\)", txt, re.S):
                    members.update(re.findall(r'method\s*=\s*"([^"]+)"', m.group(1)))
                    for grp in re.findall(r'method\s*=\s*\{([^}]*)\}', m.group(1)):
                        members.update(re.findall(r'"([^"]+)"', grp))
            members.update(re.findall(r'@(?:Accessor|Invoker)\("([^"]+)"\)', txt))
            for m in re.finditer(r"@Shadow(?:\([^)]*\))?\s*(?:\n\s*)*[\w<>\[\],.?\s]+\s+(\w+)\s*;", txt):
                members.add("FIELD:" + m.group(1))

            out.append((fname, targets, sorted(members)))
    return out


_JAVAP_CACHE = {}


def javap(jar, cls):
    key = (jar, cls)
    if key not in _JAVAP_CACHE:
        r = subprocess.run(["javap", "-p", "-classpath", jar, cls],
                           capture_output=True, text=True, encoding="utf-8", errors="replace")
        _JAVAP_CACHE[key] = None if r.returncode != 0 else r.stdout
    return _JAVAP_CACHE[key]


def declared(dump, member):
    if dump is None:
        return None
    name = member[6:] if member.startswith("FIELD:") else member.split("(")[0]
    return re.search(r"\b" + re.escape(name) + r"\s*[;(]", dump) is not None


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--base", default="26.1.2", help="基线版本（默认 26.1.2）")
    ap.add_argument("--other", default="26.2", help="对照版本（默认 26.2）")
    args = ap.parse_args()

    jb, jo = jar_path(args.base), jar_path(args.other)
    for j, v in ((jb, args.base), (jo, args.other)):
        if not os.path.exists(j):
            print(f"[错误] 缺少 {v} 的 loom 缓存 jar：{j}\n"
                  f"       先跑一次对应基线的构建：./gradlew build" +
                  (f" -Pmc={v}" if v != "26.1.2" else ""), file=sys.stderr)
            return 2

    imports = harvest_imports()
    hard, soft, checked = [], [], 0
    for fname, targets, members in parse_mixins(imports):
        for t in targets:
            checked += 1
            db, do = javap(jb, t), javap(jo, t)
            if db is None or do is None:
                missing_in = args.base if db is None else args.other
                hard.append((fname, t, f"目标类在 {missing_in} 不存在"))
                continue
            for mem in members:
                if mem == "*" or mem.startswith("lambda$"):
                    continue  # "*" 是通配；lambda 名随编译产物变化，单独人工核对
                in_base, in_other = declared(db, mem), declared(do, mem)
                if in_base is True and in_other is False:
                    soft.append((fname, t, mem, f"只在 {args.base} 声明 → {args.other} 会静默失效"))
                elif in_base is False and in_other is True:
                    soft.append((fname, t, mem, f"只在 {args.other} 声明 → {args.base} 会静默失效"))

    print(f"基线 {args.base}  <->  对照 {args.other}")
    print(f"检查 mixin 目标 {checked} 个")
    if hard:
        print(f"\n[致命] {len(hard)} 项：")
        for f, t, why in hard:
            print(f"  {f:<38} {t}\n      {why}")
    if soft:
        print(f"\n[警告] {len(soft)} 项跨版本不一致：")
        for f, t, mem, why in soft:
            print(f"  {f:<38} {t}\n      {mem} — {why}")
    if not hard and not soft:
        print("\n[通过] 所有 mixin 目标/成员在两版声明一致。")
    return 1 if (hard or soft) else 0


if __name__ == "__main__":
    sys.exit(main())
