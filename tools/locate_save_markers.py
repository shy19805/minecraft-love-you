# -*- coding: utf-8 -*-
"""定位存档中指定方块实体所在的区块坐标。

用法：
    python tools/locate_save_markers.py "<存档目录>" [关键词1,关键词2,...]
"""
import os
import struct
import sys
import zlib


def iter_chunks(path):
    """yield (chunk_x, chunk_z, payload)"""
    name = os.path.basename(path)
    parts = name[:-4].split(".")
    try:
        rx = int(parts[1])
        rz = int(parts[2])
    except Exception:
        return
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
            payload = None
            if comp == b"\x02":
                try:
                    payload = zlib.decompress(data)
                except Exception:
                    payload = None
            elif comp == b"\x01":
                try:
                    payload = zlib.decompressobj().decompress(data)
                except Exception:
                    payload = None
            if payload is None:
                continue
            yield rx * 32 + (i % 32), rz * 32 + (i // 32), payload


def main():
    root = sys.argv[1]
    keywords = sys.argv[2].split(",") if len(sys.argv) > 2 else [
        "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker",
        "minecraft:campfire",
    ]
    keys = [k.encode() for k in keywords]
    hits = {k: [] for k in keywords}
    for dirpath, _d, filenames in os.walk(root):
        if os.path.basename(dirpath) != "region":
            continue
        for name in filenames:
            if not name.endswith(".mca"):
                continue
            full = os.path.join(dirpath, name)
            for cx, cz, payload in iter_chunks(full):
                for k, raw in zip(keywords, keys):
                    if raw in payload:
                        hits[k].append((cx, cz))
    for k, v in hits.items():
        if not v:
            continue
        print("%s  (%d 个区块)" % (k, len(v)))
        for cx, cz in sorted(v)[:40]:
            print("   [%d, %d]" % (cx, cz))


if __name__ == "__main__":
    main()
