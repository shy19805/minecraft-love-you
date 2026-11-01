# -*- coding: utf-8 -*-
"""扫描 Minecraft 存档 region 文件，统计区块里出现的方块实体 / 方块 id（用于排查离线追赶）。

用法：
    python tools/scan_save_markers.py "<存档目录>" [关键词1,关键词2,...]
"""
import io
import os
import struct
import sys
import zlib
from collections import Counter


def iter_chunk_payloads(path):
    with open(path, "rb") as fh:
        header = fh.read(4096)
        if len(header) < 4096:
            return
        for i in range(1024):
            off = struct.unpack(">I", b"\x00" + header[i * 4:i * 4 + 3])[0]
            if off == 0:
                continue
            fh.seek(off << 12)
            length = struct.unpack(">I", fh.read(4))[0]
            if length <= 0 or length > 1 << 26:
                continue
            comp = fh.read(1)
            data = fh.read(length - 1)
            if comp == b"\x02":
                try:
                    yield zlib.decompress(data)
                except Exception:
                    continue
            elif comp == b"\x01":
                try:
                    yield zlib.decompressobj().decompress(data)
                except Exception:
                    continue


def main():
    root = sys.argv[1]
    keywords = sys.argv[2].split(",") if len(sys.argv) > 2 else [
        "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
        "minecraft:campfire", "minecraft:chest", "minecraft:barrel",
    ]
    keys = [k.encode() for k in keywords]
    counter = Counter()
    chunks = 0
    for dirpath, _dirnames, filenames in os.walk(root):
        if os.path.basename(dirpath) != "region":
            continue
        for name in filenames:
            if not name.endswith(".mca"):
                continue
            full = os.path.join(dirpath, name)
            for payload in iter_chunk_payloads(full):
                chunks += 1
                for k, raw in zip(keywords, keys):
                    if raw in payload:
                        counter[k] += 1
    print("扫描区块数：", chunks)
    for k, v in counter.most_common():
        print("  %-40s %d" % (k, v))


if __name__ == "__main__":
    main()
