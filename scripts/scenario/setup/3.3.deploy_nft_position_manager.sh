#!/bin/bash

set -e

source ./venv/bin/activate

source ./scripts/util/get_address.sh
source ./scripts/util/dir.sh
source ./scripts/util/console.sh

source ./scripts/convexus/pkg.sh

if [ "$#" -ne "1" ] ; then
  error "Usage: $0 <network>"
  exit 1
fi

network=$1

# Start
deployName="Convexus NFT Position Manager"
info "Setting up ${deployName}..."

# Package information
pkg=$(getNonFungiblePositionManagerPkg)
javaPkg=":Convexus-Periphery:Contracts:NonFungiblePositionManager"
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
descriptorPkg=$(getNonfungibleTokenPositionDescriptorPkg)
_factory=$(getAddress ${factoryPkg} ${network})
_tokenDescriptor=$(getAddress ${descriptorPkg} ${network})

filter=$(cat <<EOF
{
  _factory: \$_factory,
  _tokenDescriptor: \$_tokenDescriptor
}
EOF
)

jq -n \
  --arg _factory $_factory \
  --arg _tokenDescriptor $_tokenDescriptor \
  "${filter}" > ${deployDir}/params.json

./run.py -e ${network} deploy ${pkg}

success "${deployName} contract has been successfully deployed!"
