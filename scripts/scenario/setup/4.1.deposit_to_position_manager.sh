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

# Package information
pkg=$(getNonFungiblePositionManagerPkg)
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})
callsDir=$(getCallsDir ${pkg} ${network})
posmgr=$(getAddress ${pkg} ${network})

# Keystores
operator=$(cat ./config/keystores/${network}/operator.icx | jq .address -r)

# Deposit token0
info "Depositing token0..."
token0Pkg=$(getToken0Pkg ${poolId})
token0CallsDir=$(getCallsDir ${token0Pkg} ${network})
actionName="deposit_token0"

_to=$posmgr
_value=$(hex 10000000000000000000000)
_data=$(python -c "print('0x'+b'{\"method\":\"deposit\"}'.hex())")

filter=$(cat <<EOF
{
  method: "transfer",
  params: {
    _to: \$_to, 
    _value: \$_value, 
    _data: \$_data
  }
}
EOF
)

jq -n \
  --arg _to $_to \
  --arg _value $_value \
  --arg _data $_data \
  "${filter}" > ${token0CallsDir}/${actionName}.json

./run.py -e ${network} invoke ${token0Pkg} ${actionName}

# Get token0 deposited
actionName="deposited"
user=${operator}
token=${token0}
filter=$(cat <<EOF
{
  method: "deposited",
  params: {
    user: \$user, 
    token: \$token
  }
}
EOF
)

jq -n \
  --arg user $user \
  --arg token $token \
  "${filter}" > ${callsDir}/${actionName}.json

deposited0=$(unhex $(./run.py -e ${network} call ${pkg} ${actionName}))
echo "USDC Deposited: ${deposited0}"

# Deposit token1
info "Depositing token1..."
token1Pkg=$(getToken1Pkg ${poolId})
token1CallsDir=$(getCallsDir ${token1Pkg} ${network})
actionName="deposit_token1"

_to=$posmgr
_value=$(hex 10000000000000000000000)
_data=$(python -c "print('0x'+b'{\"method\":\"deposit\"}'.hex())")

filter=$(cat <<EOF
{
  method: "transfer",
  params: {
    _to: \$_to, 
    _value: \$_value, 
    _data: \$_data
  }
}
EOF
)

jq -n \
  --arg _to $_to \
  --arg _value $_value \
  --arg _data $_data \
  "${filter}" > ${token1CallsDir}/${actionName}.json

./run.py -e ${network} invoke ${token1Pkg} ${actionName}

# Get token1 deposited
actionName="deposited"
user=${operator}
token=${token1}
filter=$(cat <<EOF
{
  method: "deposited",
  params: {
    user: \$user, 
    token: \$token
  }
}
EOF
)

jq -n \
  --arg user $user \
  --arg token $token \
  "${filter}" > ${callsDir}/${actionName}.json

deposited1=$(unhex $(./run.py -e ${network} call ${pkg} ${actionName}))
echo "USDC Deposited: ${deposited1}"

# Mint the position
info "Mint the position..."
tickLower="-0xd89b4"
tickUpper="0xd89b4"
amount0Desired=$(hex 10000000000000000000000)
amount1Desired=$(hex 10000000000000000000000)
amount0Min=$(hex "0")
amount1Min=$(hex "0")
recipient=$operator
deadline=$(hex "111111111111")
fee=$(hex ${fee})
actionName="mintPosition"

filter=$(cat <<EOF
{
  method: "mint",
  params: {
    params: {
      token0: \$token0, 
      token1: \$token1, 
      fee: \$fee, 
      tickLower: \$tickLower, 
      tickUpper: \$tickUpper, 
      amount0Desired: \$amount0Desired, 
      amount1Desired: \$amount1Desired, 
      amount0Min: \$amount0Min, 
      amount1Min: \$amount1Min, 
      recipient: \$recipient, 
      deadline: \$deadline
    }
  }
}
EOF
)

jq -n \
  --arg token0 $token0 \
  --arg token1 $token1 \
  --arg fee $fee \
  --arg tickLower $tickLower \
  --arg tickUpper $tickUpper \
  --arg amount0Desired $amount0Desired \
  --arg amount1Desired $amount1Desired \
  --arg amount0Min $amount0Min \
  --arg amount1Min $amount1Min \
  --arg recipient $recipient \
  --arg deadline $deadline \
  "${filter}" > ${callsDir}/${actionName}.json

./run.py -e ${network} invoke ${pkg} ${actionName}

success "Position successfully minted!"