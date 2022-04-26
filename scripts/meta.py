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

import json

def get_meta(package, endpoint):
    deploy_path = f"./scripts/config/deploy/{package}/"
    meta = json.loads(open(f"{deploy_path}/build.json", "r").read())
    javaPkg=meta['javaPkg']
    version=meta['version'] if 'version' in meta else ''
    build=meta['build']
    return javaPkg, version, build
