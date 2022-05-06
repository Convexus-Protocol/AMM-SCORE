#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

# if [ "$#" -ne "1" ] ; then
#   error "Usage: $0 <poolId>"
#   exit 1
# fi

# poolId=$1

# Start
./scripts/scenario/1.deploy_core_factory.sh sejong


./scripts/scenario/2.deploy_core_pool.sh sejong/bnusd-usdc
./scripts/scenario/2.deploy_core_pool.sh sejong/usdc-icx
./scripts/scenario/2.deploy_core_pool.sh sejong/usdc-sicx


./scripts/scenario/3.deploy_periphery.sh sejong


./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh sejong/bnusd-usdc 2000000000000000000000 2000000000 # 1 USDC = 1 BNUSD
# ./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh sejong/bnusd-usdc 1000000000000000000000 1000000000 # 1 USDC = 1 BNUSD

./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh sejong/usdc-icx 10000000000000000000000 6000000000 # 1 ICX = 0.60 USDC
./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh sejong/usdc-sicx 6000000000 10000000000000000000000 # 1 SICX = 0.60 USDC
