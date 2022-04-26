#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh

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
fee=$(echo ${poolConfig} | jq -r .pool.fee)

# Factory Package information
pkg=$(getFactoryPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})

# Pool Package information
poolPkg=$(getPoolPkg)
setupDeployDir ${poolPkg} ${network}
setupCallsDir ${poolPkg} ${network}
poolDeployDir=$(getDeployDir ${poolPkg} ${network})
poolCallsDir=$(getCallsDir ${poolPkg} ${network})

# Get the Pool address
actionName="getPool"
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

pool=$(./run.py -e ${network} call ${pkg} ${actionName})

# Configure the deploy addresses
echo '{}' | jq \
  --arg scoreAddress $pool \
  '{scoreAddress: $scoreAddress}' \
  > ${poolDeployDir}/deploy.json


# Initialize the Pool (1:1 price)
info "Initialize the Pool (1:1 price)..."
actionName="initialize"
sqrtPriceX96="0x1000000000000000000000000"

filter=$(cat <<EOF
{
  method: "initialize",
  params: {
    sqrtPriceX96: \$sqrtPriceX96
  }
}
EOF
)

echo '{}' | jq \
  --arg sqrtPriceX96 $sqrtPriceX96 \
  "${filter}" > ${poolCallsDir}/${actionName}.json

./run.py -e ${network} invoke ${poolPkg} ${actionName}

success "Pool successfully initialized!"