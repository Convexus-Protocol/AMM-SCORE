#!/bin/bash

set -e

source ./venv/bin/activate

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

# Keystores
operator=$(cat ./config/keystores/${network}/operator.icx | jq .address -r)

# Tokens Packages
token0Pkg=$(getToken0Pkg ${poolId})
token1Pkg=$(getToken1Pkg ${poolId})
token0CallsDir=$(getCallsDir ${token0Pkg} ${network})
token1CallsDir=$(getCallsDir ${token1Pkg} ${network})

# Consts
robi="hxa40ebe7ef1e27203544f90835df03bf3fff42fd8"

# --- Send tokens to Robi ---

# Send token0
info "Sending token0 to Robi..."
actionName="send_to_robi"

_to=${robi}
_value=$(hex 10000000000000000000000)

filter=$(cat <<EOF
{
  method: "transfer",
  params: {
    _to: \$_to, 
    _value: \$_value
  }
}
EOF
)

jq -n \
  --arg _to $_to \
  --arg _value $_value \
  "${filter}" > ${token0CallsDir}/${actionName}.json

./run.py -e ${network} invoke ${token0Pkg} ${actionName}

# Send token1
info "Sending token1 to Robi..."
actionName="send_to_robi"

_to=${robi}
_value=$(hex 10000000000000000000000)

filter=$(cat <<EOF
{
  method: "transfer",
  params: {
    _to: \$_to, 
    _value: \$_value
  }
}
EOF
)

jq -n \
  --arg _to $_to \
  --arg _value $_value \
  "${filter}" > ${token1CallsDir}/${actionName}.json

./run.py -e ${network} invoke ${token1Pkg} ${actionName}