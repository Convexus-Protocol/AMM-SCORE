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
fee=$(echo ${poolConfig} | jq -r .pool.fee)

# Package information
pkg=$(getFactoryPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})

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

# Call updatePool
info "Updating the pool..."
actionName="updatePool"

filter=$(cat <<EOF
{
  method: "updatePool",
  params: {
    pool: \$pool
  }
}
EOF
)

jq -n \
  --arg pool $pool \
  "${filter}" > ${callsDir}/${actionName}.json

python run.py -e ${network} invoke ${pkg} ${actionName}

success "Pool successfully updated!"