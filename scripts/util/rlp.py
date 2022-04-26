# Copyright 2021 ICON Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from typing import List, Union, Tuple


def rlp_decode(bs: bytes) -> Union[list, bytes]:
    """
    Decode one RLPObject.
    If it has more or invalid, raises an exception.

    :param bs: bytes of RLPObject
    :return: decoded bytes or list of RLPObject
    """
    if not isinstance(bs, bytes):
        raise Exception("bytes are expected")
    content, remains = decode_one(bs)
    if len(remains) > 0:
        raise Exception("TrailingBytes")
    return content


def decode_one(bs: bytes) -> Tuple[Union[bytes, list], bytes]:
    """
    Decode one RLPObject, then return remaining data.

    :param bs: bytes of RLPObjects
    :return: a list of RLPObject or decoded bytes along with remaining bytes
    """
    if len(bs) < 1:
        raise Exception("Not enough bytes")

    b = bs[0]
    if b < 0x80:
        return bytes([b]), bs[1:]
    elif b < 0xb8:
        size = b - 0x80
        if len(bs) < size + 1:
            raise Exception('Not enough bytes')
        return bs[1:1 + size], bs[1 + size:]
    elif b < 0xc0:
        ts = b - 0xb7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)
        return bs[ts:ts + size], bs[ts + size:]
    elif b < 0xf8:
        ts = 1
        size = b - 0xc0
    else:
        ts = b - 0xf7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)

    if len(bs) < ts + size:
        raise Exception("Not enough bytes for list")

    l: List[bytes] = []
    c = bs[ts:ts + size]
    while len(c) > 0:
        _, item_ts, item_size = parse_header(c)
        l.append(c[:item_ts + item_size])
        c = c[item_ts + item_size:]

    return l, bs[ts + size:]


def parse_header(bs: bytes) -> Tuple[bool, int, int]:
    b = bs[0]
    if b < 0x80:
        return False, 0, 1
    elif b < 0xb8:
        return False, 1, b - 0x80
    elif b < 0xc0:
        ts = b - 0xb7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)
        return False, ts, size
    elif b < 0xf8:
        return True, 1, b - 0xc0
    else:
        ts = b - 0xf7 + 1
        size = int.from_bytes(bs[1:ts], byteorder="big", signed=False)
        return True, ts, size
