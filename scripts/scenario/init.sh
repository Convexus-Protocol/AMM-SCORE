#!/bin/bash

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

source ./venv/bin/activate
endpoint=$1

. ./scripts/util/get_address.sh

# ------------------------------------------------------------------------

# Deploy pool tokens
./run.py -e ${endpoint} deploy iusdc
iusdc_address=$(getAddress "iusdc" ${endpoint})
./run.py -e ${endpoint} deploy sicx
sicx_address=$(getAddress "sicx" ${endpoint})

# Deploy factory
./run.py -e ${endpoint} deploy factory
factory_address=$(getAddress "factory" ${endpoint})

# Compile pool JAR
./run.py -e ${endpoint} optimizedJar iusdc-sicx-pool

# Set the contracts bytes
echo "\"0x$(xxd -p ./Convexus-Core/Contracts/Pool/build/libs/Pool-optimized.jar)\"" | tr -d '\n' > /tmp/pool.jar
echo '{}' | jq \
  --argfile contractBytes /tmp/pool.jar \
  '{method: "setPoolContract", params: {contractBytes: $contractBytes}}' > ./config/calls/factory/${endpoint}/setPoolContract.json
./run.py -e ${endpoint} invoke factory setPoolContract

# Create the Pool from the Factory
fee="0x1f4"
echo '{}' | jq \
  --arg tokenA $iusdc_address \
  --arg tokenB $sicx_address \
  --arg fee $fee \
  '{method: "createPool", params: {tokenA: $tokenA, tokenB: $tokenB, fee: $fee}}' > ./config/calls/factory/${endpoint}/createPool.json
./run.py -e ${endpoint} invoke factory createPool

# Get the Pool address
echo '{}' | jq \
  --arg token0 $iusdc_address \
  --arg token1 $sicx_address \
  --arg fee $fee \
  '{method: "getPool", params: {token0: $token0, token1: $token1, fee: $fee}}' > ./config/calls/factory/${endpoint}/getPool.json
pool=$(./run.py -e ${endpoint} call factory getPool)
echo "Pool deployed : ${pool}"
echo '{}' | jq \
  --arg scoreAddress $pool \
  '{scoreAddress: $scoreAddress}' > ./config/deploy/iusdc-sicx-pool/${endpoint}/deploy.json

# Initialize the Pool (1:1 price)
sqrtPriceX96="0x1000000000000000000000000"
echo '{}' | jq \
  --arg sqrtPriceX96 $sqrtPriceX96 \
  '{method: "initialize", params: {sqrtPriceX96: $sqrtPriceX96}}' > ./config/calls/iusdc-sicx-pool/${endpoint}/initialize.json
./run.py -e ${endpoint} invoke iusdc-sicx-pool initialize

# Deploy SwapRouter
echo '{}' | jq --arg _factory $factory_address '{_factory: $_factory}' > ./config/deploy/swaprouter/${endpoint}/params.json
./run.py -e ${endpoint} deploy swaprouter
