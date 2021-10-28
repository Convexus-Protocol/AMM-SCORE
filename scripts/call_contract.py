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

from iconsdk.libs.in_memory_zip import gen_deploy_data_content

from scripts.config import Config
import json

from scripts.deploy_contract import get_deploy

def print_empty(*args):
    pass

def get_call(package, endpoint, filename):
    call = json.loads(open(f"./config/calls/{package}/{endpoint}/{filename}.json", "r").read())
    return call['method'], call['params']

def invoke(config: Config, package: str, paramsfilename: str, verbose=print_empty):
    owner = config.owner
    tx_handler = config.tx_handler

    address = get_deploy(package, config.endpoint)
    method, params = get_call(package, config.endpoint, paramsfilename)

    tx_hash = tx_handler.invoke(owner, address, method, params)
    return tx_handler.ensure_tx_result(tx_hash)