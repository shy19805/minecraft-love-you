#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""mixin 注入点可达性审计（防「静默失效」）。

背景：本项目 `minecraft_love_you.mixins.json` 设了 `injectors.defaultRequire = 0`。
这意味着**注入失败不会报错**：mixin 照常加载，但那个 @Inject/@Redirect 根本没插进去，
功能在游戏里完全不生效，日志里也看不到任何异常。

本脚本用 javap 静态核对每个注入点是否真的可达：

  A. 目标方法是否**声明在** @Mixin 指向的类里
     （mixin 不能注入继承来的方法；除非父类同时也是本 mixin 的 target —— 那种情况下
       子类不覆写、虚分派会走到父类被注入的实现，仍然生效）。
  B. `@At(value = "INVOKE"/"FIELD"/"NEW", target = "...")` 指向的成员
     是否真的出现在**目标方法的字节码**里（名字对但方法体里没有这个调用 = 死注入）。
  C. 标注 require 值：require=0（或缺省）→ 失败静默；require>=1 → 失败即崩（会被发现）。

用法：
    python tools/injection_audit.py                 # 默认 26.1.2 基线
    python tools/injection_audit.py --mc 26.2
    python tools/injection_audit.py --mc both
"""

import argparse
import os
import re
import subprocess
import sys

LOOM_CACHE = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf")
PROJECT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MIXIN_SRC = os.path.join(PROJECT, "src", "main", "java", "com", "minecraftloveyou", "mixin")
SRC_ROOT = os.path.join(PROJECT, "src", "main", "java")

IMPORT_RE = re.compile(r"^import\s+(?:static\s+)?([\w.$]+);", re.M)
ANNOTATION_KINDS = ("Inject", "Redirect", "WrapOperation", "ModifyVariable", "ModifyArg",
                    "ModifyConstant", "ModifyExpressionValue", "Accessor", "Invoker", "Overwrite")
NEEDS_METHOD = ("Inject", "Redirect", "WrapOperation", "ModifyVariable", "ModifyArg",
                "ModifyConstant", "ModifyExpressionValue")


# --------------------------------------------------------------------------- parsing

def balanced(text, start):
    """从 text[start] == '(' 起，返回匹配的括号内内容与结束下标。"""
    depth, i, n = 0, start, len(text)
    instr, esc = False, False
    while i < n:
        c = text[i]
        if instr:
            if esc:
                esc = False
            elif c == "\\":
                esc = True
            elif c == '"':
                instr = False
        else:
            if c == '"':
                instr = True
            elif c == "(":
                depth += 1
            elif c == ")":
                depth -= 1
                if depth == 0:
                    return text[start + 1:i], i
        i += 1
    return None, n


def find_annotations(text):
    """返回 [(kind, body)]，括号配平，能正确处理嵌套 @At(...)。"""
    out = []
    for m in re.finditer(r"@(" + "|".join(ANNOTATION_KINDS) + r")\s*\(", text):
        body, _ = balanced(text, m.end() - 1)
        if body is not None:
            out.append((m.group(1), body))
    return out


def parse_params(body):
    """把注解参数解析成 dict：'a = "x", b = @At(...)' -> {a:'x', b:'@At(...)'}。"""
    params, i, n = {}, 0, len(body)
    while i < n:
        m = re.match(r"\s*(\w+)\s*=\s*", body[i:])
        if not m:
            i += 1
            continue
        key = m.group(1)
        i += m.end()
        if body[i] == "(":                      # 裸数组 (a, b)
            val, j = balanced(body, i)
            params[key] = "(" + val + ")"
            i = j + 1
        elif body[i] == "{":                    # 数组 {"a","b"}
            j = body.index("}", i)
            params[key] = body[i:j + 1]
            i = j + 1
        elif body[i] == "@":                    # 嵌套注解 @At(...)
            am = re.match(r"@(\w+)\s*\(", body[i:])
            val, j = balanced(body, i + am.end() - 1)
            params[key] = "@" + am.group(1) + "(" + val + ")"
            i = j + 1
        elif body[i] == '"':                    # 字符串
            j = i + 1
            while body[j] != '"':
                j += 2 if body[j] == "\\" else 1
            params[key] = body[i + 1:j]
            i = j + 1
        else:                                   # 数字/常量
            m2 = re.match(r"[\w.$]+", body[i:])
            params[key] = m2.group(0)
            i += m2.end()
    return params


def str_list(value):
    if value is None:
        return []
    value = value.strip()
    if value.startswith("{"):
        return re.findall(r'"([^"]+)"', value)
    if value.startswith("("):
        return [v.strip().strip('"') for v in value[1:-1].split(",") if v.strip()]
    return [value]


def harvest_imports():
    imports = {}
    for root, _, files in os.walk(SRC_ROOT):
        for f in files:
            if f.endswith(".java"):
                with open(os.path.join(root, f), encoding="utf-8") as fh:
                    for m in IMPORT_RE.finditer(fh.read()):
                        imports[m.group(1).split(".")[-1]] = m.group(1)
    return imports


def parse_mixins(imports):
    """返回 [{file, targets, items:[{kind, params}]}]。"""
    out = []
    for root, _, files in os.walk(MIXIN_SRC):
        for fname in sorted(files):
            if not fname.endswith(".java"):
                continue
            with open(os.path.join(root, fname), encoding="utf-8") as fh:
                txt = fh.read()

            targets = []
            for m in re.finditer(r"@Mixin\s*\(", txt):
                body, _ = balanced(txt, m.end() - 1)
                targets += re.findall(r'targets\s*=\s*"([^"]+)"', body)
                for t in re.findall(r"([\w.$]+)\.class", body):
                    head, _, tail = t.strip().rpartition(".")
                    targets.append(imports.get(head, head) + "$" + tail
                                   if head and "." not in head
                                   else imports.get(t.strip(), t.strip()))

            items = []
            for kind, body in find_annotations(txt):
                if kind == "Overwrite":
                    items.append({"kind": kind, "params": {}})
                elif kind in ("Accessor", "Invoker"):
                    items.append({"kind": kind, "params": {"target": bare_target(body)}})
                else:
                    items.append({"kind": kind, "params": parse_params(body)})
            out.append({"file": fname, "targets": sorted(set(targets)),
                        "items": items, "shadows": shadow_fields(txt)})
    return out


# --------------------------------------------------------------------------- javap

class ClassInfo:
    def __init__(self, jar):
        self.jar = jar
        self._decl = {}
        self._code = {}

    def dump(self, cls):
        if cls not in self._decl:
            r = subprocess.run(["javap", "-p", "-classpath", self.jar, cls],
                               capture_output=True, text=True, encoding="utf-8", errors="replace")
            self._decl[cls] = None if r.returncode != 0 else r.stdout
        return self._decl[cls]

    def code(self, cls):
        if cls not in self._code:
            r = subprocess.run(["javap", "-p", "-c", "-classpath", self.jar, cls],
                               capture_output=True, text=True, encoding="utf-8", errors="replace")
            self._code[cls] = None if r.returncode != 0 else r.stdout
        return self._code[cls]

    def superclass(self, cls):
        d = self.dump(cls)
        if not d:
            return None
        m = re.search(r"\bextends\s+([\w.$]+)", d)
        return m.group(1) if m else None

    def chain(self, cls, limit=12):
        out, seen = [], set()
        cur = cls
        while cur and cur not in seen and len(out) < limit:
            seen.add(cur)
            out.append(cur)
            cur = self.superclass(cur)
        return out

    def declares(self, cls, name):
        d = self.dump(cls)
        return bool(d) and re.search(r"\b" + re.escape(name) + r"\s*\(", d) is not None

    def declares_field(self, cls, name):
        d = self.dump(cls)
        return bool(d) and re.search(r"\b" + re.escape(name) + r"\s*;", d) is not None

    def method_body(self, cls, name):
        """取目标方法在 javap -c 里的字节码文本（按方法名匹配，忽略描述符）。"""
        d = self.code(cls)
        if not d:
            return None
        lines = d.splitlines()
        heads = [i for i, l in enumerate(lines)
                 if re.match(r"^  \S.*\);\s*$", l) and re.search(
                     r"(?<![\w$])" + re.escape(name) + r"\s*\(", l)]
        if not heads:
            return None
        start = heads[0]
        end = len(lines)
        for i in range(start + 1, len(lines)):
            if re.match(r"^  \S.*\);\s*$", lines[i]) or lines[i].startswith("}"):
                end = i
                break
        return "\n".join(lines[start:end])


def norm_member(text):
    """把 mixin 的 `Lowner;name(desc)ret` 与 javap 的 `owner.name:(desc)ret` 归一化成同一种写法。"""
    text = text.strip()
    m = re.match(r"^L([\w/$]+);(.*)$", text)      # Lnet/minecraft/util/Mth;clamp(FFF)F
    if m:
        text = m.group(1) + "." + m.group(2)
    return re.sub(r"([\w$<>]+):\(", r"\1(", text)


_JAVA_NAME = re.compile(r"^[A-Za-z_$][\w$]*$")


def malformed_target(text):
    """镜像 `MemberInfo.parse` 的解析顺序，检测**描述符写法非法**的 @At target。

    Mixin 的解析顺序是「先找 `(`，再找 `:`」：
      有 `(`  → desc = 从 `(` 起，name = `(` 之前的整段
      无 `(`  → desc = `:` 之后，name = `:` 之前

    因此**方法**必须写括号形式 `name(args)ret`。写成冒号形式 `name:(args)ret` 时，
    `(` 先被命中 → name 变成 `name:`（冒号被粘进名字）→ 运行期
    `InvalidMemberDescriptorException: Invalid name: name:`，**加载期硬崩**（不是静默失效）。
    冒号形式只对**字段**（`name:desc`，无括号）有效。

    返回错误说明字符串，或 None（合法）。
    """
    if not text:
        return None
    ref = re.sub(r"\s", "", text)
    if "->" in ref:                       # tail 分隔符
        ref = ref.split("->", 1)[0]
    paren = ref.find("(")
    colon = ref.find(":")
    if paren > -1:
        ref = ref[:paren]
    elif colon > -1:
        ref = ref[:colon]
    else:
        return None                       # 无描述符，交给其它规则
    for q in ("*", "+"):                  # 量词后缀
        if ref.endswith(q):
            ref = ref[:-1]
            break
    else:
        if ref.endswith("}") and "{" in ref:
            ref = ref[:ref.find("{")]
    last_dot = ref.rfind(".")
    semi = ref.find(";")
    if last_dot > -1:
        name = ref[last_dot + 1:]
    elif semi > -1 and ref.startswith("L"):
        name = ref[semi + 1:]
    else:
        name = ref
    if name in ("<init>", "<clinit>"):
        return None
    if not _JAVA_NAME.match(name):
        return (f"@At target='{text}' 描述符写法非法：解析出的成员名是 '{name}'。"
                f"方法必须写括号形式 name(args)ret；冒号形式 name:(args)ret 只对字段有效 "
                f"→ 加载期硬崩 InvalidMemberDescriptorException")
    return None


def member_variants(at_target, owner_cls):
    """@At target 的候选写法：javap 对「调用自身类成员」会省略 owner（`Method text:(...)V`）。"""
    variants = {norm_member(at_target)}
    m = re.match(r"^L([\w/$]+);(.*)$", at_target.strip())
    if m and m.group(1).replace("/", ".") == owner_cls:
        variants.add(norm_member(m.group(2)))
    return variants


def bare_target(body):
    """@Accessor/@Invoker/@Shadow 的裸目标名：`"name"` 或 `name`。"""
    m = re.search(r'"([^"]+)"', body)
    if m:
        return m.group(1)
    return body.strip().split("(")[0].strip() or None


def shadow_fields(txt):
    """@Shadow 字段名（Mixin 找不到字段会直接崩，属于硬失败）。"""
    out = []
    for m in re.finditer(r"@Shadow(?:\([^)]*\))?\s*(?:\n\s*)*[\w<>\[\],.?\s]+?\s+(\w+)\s*;", txt):
        out.append(m.group(1))
    return out


def at_targets(params):
    """返回 [(value, target, ordinal)]。"""
    out = []
    for key, val in params.items():
        if key != "at" or not isinstance(val, str) or not val.startswith("@At"):
            continue
        inner = val[val.index("(") + 1:-1]
        p = parse_params(inner)
        out.append((p.get("value", "INVOKE"), p.get("target"), p.get("ordinal")))
    return out


# --------------------------------------------------------------------------- audit

def audit(jar, mixins, imports):
    ci = ClassInfo(jar)
    findings = []
    for mx in mixins:
        targets = mx["targets"]
        if not targets:
            continue

        # --- @Shadow 字段：Mixin 找不到字段会直接崩，必须声明在目标类自身 ---
        for tcls in targets:
            for fld in mx["shadows"]:
                if not ci.declares_field(tcls, fld):
                    findings.append((mx["file"], tcls, "Shadow", fld, "HIGH",
                                     f"@Shadow 字段 {fld} 未在 {tcls} 声明（@Shadow 不跨类）→ 加载即崩"))

        for item in mx["items"]:
            kind, params = item["kind"], item["params"]

            # --- @Accessor / @Invoker：找不到候选会抛 No candidates were found ---
            if kind in ("Accessor", "Invoker"):
                name = (params.get("target") or "").split("(")[0].strip()
                if not name:
                    continue
                for tcls in targets:
                    ok = ci.declares_field(tcls, name) or ci.declares(tcls, name)
                    if not ok:
                        findings.append((mx["file"], tcls, kind, name, "HIGH",
                                         f"@{kind}(\"{name}\") 在 {tcls} 找不到字段/方法 → 加载即崩"))
                    else:
                        findings.append((mx["file"], tcls, kind, name, "OK", "命中"))
                continue

            if kind == "Overwrite":
                continue
            methods = str_list(params.get("method"))
            if not methods:
                continue
            require = params.get("require")
            hard = require is not None and require != "0"
            ats = at_targets(params)

            for tcls in targets:
                for raw in methods:
                    mname = raw.split("(")[0].strip()
                    if mname == "*":
                        continue
                    # javap 用「类的简单名」打印构造器（FishingHook(...)），用 `static {};`
                    # 打印类初始化，两者都不含 <init>/<clinit> 字面量。不翻译过去的话
                    # declares 永远匹配不上 → 对构造器目标误报 HIGH（本机实测）。
                    if mname == "<init>":
                        lookup = tcls.rsplit(".", 1)[-1]
                    elif mname == "<clinit>":
                        d = ci.dump(tcls)
                        if d and "static {};" in d:
                            continue
                        lookup = mname          # 真没有，交给下面的 HIGH 分支
                    else:
                        lookup = mname
                    declared_here = ci.declares(tcls, lookup)
                    via_ancestor = None
                    if not declared_here:
                        for anc in ci.chain(tcls)[1:]:
                            if ci.declares(anc, lookup):
                                via_ancestor = anc
                                break
                    if not declared_here and via_ancestor is None:
                        findings.append((mx["file"], tcls, kind, raw, "HIGH",
                                         f"目标方法 {mname} 在 {tcls} 及其父类链上都不存在 → 注入失败（require={'>=1 会崩' if hard else '0 静默'}）"))
                        continue
                    if not declared_here and via_ancestor:
                        if via_ancestor in targets:
                            status = f"OK(继承自 {via_ancestor.split('.')[-1]}，该父类也是本 mixin 的 target)"
                        else:
                            findings.append((mx["file"], tcls, kind, raw, "HIGH",
                                             f"{mname} 未在 {tcls} 声明，实际实现在 {via_ancestor}，而它**不在本 mixin 的 target 列表** → 该类上注入失败"))
                            continue
                    else:
                        status = "OK"

                    owner_cls = tcls if declared_here else via_ancestor
                    body = ci.method_body(owner_cls, mname)
                    if body is not None and re.match(r"\s*abstract\b", body.splitlines()[0]):
                        findings.append((mx["file"], tcls, kind, raw, "HIGH",
                                         f"{owner_cls.split('.')[-1]}.{mname} 是 abstract 方法（无字节码）→ 注入必然不生效；真正实现应在子类上"))
                        continue

                    # 校验 @At 指向的成员是否真在字节码里
                    checked_at = 0
                    for value, at_target, ordinal in ats:
                        if not at_target or value not in ("INVOKE", "FIELD", "NEW", "CONSTANT"):
                            continue
                        checked_at += 1
                        bad = malformed_target(at_target)
                        if bad:
                            findings.append((mx["file"], tcls, kind, raw, "HIGH", bad))
                            continue
                        if body is None:
                            findings.append((mx["file"], tcls, kind, raw, "MED",
                                             f"无法读取 {owner_cls}.{mname} 的字节码，@At 未能校验"))
                            continue
                        needle_body = norm_member(body)
                        hits = [v for v in member_variants(at_target, owner_cls) if v in needle_body]
                        if not hits:
                            findings.append((mx["file"], tcls, kind, raw, "HIGH",
                                             f"@At({value}) target='{at_target}' 不在 {owner_cls.split('.')[-1]}.{mname} 字节码里 → 死注入（require={'>=1 会崩' if hard else '0 静默'}）"))
                            continue
                        if ordinal is not None and ordinal.isdigit():
                            cnt = needle_body.count(hits[0])
                            if int(ordinal) >= cnt:
                                findings.append((mx["file"], tcls, kind, raw, "HIGH",
                                                 f"@At ordinal={ordinal} 越界：{owner_cls.split('.')[-1]}.{mname} 里 '{at_target}' 只出现 {cnt} 次 → 死注入"))
                                continue
                        findings.append((mx["file"], tcls, kind, raw, "OK", f"@At 命中 {at_target}"))
                    if checked_at == 0:
                        findings.append((mx["file"], tcls, kind, raw, "OK", status))
    return findings


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--mc", default="26.1.2", help="26.1.2 / 26.2 / both")
    args = ap.parse_args()
    versions = ["26.1.2", "26.2"] if args.mc == "both" else [args.mc]

    imports = harvest_imports()
    mixins = parse_mixins(imports)

    for ver in versions:
        jar = os.path.join(LOOM_CACHE, ver, f"minecraft-merged-deobf-{ver}.jar")
        if not os.path.exists(jar):
            print(f"[跳过] 缺少 {ver} 的 loom 缓存 jar", file=sys.stderr)
            continue
        print(f"\n{'=' * 78}\nMinecraft {ver}\n{'=' * 78}")
        rows = audit(jar, mixins, imports)
        bad = [r for r in rows if r[4] == "HIGH"]
        med = [r for r in rows if r[4] == "MED"]
        print(f"注入点检查 {len(rows)} 项：HIGH {len(bad)} / MED {len(med)} / OK {len(rows) - len(bad) - len(med)}")
        for f, t, k, m, sev, why in rows:
            if sev == "OK":
                continue
            print(f"\n  [{sev}] {f}\n      @{k}(method=\"{m}\") on {t}\n      {why}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
