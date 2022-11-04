#!/bin/bash

./scripts/scenario/1.deploy_core_factory.sh lisbon

./scripts/scenario/2.deploy_core_pool.sh lisbon/bnusd-icx &
./scripts/scenario/2.deploy_core_pool.sh lisbon/bnusd-usdc &
./scripts/scenario/2.deploy_core_pool.sh lisbon/usdc-icx &
./scripts/scenario/2.deploy_core_pool.sh lisbon/usdc-sicx &
./scripts/scenario/2.deploy_core_pool.sh lisbon/usdt-icx &
./scripts/scenario/2.deploy_core_pool.sh lisbon/weth-crv &
./scripts/scenario/2.deploy_core_pool.sh lisbon/weth-sicx &

./scripts/scenario/3.deploy_periphery.sh lisbon