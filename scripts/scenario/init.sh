#!/bin/bash

source ./venv/bin/activate

endpoint="gochain"

getAddress () {
  package=$1
  cat ./config/deploy/${package}/${endpoint}/deploy.json | jq .scoreAddress -r
}

# Deploy pool tokens
./run.py -e ${endpoint} deploy iusdc
iusdc_address=$(getAddress "iusdc")
./run.py -e ${endpoint} deploy sicx
sicx_address=$(getAddress "sicx")

# Deploy factory
./run.py -e ${endpoint} deploy factory
factory_address=$(getAddress "factory")

# Deploy pool
# Write the tokens addresses to the pool params
echo '{}' | jq \
  --arg _token0 $iusdc_address \
  --arg _token1 $sicx_address \
  --arg _factory $factory_address \
  --arg fee 500 \
  --arg tickSpacing 10 \
  '{_token0: $_token0, _token1: $_token1, _factory: $_factory, fee: $fee, tickSpacing: $tickSpacing}' > ./config/deploy/iusdc-sicx-pool/${endpoint}/params.json
./run.py -e ${endpoint} deploy iusdc-sicx-pool

# Initialize Pool
pool=$(getAddress "iusdc-sicx-pool")
./run.py -e ${endpoint} invoke iusdc-sicx-pool initialize
echo '{}' | jq --arg tokenA $iusdc_address --arg tokenB $sicx_address --arg pool $pool '{method: "createPool", params: {tokenA: $tokenA, tokenB: $tokenB, fee: "0x1f4", pool: $pool}}' > ./config/calls/factory/${endpoint}/createPool.json
./run.py -e ${endpoint} invoke factory createPool

# Deploy LiquidityManagement
echo '{}' | jq --arg _factory $factory_address --arg _sICX $sicx_address '{_factory: $_factory, _sICX: $_sICX}' > ./config/deploy/liquidity-management/${endpoint}/params.json
./run.py -e ${endpoint} deploy liquidity-management
