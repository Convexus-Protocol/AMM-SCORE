#!/bin/bash

if [ $# -eq 0 ]; then
  echo "Usage: $0 [endpoint]"
  exit 0
fi

source ./venv/bin/activate
endpoint=$1

. ./scripts/util/get_address.sh

# ------------------------------------------------------------------------
operator=$(cat ./config/keystores/${endpoint}/operator.icx | jq .address -r)

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

# Create the Pool from the Factory with fee 0.3%
fee="0xbb8" # 3000
echo '{}' | jq \
  --arg tokenA $iusdc_address \
  --arg tokenB $sicx_address \
  --arg fee $fee \
  '{method: "createPool", params: {tokenA: $tokenA, tokenB: $tokenB, fee: $fee}}' > ./config/calls/factory/${endpoint}/createPool.json
./run.py -e ${endpoint} invoke factory createPool

# Update the pool
# fee="0xbb8" # 3000
# echo '{}' | jq \
#   --arg pool $pool \
#   --arg tokenA $iusdc_address \
#   --arg tokenB $sicx_address \
#   --arg fee $fee \
#   '{method: "updatePool", params: {pool: $pool, tokenA: $tokenA, tokenB: $tokenB, fee: $fee}}' > ./config/calls/factory/${endpoint}/updatePool.json
# ./run.py -e ${endpoint} invoke factory updatePool

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

# Deploy NFTPositionDescriptor
./run.py -e ${endpoint} deploy nftpositiondescriptor

# Deploy NFTPositionManager
_tokenDescriptor_=$(getAddress "nftpositiondescriptor" ${endpoint})
echo '{}' | jq \
  --arg _factory $factory_address \
  --arg _tokenDescriptor_ $_tokenDescriptor_ \
  '{_factory: $_factory, _tokenDescriptor_: $_tokenDescriptor_}' > ./config/deploy/nftpositionmanager/${endpoint}/params.json
./run.py -e ${endpoint} deploy nftpositionmanager

# Deposit 10000 * 10**18 USDC & SICX to NFTPositionManager
nftpositionmanager=$(getAddress "nftpositionmanager" ${endpoint})
echo '{}' | jq \
  --arg _to $nftpositionmanager \
  '{method: "transfer", params: {_to: $_to, _value: "0x21e19e0c9bab2400000", _data: "0x7b226d6574686f64223a226465706f736974227d"}}' > ./config/calls/iusdc/${endpoint}/deposit_to_nftpositionmanager.json
./run.py -e ${endpoint} invoke iusdc deposit_to_nftpositionmanager

nftpositionmanager=$(getAddress "nftpositionmanager" ${endpoint})
echo '{}' | jq \
  --arg _to $nftpositionmanager \
  '{method: "transfer", params: {_to: $_to, _value: "0x21e19e0c9bab2400000", _data: "0x7b226d6574686f64223a226465706f736974227d"}}' > ./config/calls/sicx/${endpoint}/deposit_to_nftpositionmanager.json
./run.py -e ${endpoint} invoke sicx deposit_to_nftpositionmanager

# Check deposited amount
echo '{}' | jq \
  --arg user $operator \
  --arg token $iusdc_address \
  '{method: "deposited", params: {user: $user, token: $token}}' > ./config/calls/nftpositionmanager/${endpoint}/deposited.json
echo "USDC Deposited:"
./run.py -e ${endpoint} call nftpositionmanager deposited

echo '{}' | jq \
  --arg user $operator \
  --arg token $sicx_address \
  '{method: "deposited", params: {user: $user, token: $token}}' > ./config/calls/nftpositionmanager/${endpoint}/deposited.json
echo "SICX Deposited:"
./run.py -e ${endpoint} call nftpositionmanager deposited

# Mint a position
tickLower="-0xd89b4"
tickUpper="0xd89b4"
amount0Desired="0x21e19e0c9bab2400000"
amount1Desired="0x21e19e0c9bab2400000"
amount0Min="0"
amount1Min="0"
recipient=$operator
deadline="111111111111"
echo '{}' | jq \
  --arg token0 $iusdc_address \
  --arg token1 $sicx_address \
  --arg fee $fee \
  --arg tickLower $tickLower \
  --arg tickUpper $tickUpper \
  --arg amount0Desired $amount0Desired \
  --arg amount1Desired $amount1Desired \
  --arg amount0Min $amount0Min \
  --arg amount1Min $amount1Min \
  --arg recipient $recipient \
  --arg deadline $deadline \
  '{method: "mint", params: {params: {token0: $token0, token1: $token1, fee: $fee, tickLower: $tickLower, tickUpper: $tickUpper, amount0Desired: $amount0Desired, amount1Desired: $amount1Desired, amount0Min: $amount0Min, amount1Min: $amount1Min, recipient: $recipient, deadline: $deadline}}}' > ./config/calls/nftpositionmanager/${endpoint}/mint.json
./run.py -e ${endpoint} invoke nftpositionmanager mint

# Swap some tokens from the SwapRouter
swaprouter=$(getAddress "swaprouter" ${endpoint})
amountOutMinimum="0x0"
sqrtPriceLimitX96="0x0"
params=$(echo '{}' | jq \
  --arg tokenOut $sicx_address \
  --arg fee $fee \
  --arg recipient $operator \
  --arg deadline $deadline \
  --arg amountOutMinimum $amountOutMinimum \
  --arg sqrtPriceLimitX96 $sqrtPriceLimitX96 \
  '{method: "exactInputSingle", params: {tokenOut: $tokenOut, fee: $fee, recipient: $recipient, deadline: $deadline, amountOutMinimum: $amountOutMinimum, sqrtPriceLimitX96: $sqrtPriceLimitX96}}')
data=0x$(xxd -pu -c 1000000 <<< $params)

value="0x16345785d8a0000" # 0.1 * 10**18
echo '{}' | jq \
  --arg _to $swaprouter \
  --arg _data $data \
  --arg _value $value \
  '{method: "transfer", params: {_to: $_to, _value: $_value, _data: $_data}}' > ./config/calls/iusdc/${endpoint}/exactInputSingle.json
./run.py -e ${endpoint} invoke iusdc exactInputSingle

