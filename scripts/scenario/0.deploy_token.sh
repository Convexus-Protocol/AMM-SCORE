#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh

if [ "$#" -ne "5" ] ; then
  error "Usage: $0 <network> <name> <symbol> <decimals> <initial supply>"
  exit 1
fi

network=$1
name=$2
symbol=$3
decimals=$4
initialSupply=$5

# Start
deployName="${name} (${symbol}) Token"
info "Deploying ${deployName}..."

# Package information
pkg="tokens/${symbol}-token"
javaPkg=":Convexus-Commons:Tokens:Contracts:irc2"
build="optimized"

# Setup packages
setupJavaDir ${pkg} ${javaPkg} ${build}
setupDeployDir ${pkg} ${network}
setupCallsDir ${pkg} ${network}
deployDir=$(getDeployDir ${pkg} ${network})

# Start
info "Cleaning..."
./gradlew "${javaPkg}:clean" > /dev/null

# Deploy on ICON network
_name=$name
_symbol=$symbol
_decimals=$(hex ${decimals})
_initialSupply=$(hex ${initialSupply})

filter=$(cat <<EOF
{
  _name: \$_name,
  _symbol: \$_symbol,
  _decimals: \$_decimals,
  _initialSupply: \$_initialSupply
}
EOF
)

jq -n \
  --arg _name $_name \
  --arg _symbol $_symbol \
  --arg _decimals $_decimals \
  --arg _initialSupply $_initialSupply \
  "${filter}" > ${deployDir}/params.json

python run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
