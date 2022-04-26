#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

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

# Package information
pkg=$(getToken0Pkg ${poolId})
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Configure the deploy addresses
jq -n \
  --arg scoreAddress $token0 \
  '{scoreAddress: $scoreAddress}' \
  > ./${deployDir}/deploy.json