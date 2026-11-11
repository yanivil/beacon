"""Rewrites an 8-bit RGB PNG as an 8-bit RGBA PNG with every pixel opaque.

Google Play asks for the app icon as a 32-bit PNG (with an alpha channel).
Chrome writes a fully opaque page as 24-bit RGB, so render.sh runs this on
the icon. Only the Python standard library is used. The picture doesn't
change: every pixel gets alpha 255.

Usage: python3 -I png_add_alpha.py <in.png> <out.png>
"""

import struct
import sys
import zlib

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def read_chunks(data):
    """Splits a PNG file into its chunks.

    @param data: the whole file's bytes.
    @return: a list of (type, payload) pairs, in file order.
    @throws ValueError: if the signature is wrong.
    """
    if not data.startswith(PNG_SIGNATURE):
        raise ValueError("not a PNG file")
    chunks, pos = [], len(PNG_SIGNATURE)
    while pos < len(data):
        (length,) = struct.unpack(">I", data[pos:pos + 4])
        kind = data[pos + 4:pos + 8]
        chunks.append((kind, data[pos + 8:pos + 8 + length]))
        pos += 12 + length  # length, type, payload, CRC
    return chunks


def paeth(a, b, c):
    """The PNG Paeth predictor for one byte.

    @param a: the byte to the left; @param b: the byte above; @param c: above-left.
    @return: whichever of a, b and c is nearest to a + b - c.
    """
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def unfilter(raw, width, height, bpp):
    """Undoes PNG's per-row filters.

    @param raw: the decompressed image data, a filter byte before each row.
    @param width: pixels per row; @param height: rows; @param bpp: bytes per pixel.
    @return: the rows' plain bytes.
    @throws ValueError: on an unknown filter type.
    """
    stride, rows, prev, pos = width * bpp, [], bytearray(width * bpp), 0
    for _ in range(height):
        ftype, row = raw[pos], bytearray(raw[pos + 1:pos + 1 + stride])
        pos += 1 + stride
        for i in range(stride):
            left = row[i - bpp] if i >= bpp else 0
            up, up_left = prev[i], (prev[i - bpp] if i >= bpp else 0)
            if ftype == 1:
                row[i] = (row[i] + left) & 0xFF
            elif ftype == 2:
                row[i] = (row[i] + up) & 0xFF
            elif ftype == 3:
                row[i] = (row[i] + (left + up) // 2) & 0xFF
            elif ftype == 4:
                row[i] = (row[i] + paeth(left, up, up_left)) & 0xFF
            elif ftype != 0:
                raise ValueError("unknown PNG filter %d" % ftype)
        rows.append(bytes(row))
        prev = row
    return rows


def chunk(kind, payload):
    """Builds one PNG chunk with its length and CRC.

    @param kind: the 4-byte chunk type; @param payload: its data.
    @return: the chunk's bytes.
    """
    crc = zlib.crc32(kind + payload) & 0xFFFFFFFF
    return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", crc)


def add_alpha(src, dst):
    """Writes src (8-bit RGB, not interlaced) to dst as 8-bit RGBA, fully opaque.

    @param src: the input path; @param dst: the output path.
    @throws ValueError: if src isn't an 8-bit, non-interlaced RGB PNG.
    """
    with open(src, "rb") as f:
        chunks = read_chunks(f.read())
    ihdr = next(p for k, p in chunks if k == b"IHDR")
    width, height, depth, colour, _, _, interlace = struct.unpack(">IIBBBBB", ihdr)
    if depth != 8 or colour != 2 or interlace != 0:
        raise ValueError("expected an 8-bit, non-interlaced RGB PNG")
    raw = zlib.decompress(b"".join(p for k, p in chunks if k == b"IDAT"))
    out = bytearray()
    for row in unfilter(raw, width, height, 3):
        out.append(0)  # filter type 0 for every output row
        for i in range(0, len(row), 3):
            out += row[i:i + 3] + b"\xff"
    with open(dst, "wb") as f:
        f.write(PNG_SIGNATURE)
        f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)))
        f.write(chunk(b"IDAT", zlib.compress(bytes(out), 9)))
        f.write(chunk(b"IEND", b""))


if __name__ == "__main__":
    if len(sys.argv) != 3:
        sys.exit("usage: python3 -I png_add_alpha.py <in.png> <out.png>")
    add_alpha(sys.argv[1], sys.argv[2])
