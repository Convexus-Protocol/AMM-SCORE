#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <poolId>"
  exit 1
fi

poolId=$1

# Start

# Read the pool config file
poolConfig=$(python ${configsDir}/${poolId}.py)

# Read the pool config variables
network=$(echo ${poolConfig} | jq -r .network)
token0=$(echo ${poolConfig} | jq -r .pool.token0)
token1=$(echo ${poolConfig} | jq -r .pool.token1)
amount0=$(echo ${poolConfig} | jq -r .pool.amount0)
amount1=$(echo ${poolConfig} | jq -r .pool.amount1)
fee=$(echo ${poolConfig} | jq -r .pool.fee)

# Factory Package information
pkg=$(getFactoryPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})

# Pool Package information
poolPkg=$(getPoolPkg ${poolId})
setupDeployDir ${poolPkg} ${network}
setupCallsDir ${poolPkg} ${network}
poolDeployDir=$(getDeployDir ${poolPkg} ${network})
poolCallsDir=$(getCallsDir ${poolPkg} ${network})

# Get the Pool address
actionName="getPool-${token0}-${token1}"
fee=$(hex ${fee})
filter=$(cat <<EOF
{
  method: "getPool",
  params: {
    token0: \$token0, 
    token1: \$token1, 
    fee: \$fee
  }
}
EOF
)

jq -n \
  --arg token0 $token0 \
  --arg token1 $token1 \
  --arg fee $fee \
  "${filter}" > ${callsDir}/${actionName}.json

pool=$(python run.py -e ${network} call ${pkg} ${actionName})

# Configure the deploy addresses
echo '{}' | jq \
  --arg scoreAddress $pool \
  '{scoreAddress: $scoreAddress}' \
  > ${poolDeployDir}/deploy.json


# Initialize the Pool (1:1 price)
info "Initialize the Pool ..."
actionName="initialize-${pool}"
sqrtPriceX96=$(python -c "import math; print(hex(int(math.sqrt(${amount1}/${amount0})*2**96)))")
price=$(python -c "print(${sqrtPriceX96}**2 / 2**192)")
info "Price token0:token1 = ${price}"

filter=$(cat <<EOF
{
  method: "initialize",
  params: {
    sqrtPriceX96: \$sqrtPriceX96
  }
}
EOF
)

jq -n \
  --arg sqrtPriceX96 $sqrtPriceX96 \
  "${filter}" > ${poolCallsDir}/${actionName}.json

python run.py -e ${network} invoke ${poolPkg} ${actionName}

success "Pool successfully initialized!"