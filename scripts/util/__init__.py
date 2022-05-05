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

import getpass
import json
import sys

from iconsdk.exception import KeyStoreException
from iconsdk.icon_service import IconService
from iconsdk.providers.http_provider import HTTPProvider
from iconsdk.wallet.wallet import KeyWallet


def die(message):
    print(message)
    sys.exit(-1)


def in_icx(value):
    return value / 10**18


def in_loop(value):
    return value * 10**18


def print_response(header, msg):
    print(f'{header}: {json.dumps(msg, indent=4)}')


def get_icon_service(endpoint):
    endpoint_map = {
        "mainnet":   ['https://ctz.solidwallet.io', 0x1],
        "lisbon":    ['https://lisbon.net.solidwallet.io', 0x2],
        "sejong":    ['https://sejong.net.solidwallet.io', 0x53],
        "berlin":    ['https://berlin.net.solidwallet.io', 0x7],
        "localhost": ['http://localhost:9082', 0x3],
        "custom":    ['https://endpoint.convexus.netlib.re', 0x3],
    }
    if not endpoint in endpoint_map:
        raise Exception(f"Invalid endpoint '{endpoint}'")
    url, nid = endpoint_map.get(endpoint, [endpoint, 0x3])
    return IconService(HTTPProvider(url, 3)), nid


def get_address_from_keystore(keystore):
    path = keystore.name
    with open(path, encoding='utf-8-sig') as f:
        keyfile: dict = json.load(f)
        return keyfile.get('address')


def load_keystore(keystore_path, passwd=None):
    try:
        if passwd is None:
            print("Keystore:", keystore_path)
            passwd = getpass.getpass()
        return KeyWallet.load(keystore_path, passwd)
    except KeyStoreException as e:
        die(e.message)
