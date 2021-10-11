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

def print_empty(*args):
    pass

def deploy(config: Config, target: str, version: str, params_path: str, build: str="optimized", verbose=print_empty):
    owner = config.owner
    tx_handler = config.tx_handler

    content = gen_deploy_data_content(f"./{target}/build/libs/{target}-{version}-{build}.jar")
    content_type = 'application/java'
    params = json.loads(open(f"{params_path}/{config.endpoint}/params.json", "r").read())
    tx_hash = tx_handler.install(owner, content, content_type, params)

    tx_result = tx_handler.ensure_tx_result(tx_hash, verbose != print_empty)
    open(f"{params_path}/{config.endpoint}/deploy.json", "w+").write(json.dumps(tx_result, indent=2))
    score_address = tx_result["scoreAddress"]
    verbose(f"{params_path}: {score_address}")
    return score_address
