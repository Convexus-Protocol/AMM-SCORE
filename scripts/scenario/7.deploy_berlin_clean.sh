#!/bin/bash

./scripts/scenario/1.deploy_core_factory.sh berlin

./scripts/scenario/2.deploy_core_pool.sh berlin/bnusd-icx &
./scripts/scenario/2.deploy_core_pool.sh berlin/bnusd-usdc &
./scripts/scenario/2.deploy_core_pool.sh berlin/usdc-icx &
./scripts/scenario/2.deploy_core_pool.sh berlin/usdc-sicx &
./scripts/scenario/2.deploy_core_pool.sh berlin/usdt-icx &
./scripts/scenario/2.deploy_core_pool.sh berlin/weth-crv &
./scripts/scenario/2.deploy_core_pool.sh berlin/weth-sicx &

./scripts/scenario/3.deploy_periphery.sh berlin