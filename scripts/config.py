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

from iconsdk.wallet.wallet import KeyWallet

from scripts.util import get_icon_service, load_keystore, in_loop
from scripts.util.txhandler import TxHandler


class Singleton(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            cls._instances[cls] = super(Singleton, cls).__call__(*args, **kwargs)
        return cls._instances[cls]


class Config(metaclass=Singleton):

    def __init__(self, endpoint: str, keystore_path: str, passwd=None) -> None:
        icon_service, nid = get_icon_service(endpoint)
        self.tx_handler = TxHandler(icon_service, nid)
        self.endpoint = endpoint
        self.owner = load_keystore(keystore_path, passwd)

        # generate some test accounts
        if endpoint in ('local', 'gochain'):
            self.accounts = []
            for i in range(2):
                wallet = KeyWallet.create()
                self.tx_handler.transfer(self.owner, wallet.get_address(), in_loop(1))
                self.accounts.append(wallet)
