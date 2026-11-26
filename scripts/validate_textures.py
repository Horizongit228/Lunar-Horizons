"""Validate PNG bytes and local model texture references before packaging."""
import json
import pathlib
import struct
import zlib

root = pathlib.Path("src/main/resources")
assets = root / "assets"
def validate_png(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n", "bad PNG signature"
    pos = 8
    chunks = []
    compressed = []
    header = None
    while pos < len(data):
        assert pos + 12 <= len(data), "truncated chunk"
        length, kind = struct.unpack_from(">I4s", data, pos)
        end = pos + 12 + length
        assert end <= len(data), "truncated chunk payload"
        payload = data[pos+8:end-4]
        assert zlib.crc32(kind + payload) == struct.unpack_from(">I", data, end-4)[0], "invalid CRC"
        chunks.append(kind)
        if kind == b"IHDR":
            assert len(chunks) == 1 and length == 13, "IHDR must be 13 bytes"
            header = struct.unpack(">IIBBBBB", payload)
        elif kind == b"IDAT":
            compressed.append(payload)
        elif kind == b"IEND":
            assert length == 0 and end == len(data), "invalid IEND"
        pos = end
    assert header and chunks[-1] == b"IEND" and compressed, "incomplete PNG"
    w,h,depth,color,compression,filter_method,interlace = header
    assert w>0 and h>0 and compression==0 and filter_method==0 and interlace in (0,1), "invalid PNG header"
    valid_depths = {0:(1,2,4,8,16),2:(8,16),3:(1,2,4,8),4:(8,16),6:(8,16)}
    assert color in valid_depths and depth in valid_depths[color], "invalid bit depth/color type"
    stream = zlib.decompressobj()
    raw = stream.decompress(b"".join(compressed)) + stream.flush()
    assert stream.eof and not stream.unused_data and not stream.unconsumed_tail, "invalid image data stream"
    if interlace == 0:
        channels = {0:1,2:3,3:1,4:2,6:4}[color]
        stride = 1 + (w*depth*channels+7)//8
        assert len(raw) == stride*h, "invalid scanline length"
        assert all(raw[i] <= 4 for i in range(0,len(raw),stride)), "invalid PNG filter"

errors = []
pngs = list(root.rglob("*.png"))
for path in pngs:
    try:
        validate_png(path)
    except Exception as exc:
        errors.append(f"{path}: {exc}")
for path in assets.rglob("models/**/*.json"):
    model = json.loads(path.read_text(encoding="utf-8"))
    for texture in model.get("textures", {}).values():
        if isinstance(texture, str) and texture.startswith("lunar_horizons:"):
            name = texture.split(":",1)[1]
            target = assets / "lunar_horizons" / "textures" / (name + ".png")
            if not target.is_file():
                errors.append(f"{path}: missing {target}")
if errors:
    raise SystemExit("\n".join(errors))
print(f"Validated {len(pngs)} PNG textures and local model texture references.")
