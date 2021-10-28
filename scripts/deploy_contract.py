# Copyright 2021 ICONation
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

from scripts.meta import get_meta

def print_empty(*args):
    pass

def get_deploy(package, endpoint):
    deploy_path = f"./config/deploy/{package}/"
    deploy = json.loads(open(f"{deploy_path}/{endpoint}/deploy.json", "r").read())
    return deploy['scoreAddress']

def get_params(package, endpoint):
    deploy_path = f"./config/deploy/{package}/"
    params = json.loads(open(f"{deploy_path}/{endpoint}/params.json", "r").read())
    return params

def deploy(config: Config, package: str, verbose=print_empty):
    owner = config.owner
    tx_handler = config.tx_handler
    
    params = get_params(package, config.endpoint)
    javaPkg, version, build = get_meta(package, config.endpoint)

    content = gen_deploy_data_content(f"./{javaPkg}/build/libs/{javaPkg}-{version}-{build}.jar")
    content_type = 'application/java'
    tx_hash = tx_handler.install(owner, content, content_type, params)

    tx_result = tx_handler.ensure_tx_result(tx_hash, verbose != print_empty)
    deploy_path = f"./config/deploy/{package}/"
    open(f"{deploy_path}/{config.endpoint}/deploy.json", "w+").write(json.dumps(tx_result, indent=2))
    score_address = tx_result["scoreAddress"]
    verbose(f"{deploy_path}: {score_address}")
    return score_address

def update(config: Config, package: str, verbose=print_empty):
    owner = config.owner
    tx_handler = config.tx_handler

    javaPkg, version, build = get_meta(package, config.endpoint)
    address = get_deploy(package, config.endpoint)
    params = get_params(package, config.endpoint)

    content = gen_deploy_data_content(f"./{javaPkg}/build/libs/{javaPkg}-{version}-{build}.jar")
    content_type = 'application/java'
    tx_hash = tx_handler.update(owner, address, content, content_type, params)

    tx_result = tx_handler.ensure_tx_result(tx_hash, verbose != print_empty)
    deploy_path = f"./config/deploy/{package}/"
    open(f"{deploy_path}/{config.endpoint}/deploy.json", "w+").write(json.dumps(tx_result, indent=2))
    score_address = tx_result["scoreAddress"]
    verbose(f"{deploy_path}: {score_address}")
    return score_address