#!/bin/bash

source ./venv/bin/activate

endpoint="gochain"

getAddress () {
  package=$1
  cat ./config/deploy/${package}/${endpoint}/deploy.json | jq .scoreAddress -r
}

# Deploy pool tokens
./run.py -e ${endpoint} deploy iusdc
./run.py -e ${endpoint} deploy sicx

# Deploy factory
./run.py -e ${endpoint} deploy factory

# Deploy pool
# Write the tokens addresses to the pool params
iusdc_address=$(getAddress "iusdc")
sicx_address=$(getAddress "sicx")
echo '{}' | jq --arg token0 $iusdc_address --arg token1 $sicx_address '{_token0: $token0, _token1: $token1}' > ./config/deploy/iusdc-sicx-pool/${endpoint}/params.json
./run.py -e ${endpoint} deploy iusdc-sicx-pool

# Initialize Pool
pool=$(getAddress "iusdc-sicx-pool")
./run.py -e ${endpoint} invoke iusdc-sicx-pool initialize
echo '{}' | jq --arg tokenA $iusdc_address --arg tokenB $sicx_address --arg pool $pool '{method: "createPool", params: {tokenA: $tokenA, tokenB: $tokenB, fee: "0x1f4", pool: $pool}}' > ./config/calls/factory/${endpoint}/createPool.json
./run.py -e ${endpoint} invoke factory createPool

# Deploy LiquidityManagement
factory_address=$(cat ./config/deploy/factory/${endpoint}/deploy.json | jq .scoreAddress -r)
echo '{}' | jq --arg _factory $factory_address --arg _sICX $sicx_address '{_factory: $_factory, _sICX: $_sICX}' > ./config/deploy/liquidity-management/${endpoint}/params.json
./run.py -e ${endpoint} deploy liquidity-management
