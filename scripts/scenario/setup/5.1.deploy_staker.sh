#!/bin/bash

set -e


source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh
source ./scripts/util/hex.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Convexus Staker"
info "Setting up ${deployName}..."

# Package information
pkg=$(getStaker)
javaPkg=":Convexus-Periphery:Contracts:Staker"
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
factoryPkg=$(getFactoryPkg)
nonFungiblePositionManagerPkg=$(getNonFungiblePositionManagerPkg)
factory=$(getAddress ${factoryPkg} ${network})
nonFungiblePositionManager=$(getAddress ${nonFungiblePositionManagerPkg} ${network})
maxIncentiveStartLeadTime=$(hex 2592000)
maxIncentiveDuration=$(hex 63072000)
filter=$(cat <<EOF
{
  factory: \$factory,
  nonFungiblePositionManager: \$nonFungiblePositionManager,
  maxIncentiveStartLeadTime: \$maxIncentiveStartLeadTime,
  maxIncentiveDuration: \$maxIncentiveDuration
}
EOF
)

jq -n \
  --arg factory $factory \
  --arg nonFungiblePositionManager $nonFungiblePositionManager \
  --arg maxIncentiveStartLeadTime $maxIncentiveStartLeadTime \
  --arg maxIncentiveDuration $maxIncentiveDuration \
  "${filter}" > ${deployDir}/params.json

python run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
