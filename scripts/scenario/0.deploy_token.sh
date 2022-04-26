#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh

if [ "$#" -ne "3" ] ; then
  error "Usage: $0 <network> <name> <symbol>"
  exit 1
fi

network=$1
name=$2
symbol=$3

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
_decimals=$(hex 18)
_initialSupply=$(hex 1000000)

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

./run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
