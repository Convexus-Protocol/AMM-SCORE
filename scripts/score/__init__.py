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


class Score:

    def __init__(self, tx_handler, address):
        self._address = address
        self._tx_handler = tx_handler

    def call(self, method, params=None):
        return self._tx_handler.call(self._address, method, params)

    def invoke(self, wallet, method, params=None, limit=None):
        return self._tx_handler.invoke(wallet, self._address, method, params, limit)

    @property
    def address(self):
        return self._address
