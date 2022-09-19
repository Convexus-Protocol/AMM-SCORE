#!/bin/bash

set -e


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
./scripts/scenario/1.deploy_core_factory.sh berlin


./scripts/scenario/2.deploy_core_pool.sh berlin/bnusd-usdc
./scripts/scenario/2.deploy_core_pool.sh berlin/usdc-icx
./scripts/scenario/2.deploy_core_pool.sh berlin/usdc-sicx


./scripts/scenario/3.deploy_periphery.sh berlin


./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh berlin/bnusd-usdc 2000000000000000000000 2000000000000000000000 # 1 USDC = 1 BNUSD
# ./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh berlin/bnusd-usdc 1000000000000000000000 1000000000000000000000 # 1 USDC = 1 BNUSD

./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh berlin/usdc-icx 10000000000000000000000 6000000000000000000000 # 1 ICX = 0.60 USDC
./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh berlin/usdc-sicx 6000000000000000000000 10000000000000000000000 # 1 SICX = 0.60 USDC
