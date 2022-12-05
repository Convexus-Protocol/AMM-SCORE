#!/bin/bash

set -e

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh ${network}/bnusd-usdc 2000000000000000000000 2000000000000000000000 # 1 USDC = 1 BNUSD
./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh ${network}/usdc-icx 1000000000000000000000 600000000 # 1 ICX = 0.60 USDC
./scripts/scenario/setup/4.1.1.deposit_to_position_manager.sh ${network}/usdc-sicx 6000000000000000000000 10000000000000000000000 # 1 SICX = 0.60 USDC
