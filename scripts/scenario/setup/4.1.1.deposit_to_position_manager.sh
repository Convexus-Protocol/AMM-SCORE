#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh
source ./scripts/util/env.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "3" ] ; then
  error "Usage: $0 <poolId> <amount0> <amount1>"
  exit 1
fi

poolId=$1
amount0=$2
amount1=$3

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
operator=$(cat ./scripts/config/keystores/${network}/operator.icx | jq .address -r)

symbol () {
  tokenName=$1
  
  if [ ${tokenName} == "token0" ] ; then
    token=${token0}
    tokenPkg=$(getToken0Pkg ${poolId})
  elif [ ${tokenName} == "token1" ] ; then
    token=${token1}
    tokenPkg=$(getToken1Pkg ${poolId})
  fi

  if [ ${token} == "cx0000000000000000000000000000000000000001" ]; then
    echo "ICX"
    return;
  fi

  tokenCallsDir=$(getCallsDir ${tokenPkg} ${network})
  actionName="symbol"

  filter=$(cat <<EOF
  {
    method: "symbol"
  }
EOF
)
  jq -n \
    "${filter}" > ${tokenCallsDir}/${actionName}.json
  
  echo $(./run.py -e ${network} call ${tokenPkg} ${actionName})
}

deposit_token () {
  tokenName=$1
  amount=$2

  if [ ${tokenName} == "token0" ] ; then
    token=${token0}
    tokenPkg=$(getToken0Pkg ${poolId})
  elif [ ${tokenName} == "token1" ] ; then
    token=${token1}
    tokenPkg=$(getToken1Pkg ${poolId})
  fi

  tokenSymbol=$(symbol ${tokenName})
  # Deposit token
  info "Depositing ${amount} ${tokenSymbol}..."
  actionName="deposit_${tokenName}"

  if [ ${token} == "cx0000000000000000000000000000000000000001" ]; then
    value=$(hex ${amount})
    to=$posmgr

    filter=$(cat <<EOF
    {
      method: "depositIcx",
      value: \$value
    }
EOF
)
    jq -n \
      --arg value $value \
      "${filter}" > ${callsDir}/${actionName}.json

    ./run.py -e ${network} invoke ${pkg} ${actionName}
  else
    tokenCallsDir=$(getCallsDir ${tokenPkg} ${network})
    _to=$posmgr
    _value=$(hex ${amount})
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
      "${filter}" > ${tokenCallsDir}/${actionName}.json
    
    ./run.py -e ${network} invoke ${tokenPkg} ${actionName}
  fi

  # Get token deposited
  actionName="deposited"
  user=${operator}
  
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

  deposited=$(unhex $(./run.py -e ${network} call ${pkg} ${actionName}))
  info "${tokenName} (${token}) deposited: ${deposited} ${tokenSymbol}"
}

deposit_token "token0" $amount0
deposit_token "token1" $amount1

# Mint the position
info "Mint the position..."
tickLower="-0xd89b4"
tickUpper="0xd89b4"
amount0Desired=$(hex ${amount0})
amount1Desired=$(hex ${amount1})
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